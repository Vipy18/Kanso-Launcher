package com.kanso.launcher.data.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.audiofx.Visualizer
import androidx.core.content.ContextCompat
import com.kanso.launcher.service.KansoNotificationListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Audio & Music Sync Visualizer Engine:
 * - Captures real-time 13-band FFT via Android Visualizer(0) whenever RECORD_AUDIO is granted
 *   and system audio / media is active.
 * - Automatically falls back to a zero-permission, multi-harmonic reactive wave synced directly
 *   to active media playback (Spotify, YouTube, YT Music, Apple Music, Prime Music, etc.).
 */
class AudioVisualizerEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        const val BAND_COUNT = 13
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _bands = MutableStateFlow(List(BAND_COUNT) { 0.14f })
    val bands: StateFlow<List<Float>> = _bands.asStateFlow()

    private val _isUsingLiveFft = MutableStateFlow(false)
    val isUsingLiveFft: StateFlow<Boolean> = _isUsingLiveFft.asStateFlow()

    private val visualizerLock = Any()
    private var visualizer: Visualizer? = null
    private var lastFftUpdateMs: Long = 0L
    private var pulseJob: Job? = null

    init {
        startObserving()
    }

    private fun startObserving() {
        scope.launch(Dispatchers.Default) {
            KansoNotificationListener.mediaState.collectLatest { media ->
                val active = media.isPlaying || audioManager.isMusicActive
                if (active) {
                    tryAttachLiveFft()
                    startPulseLoop(seed = media.title.hashCode())
                } else {
                    releaseVisualizer()
                    stopPulseLoop()
                    _bands.value = List(BAND_COUNT) { idx ->
                        val taper = 1f - (idx.toFloat() / (BAND_COUNT * 1.4f))
                        (0.12f * taper).coerceIn(0.06f, 0.2f)
                    }
                }
            }
        }
    }

    fun refreshFftAttachment() {
        val active = KansoNotificationListener.mediaState.value.isPlaying || audioManager.isMusicActive
        if (active) {
            tryAttachLiveFft()
        }
    }

    private fun tryAttachLiveFft() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            _isUsingLiveFft.value = false
            return
        }
        synchronized(visualizerLock) {
            if (visualizer != null) return

            val attached = runCatching {
                val viz = Visualizer(0).apply {
                    captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(512)
                    setDataCaptureListener(
                        object : Visualizer.OnDataCaptureListener {
                            override fun onWaveFormDataCapture(
                                visualizer: Visualizer?,
                                waveform: ByteArray?,
                                samplingRate: Int
                            ) = Unit

                            override fun onFftDataCapture(
                                visualizer: Visualizer?,
                                fft: ByteArray?,
                                samplingRate: Int
                            ) {
                                if (fft == null || fft.size < 4) return
                                val computed = computeBandsFromFft(fft)
                                if (computed.any { it > 0.08f }) {
                                    lastFftUpdateMs = System.currentTimeMillis()
                                    _isUsingLiveFft.value = true
                                    _bands.value = computed
                                }
                            }
                        },
                        Visualizer.getMaxCaptureRate() / 2,
                        false,
                        true
                    )
                    enabled = true
                }
                visualizer = viz
                true
            }.getOrDefault(false)

            if (!attached) {
                releaseVisualizerLocked()
            }
        }
    }

    private fun computeBandsFromFft(fft: ByteArray): List<Float> {
        val binCount = (fft.size / 2).coerceAtLeast(BAND_COUNT)
        val prev = _bands.value
        return List(BAND_COUNT) { band ->
            val startBin = ((band * binCount) / BAND_COUNT).coerceIn(1, binCount - 1)
            val endBin = (((band + 1) * binCount) / BAND_COUNT).coerceIn(startBin + 1, binCount)
            var sum = 0f
            var count = 0
            for (k in startBin until endBin) {
                val re = fft.getOrElse(k * 2) { 0 }.toFloat()
                val im = fft.getOrElse(k * 2 + 1) { 0 }.toFloat()
                val mag = hypot(re, im) / 128f
                sum += mag
                count++
            }
            val raw = if (count > 0) (sum / count) * 2.2f else 0f
            val smoothed = (prev.getOrElse(band) { 0.1f } * 0.45f + raw * 0.55f)
            smoothed.coerceIn(0.10f, 1.0f)
        }
    }

    private fun startPulseLoop(seed: Int) {
        if (pulseJob?.isActive == true) return
        pulseJob = scope.launch(Dispatchers.Default) {
            var phase = (abs(seed) % 360) * 0.017f
            while (isActive) {
                val now = System.currentTimeMillis()
                val fftFresh = _isUsingLiveFft.value && (now - lastFftUpdateMs) < 350L
                val stillPlaying = KansoNotificationListener.mediaState.value.isPlaying || audioManager.isMusicActive

                if (!stillPlaying) {
                    _bands.value = List(BAND_COUNT) { idx ->
                        (0.14f - idx * 0.006f).coerceIn(0.06f, 0.16f)
                    }
                    delay(180L)
                    continue
                }

                if (!fftFresh) {
                    _isUsingLiveFft.value = false
                    phase += 0.24f
                    val nextBands = List(BAND_COUNT) { idx ->
                        val normIdx = idx.toFloat() / (BAND_COUNT - 1).toFloat()
                        val w1 = sin(phase * 1.4f + normIdx * PI.toFloat() * 2.1f)
                        val w2 = sin(phase * 2.7f - normIdx * PI.toFloat() * 3.4f)
                        val w3 = sin(phase * 0.9f + idx * 0.75f)
                        val energy = ((w1 * 0.45f + w2 * 0.35f + w3 * 0.20f) + 1f) * 0.5f
                        val envelope = 0.72f + 0.28f * sin(normIdx * PI.toFloat())
                        (0.18f + energy * envelope * 0.82f).coerceIn(0.14f, 1.0f)
                    }
                    _bands.value = nextBands
                }
                delay(com.kanso.launcher.service.KansoPerformanceGovernor.state.value.visualizerDelayMs.coerceIn(45L, 160L))
            }
        }
    }

    private fun stopPulseLoop() {
        pulseJob?.cancel()
        pulseJob = null
    }

    fun releaseVisualizer() {
        synchronized(visualizerLock) {
            releaseVisualizerLocked()
        }
    }

    private fun releaseVisualizerLocked() {
        runCatching {
            visualizer?.enabled = false
            visualizer?.release()
        }
        visualizer = null
        _isUsingLiveFft.value = false
    }

    fun shutdown() {
        stopPulseLoop()
        releaseVisualizer()
    }
}
