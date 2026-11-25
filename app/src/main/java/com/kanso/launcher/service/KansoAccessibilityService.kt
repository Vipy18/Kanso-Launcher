package com.kanso.launcher.service

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import com.kanso.launcher.KansoApplication
import com.kanso.launcher.LauncherActivity
import com.kanso.launcher.ui.lockscreen.LockscreenActivity
import com.kanso.launcher.ui.recents.RecentsActivity
import com.kanso.launcher.util.RomDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Unified Accessibility Bridge for Kanso Launcher:
 * 1. Forces Kanso Multitasking (RecentsActivity) over stock SystemUI / OEM Recents on ALL devices
 *    whenever enabled in Settings.
 * 2. Forces Kanso Lockscreen over stock Keyguard when Lockscreen Companion is enabled.
 * 3. Captures throttled Android 11+ Accessibility window snapshots into RecentsSnapshotCache
 *    for live Multitasking previews.
 * 4. Intercepts hard-locked Chinese OEM stock launchers back to Kanso Home.
 * 5. Executes hardware Global Actions (Lock Screen, System Recents, Screenshot).
 */
class KansoAccessibilityService : AccessibilityService() {

    companion object {
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        @Volatile
        private var instance: KansoAccessibilityService? = null

        @Volatile
        private var lockscreenShownForCurrentLockCycle: Boolean = false

        private val oemHomePackages = setOf(
            "com.hihonor.android.launcher",
            "com.huawei.android.launcher",
            "com.miui.home",
            "com.oppo.launcher",
            "com.bbk.launcher2",
            "com.vivo.launcher",
            "com.oneplus.launcher"
        )

        private val systemUiRecentsPackages = setOf(
            "com.android.systemui",
            "com.google.android.apps.nexuslauncher",
            "com.android.launcher3",
            "com.sec.android.app.launcher",
            "com.miui.home",
            "com.hihonor.android.launcher",
            "com.huawei.android.launcher",
            "com.oppo.launcher",
            "com.oneplus.launcher",
            "com.bbk.launcher2"
        )

        fun resetLockCycleLatch() {
            lockscreenShownForCurrentLockCycle = false
        }

        fun performLockScreen(): Boolean {
            val svc = instance ?: return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                runCatching { svc.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) }.getOrDefault(false)
            } else {
                false
            }
        }

        fun performSystemRecents(): Boolean {
            val svc = instance ?: return false
            return runCatching { svc.performGlobalAction(GLOBAL_ACTION_RECENTS) }.getOrDefault(false)
        }

        fun performTakeScreenshot(): Boolean {
            val svc = instance ?: return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                runCatching { svc.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT) }.getOrDefault(false)
            } else {
                false
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var forceRecentsOverride = false
    @Volatile
    private var forceLockscreenOverride = false

    private var lastHomeRedirectMs: Long = 0L
    private var lastRecentsRedirectMs: Long = 0L
    private var lastLockRedirectMs: Long = 0L
    private var lastSnapshotCaptureMs: Long = 0L
    private var currentTrackedAppPkg: String = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isRunning.value = true

        val app = application as? KansoApplication
        if (app != null) {
            serviceScope.launch {
                app.preferences.settingsFlow.collectLatest { settings ->
                    forceRecentsOverride = settings.forceRecentsOverrideEnabled
                    forceLockscreenOverride = settings.lockscreenCompanionEnabled
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val cls = event.className?.toString().orEmpty()
        val now = System.currentTimeMillis()

        // Ignore Kanso's own windows
        if (pkg == packageName) return

        // 1. Force Kanso Lockscreen over stock Keyguard when toggled on in Settings (all devices)
        if (forceLockscreenOverride && isKeyguardWindow(pkg, cls)) {
            if (!lockscreenShownForCurrentLockCycle && now - lastLockRedirectMs > 800L) {
                lastLockRedirectMs = now
                lockscreenShownForCurrentLockCycle = true
                launchKansoLockscreen()
            }
            return
        }

        // 2. Force Kanso Multitasking (RecentsActivity) over stock Recents/Overview when toggled on (all devices)
        if (forceRecentsOverride && isStockRecentsWindow(pkg, cls)) {
            if (now - lastRecentsRedirectMs > 650L) {
                lastRecentsRedirectMs = now
                // Snapshot last active foreground app before overlaying Kanso Recents
                if (currentTrackedAppPkg.isNotBlank()) {
                    captureSnapshotIfPermitted(currentTrackedAppPkg, now)
                }
                launchKansoRecents()
            }
            return
        }

        // 3. Intercept OEM stock home packages on Chinese OEM ROMs where default launcher is hard-locked
        if (RomDetector.isChineseRom() && pkg in oemHomePackages && !isStockRecentsWindow(pkg, cls)) {
            if (now - lastHomeRedirectMs > 450L) {
                lastHomeRedirectMs = now
                val homeIntent = Intent(this, LauncherActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    )
                }
                runCatching { startActivity(homeIntent) }
            }
            return
        }

        // 4. Track active user app & capture throttled Android 11+ window snapshot for Multitasking previews
        if (pkg != "com.android.systemui" && pkg != "android") {
            currentTrackedAppPkg = pkg
            if (forceRecentsOverride) {
                captureSnapshotIfPermitted(pkg, now)
            }
        }
    }

    private fun isStockRecentsWindow(pkg: String, cls: String): Boolean {
        if (pkg !in systemUiRecentsPackages) return false
        val lower = cls.lowercase()
        return lower.contains("recents") ||
            lower.contains("overview") ||
            lower.contains("taskview") ||
            lower.contains("taskswitcher") ||
            lower.contains("multitask")
    }

    private fun isKeyguardWindow(pkg: String, cls: String): Boolean {
        if (pkg != "com.android.systemui") return false
        val lower = cls.lowercase()
        if (lower.contains("keyguard") || lower.contains("lockscreen") || lower.contains("bouncer")) {
            return true
        }
        val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return km?.isKeyguardLocked == true && lower.contains("statusbar")
    }

    private fun launchKansoRecents() {
        val intent = Intent(this, RecentsActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
        }
        runCatching { startActivity(intent) }
    }

    private fun launchKansoLockscreen() {
        val intent = Intent(this, LockscreenActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
        }
        runCatching { startActivity(intent) }
    }

    private fun captureSnapshotIfPermitted(targetPkg: String, now: Long) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        if (now - lastSnapshotCaptureMs < 2400L) return
        if (KansoPerformanceGovernor.state.value.isAnimationBurstActive) return

        lastSnapshotCaptureMs = now
        runCatching {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshot: ScreenshotResult) {
                        RecentsSnapshotCache.storeFromScreenshotResult(targetPkg, screenshot)
                    }

                    override fun onFailure(errorCode: Int) = Unit
                }
            )
        }
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        if (instance === this) instance = null
        _isRunning.value = false
        serviceScope.cancel()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        _isRunning.value = false
        serviceScope.cancel()
        super.onDestroy()
    }
}
