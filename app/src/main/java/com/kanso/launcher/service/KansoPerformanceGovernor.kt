package com.kanso.launcher.service

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.Choreographer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicBoolean

enum class PerformanceTier(val label: String, val badge: String) {
    TURBO_60("Turbo 60FPS", "60Hz MAX"),
    BALANCED_60("Balanced 60FPS", "60Hz ECO"),
    ECO_THERMAL("Thermal Saver", "CPU CAP")
}

data class GovernorState(
    val enabled: Boolean = true,
    val fpsEstimate: Int = 60,
    val tier: PerformanceTier = PerformanceTier.TURBO_60,
    val isAnimationBurstActive: Boolean = false,
    val telemetryPollIntervalMs: Long = 1500L,
    val visualizerDelayMs: Long = 55L,
    val highFidelityEffectsEnabled: Boolean = true,
    val cpuBudgetPercent: Int = 100
)

/**
 * Intelligent 60FPS Frame-Budget CPU Governor:
 * - Monitors real Choreographer frame intervals against the 16.67ms (60FPS) target.
 * - Tracks Android thermal status and battery saver state.
 * - Automatically stretches non-critical background polling & audio FFT cadence during active
 *   UI animations, drags, and pager flings so CPU headroom stays reserved for the render thread
 *   while preserving crisp mecha-industrial vector fidelity.
 */
object KansoPerformanceGovernor {

    private const val TARGET_FRAME_MS = 16.67f
    private const val WINDOW_SIZE = 30

    private val _state = MutableStateFlow(GovernorState())
    val state: StateFlow<GovernorState> = _state.asStateFlow()

    private val running = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var powerManager: PowerManager? = null

    private var lastFrameNanos: Long = 0L
    private val frameDeltasMs = FloatArray(WINDOW_SIZE) { TARGET_FRAME_MS }
    private var frameCursor = 0
    private var sampleCount = 0
    private var slowFrameStreak = 0

    private val animationReleaseRunnable = Runnable {
        _state.update { current ->
            recomputeBudgets(current.copy(isAnimationBurstActive = false))
        }
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running.get()) return
            if (lastFrameNanos > 0L) {
                val deltaMs = ((frameTimeNanos - lastFrameNanos) / 1_000_000f).coerceIn(4f, 120f)
                frameDeltasMs[frameCursor] = deltaMs
                frameCursor = (frameCursor + 1) % WINDOW_SIZE
                sampleCount++

                if (deltaMs > 19.5f) {
                    slowFrameStreak = (slowFrameStreak + 1).coerceAtMost(20)
                } else if (slowFrameStreak > 0) {
                    slowFrameStreak--
                }

                if (sampleCount >= 15) {
                    sampleCount = 0
                    evaluateWindow()
                }
            }
            lastFrameNanos = frameTimeNanos
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun start(context: Context) {
        powerManager = context.applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (!_state.value.enabled) return
        if (running.compareAndSet(false, true)) {
            lastFrameNanos = 0L
            mainHandler.post {
                if (running.get()) {
                    Choreographer.getInstance().postFrameCallback(frameCallback)
                }
            }
        }
    }

    fun stop() {
        running.set(false)
        mainHandler.removeCallbacks(animationReleaseRunnable)
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runCatching { Choreographer.getInstance().removeFrameCallback(frameCallback) }
        } else {
            mainHandler.post {
                runCatching { Choreographer.getInstance().removeFrameCallback(frameCallback) }
            }
        }
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        _state.update {
            if (enabled) recomputeBudgets(it.copy(enabled = true))
            else GovernorState(enabled = false, fpsEstimate = 60, tier = PerformanceTier.TURBO_60)
        }
        if (enabled) start(context) else stop()
    }

    /**
     * Called during active pager swipes, grid drags, and shade transitions to cap background
     * telemetry/FFT CPU usage for a short burst window while keeping UI animations locked at 60FPS.
     */
    fun notifyAnimationBurst(durationMs: Long = 650L) {
        if (!_state.value.enabled) return
        mainHandler.removeCallbacks(animationReleaseRunnable)
        if (!_state.value.isAnimationBurstActive) {
            _state.update { current ->
                recomputeBudgets(current.copy(isAnimationBurstActive = true))
            }
        }
        mainHandler.postDelayed(animationReleaseRunnable, durationMs.coerceIn(200L, 1800L))
    }

    private fun evaluateWindow() {
        var sum = 0f
        for (v in frameDeltasMs) sum += v
        val avgMs = (sum / WINDOW_SIZE).coerceAtLeast(8f)
        val fps = (1000f / avgMs).toInt().coerceIn(24, 120)

        val pm = powerManager
        val isPowerSave = runCatching { pm?.isPowerSaveMode == true }.getOrDefault(false)
        val thermalHot = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && pm != null) {
            runCatching { pm.currentThermalStatus >= PowerManager.THERMAL_STATUS_MODERATE }.getOrDefault(false)
        } else {
            false
        }

        val tier = when {
            thermalHot || slowFrameStreak >= 6 -> PerformanceTier.ECO_THERMAL
            isPowerSave || avgMs > 18.2f -> PerformanceTier.BALANCED_60
            else -> PerformanceTier.TURBO_60
        }

        _state.update { current ->
            recomputeBudgets(current.copy(fpsEstimate = fps, tier = tier))
        }
    }

    private fun recomputeBudgets(base: GovernorState): GovernorState {
        if (!base.enabled) return base
        return when {
            base.isAnimationBurstActive -> base.copy(
                telemetryPollIntervalMs = 4500L,
                visualizerDelayMs = 90L,
                highFidelityEffectsEnabled = base.tier != PerformanceTier.ECO_THERMAL,
                cpuBudgetPercent = 62
            )
            base.tier == PerformanceTier.ECO_THERMAL -> base.copy(
                telemetryPollIntervalMs = 4000L,
                visualizerDelayMs = 110L,
                highFidelityEffectsEnabled = false,
                cpuBudgetPercent = 55
            )
            base.tier == PerformanceTier.BALANCED_60 -> base.copy(
                telemetryPollIntervalMs = 2500L,
                visualizerDelayMs = 75L,
                highFidelityEffectsEnabled = true,
                cpuBudgetPercent = 78
            )
            else -> base.copy(
                telemetryPollIntervalMs = 1500L,
                visualizerDelayMs = 55L,
                highFidelityEffectsEnabled = true,
                cpuBudgetPercent = 100
            )
        }
    }
}
