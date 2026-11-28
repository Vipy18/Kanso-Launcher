package com.kanso.launcher.service

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
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
import java.util.ArrayDeque
import java.util.Locale

/**
 * Unified Accessibility Bridge for Kanso Launcher:
 * 1. Forces Kanso Multitasking (RecentsActivity) over stock SystemUI / OEM Recents on ALL devices
 *    via window/node disambiguation and KEYCODE_APP_SWITCH interception.
 * 2. Forces Kanso Lockscreen over stock Keyguard when Lockscreen Companion is enabled.
 * 3. Captures throttled Android 11+ Accessibility window snapshots into RecentsSnapshotCache.
 * 4. Debounces Chinese OEM stock Home interception so Recents gestures and system panels settle cleanly.
 * 5. Assists explicit Force Stop confirmation on Android App Info when requested by the user.
 */
class KansoAccessibilityService : AccessibilityService() {

    companion object {
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        @Volatile
        private var instance: KansoAccessibilityService? = null

        @Volatile
        private var lockscreenShownForCurrentLockCycle: Boolean = false

        @Volatile
        private var suppressHomeRedirectUntilMs: Long = 0L

        @Volatile
        private var assistedForceStopPkg: String? = null

        @Volatile
        private var assistedForceStopUntilMs: Long = 0L

        @Volatile
        private var assistedForceStopClickedPrimary: Boolean = false

        private const val HOME_REDIRECT_DEBOUNCE_MS = 160L
        private const val RECENTS_HOME_SUPPRESS_MS = 1800L
        private const val FORCE_STOP_WINDOW_MS = 7000L

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
            "com.bbk.launcher2",
            "com.vivo.launcher"
        )

        private val settingsPackages = setOf(
            "com.android.settings",
            "com.hihonor.settings",
            "com.huawei.settings",
            "com.miui.securitycenter",
            "com.coloros.settings",
            "com.oplus.settings",
            "com.vivo.settings"
        )

        private val recentsClassKeywords = listOf(
            "recents",
            "overview",
            "taskview",
            "taskswitcher",
            "multitask",
            "recent",
            "hwrecents",
            "splitscreen",
            "freeform",
            "gesturenav"
        )

        private val recentsTextMarkers = listOf(
            "recents",
            "recent apps",
            "overview",
            "clear all",
            "close all",
            "最近任务",
            "最近使用",
            "一键清理",
            "全部清除",
            "清除全部",
            "多任务"
        )

        private val recentsViewIdMarkers = listOf(
            "recents",
            "clear_all",
            "clearall",
            "clean_all",
            "task_view",
            "taskview",
            "hwrecents",
            "recent_apps",
            "memory_info",
            "ram_info"
        )

        private val forceStopPrimaryLabels = listOf("force stop", "强行停止", "结束运行")
        private val forceStopConfirmLabels = listOf("force stop", "ok", "强行停止", "确定", "停止")

        fun resetLockCycleLatch() {
            lockscreenShownForCurrentLockCycle = false
        }

        fun notifyRecentsOpened(durationMs: Long = RECENTS_HOME_SUPPRESS_MS) {
            suppressHomeRedirectUntilMs = maxOf(
                suppressHomeRedirectUntilMs,
                System.currentTimeMillis() + durationMs
            )
            instance?.cancelPendingHomeRedirect()
        }

        /**
         * Arms a short-lived assisted Force Stop observer right before launching Android App Info
         * for [targetPackage]. Only runs when explicitly triggered by the user's Force Stop button.
         */
        fun requestAssistedForceStop(targetPackage: String) {
            if (targetPackage.isBlank()) return
            val now = System.currentTimeMillis()
            assistedForceStopPkg = targetPackage
            assistedForceStopUntilMs = now + FORCE_STOP_WINDOW_MS
            assistedForceStopClickedPrimary = false
            suppressHomeRedirectUntilMs = maxOf(suppressHomeRedirectUntilMs, now + FORCE_STOP_WINDOW_MS)
            instance?.cancelPendingHomeRedirect()
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
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var forceRecentsOverride = false

    @Volatile
    private var forceLockscreenOverride = false

    private var lastHomeRedirectMs: Long = 0L
    private var lastRecentsRedirectMs: Long = 0L
    private var lastLockRedirectMs: Long = 0L
    private var lastSnapshotCaptureMs: Long = 0L
    private var currentTrackedAppPkg: String = ""
    private var pendingHomeRedirectRunnable: Runnable? = null

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

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return super.onKeyEvent(event)
        if (forceRecentsOverride &&
            event.keyCode == KeyEvent.KEYCODE_APP_SWITCH &&
            event.action == KeyEvent.ACTION_DOWN
        ) {
            val now = System.currentTimeMillis()
            cancelPendingHomeRedirect()
            suppressHomeRedirectUntilMs = maxOf(suppressHomeRedirectUntilMs, now + RECENTS_HOME_SUPPRESS_MS)
            if (now - lastRecentsRedirectMs > 450L) {
                lastRecentsRedirectMs = now
                if (currentTrackedAppPkg.isNotBlank()) {
                    captureSnapshotIfPermitted(currentTrackedAppPkg, now)
                }
                launchKansoRecents()
            }
            return true
        }
        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return

        val now = System.currentTimeMillis()

        // 0. Assisted Force Stop flow (only when explicitly armed by Recents Force Stop button)
        if (isAssistedForceStopActive(now)) {
            val activePkg = event.packageName?.toString()
                ?: rootInActiveWindow?.packageName?.toString().orEmpty()
            if (activePkg in settingsPackages) {
                cancelPendingHomeRedirect()
                handleAssistedForceStopStep(now)
            }
            return
        }

        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val cls = event.className?.toString().orEmpty()

        // Ignore Kanso's own windows and cancel any pending OEM Home redirect
        if (pkg == packageName) {
            cancelPendingHomeRedirect()
            return
        }

        // 1. Force Kanso Lockscreen over stock Keyguard when toggled on in Settings (all devices)
        if (forceLockscreenOverride && isKeyguardWindow(pkg, cls)) {
            cancelPendingHomeRedirect()
            if (!lockscreenShownForCurrentLockCycle && now - lastLockRedirectMs > 800L) {
                lastLockRedirectMs = now
                lockscreenShownForCurrentLockCycle = true
                launchKansoLockscreen()
            }
            return
        }

        // 2. Force Kanso Multitasking (RecentsActivity) over stock Recents/Overview when toggled on
        val isRecentsWindow = isStockRecentsWindow(pkg, cls, event)
        if (isRecentsWindow) {
            cancelPendingHomeRedirect()
            suppressHomeRedirectUntilMs = maxOf(suppressHomeRedirectUntilMs, now + RECENTS_HOME_SUPPRESS_MS)
            if (forceRecentsOverride && now - lastRecentsRedirectMs > 650L) {
                lastRecentsRedirectMs = now
                if (currentTrackedAppPkg.isNotBlank()) {
                    captureSnapshotIfPermitted(currentTrackedAppPkg, now)
                }
                launchKansoRecents()
            }
            return
        }

        // 3. Debounced OEM stock home interceptor on Chinese OEM ROMs (never while Recents is active/settling)
        if (RomDetector.isChineseRom() && pkg in oemHomePackages) {
            if (RecentsActivity.isForeground || now < suppressHomeRedirectUntilMs) {
                cancelPendingHomeRedirect()
                return
            }
            scheduleDebouncedChineseHomeRedirect()
            return
        }

        // 4. Non-OEM app or SystemUI window appeared — cancel any queued Home redirect
        cancelPendingHomeRedirect()

        // 5. Track active user app & capture throttled Android 11+ window snapshot for Multitasking previews
        if (pkg != "com.android.systemui" && pkg != "android") {
            currentTrackedAppPkg = pkg
            if (forceRecentsOverride) {
                captureSnapshotIfPermitted(pkg, now)
            }
        }
    }

    private fun scheduleDebouncedChineseHomeRedirect() {
        cancelPendingHomeRedirect()
        val runnable = Runnable {
            val now = System.currentTimeMillis()
            if (RecentsActivity.isForeground ||
                now < suppressHomeRedirectUntilMs ||
                isAssistedForceStopActive(now)
            ) {
                return@Runnable
            }
            if (now - lastHomeRedirectMs <= 450L) return@Runnable

            // Re-verify the active foreground window is genuinely the OEM Home desktop
            val activeRoot = runCatching { rootInActiveWindow }.getOrNull()
            val activePkg = activeRoot?.packageName?.toString().orEmpty()
            if (activePkg == packageName || activePkg !in oemHomePackages) {
                return@Runnable
            }
            if (hasRecentsMarkersInWindowTree(activeRoot)) {
                suppressHomeRedirectUntilMs = maxOf(suppressHomeRedirectUntilMs, now + RECENTS_HOME_SUPPRESS_MS)
                if (forceRecentsOverride && now - lastRecentsRedirectMs > 650L) {
                    lastRecentsRedirectMs = now
                    launchKansoRecents()
                }
                return@Runnable
            }

            // Ensure SystemUI shade/keyguard isn't focused above OEM launcher
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            if (km?.isKeyguardLocked == true) return@Runnable

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
        pendingHomeRedirectRunnable = runnable
        mainHandler.postDelayed(runnable, HOME_REDIRECT_DEBOUNCE_MS)
    }

    private fun cancelPendingHomeRedirect() {
        pendingHomeRedirectRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingHomeRedirectRunnable = null
    }

    private fun isStockRecentsWindow(pkg: String, cls: String, event: AccessibilityEvent): Boolean {
        if (pkg !in systemUiRecentsPackages) return false

        val lowerCls = cls.lowercase(Locale.ROOT)
        if (recentsClassKeywords.any { lowerCls.contains(it) }) {
            return true
        }

        // Check event text & contentDescription for Recents / Overview markers
        val eventTexts = buildList {
            event.contentDescription?.toString()?.let { add(it) }
            event.text?.forEach { cs -> cs?.toString()?.let { add(it) } }
        }
        if (eventTexts.any { txt ->
                val lower = txt.lowercase(Locale.ROOT)
                recentsTextMarkers.any { lower.contains(it) }
            }
        ) {
            return true
        }

        // Check interactive window titles
        val interactiveWindows = runCatching { windows }.getOrNull().orEmpty()
        for (win in interactiveWindows) {
            val title = win.title?.toString()?.lowercase(Locale.ROOT).orEmpty()
            if (title.isNotBlank() && recentsTextMarkers.any { title.contains(it) }) {
                return true
            }
        }

        // Inspect active window node tree for OEM recents controls (clear-all button, RAM bar, recents view IDs)
        val root = runCatching { rootInActiveWindow }.getOrNull()
        if (root != null && root.packageName?.toString() == pkg && hasRecentsMarkersInWindowTree(root)) {
            return true
        }

        return false
    }

    private fun hasRecentsMarkersInWindowTree(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        val maxNodes = 48

        while (queue.isNotEmpty() && visited < maxNodes) {
            val node = queue.removeFirst()
            visited++

            val viewId = node.viewIdResourceName?.lowercase(Locale.ROOT).orEmpty()
            if (viewId.isNotBlank() && recentsViewIdMarkers.any { viewId.contains(it) }) {
                return true
            }

            val text = node.text?.toString()?.lowercase(Locale.ROOT).orEmpty()
            val desc = node.contentDescription?.toString()?.lowercase(Locale.ROOT).orEmpty()
            if ((text.isNotBlank() && recentsTextMarkers.any { text.contains(it) }) ||
                (desc.isNotBlank() && recentsTextMarkers.any { desc.contains(it) })
            ) {
                return true
            }

            val childCount = node.childCount.coerceAtMost(12)
            for (i in 0 until childCount) {
                node.getChild(i)?.let { queue.addLast(it) }
            }
        }
        return false
    }

    private fun isAssistedForceStopActive(now: Long): Boolean {
        if (assistedForceStopPkg == null) return false
        if (now > assistedForceStopUntilMs) {
            assistedForceStopPkg = null
            assistedForceStopClickedPrimary = false
            return false
        }
        return true
    }

    private fun handleAssistedForceStopStep(now: Long) {
        val root = runCatching { rootInActiveWindow }.getOrNull() ?: return
        val targets = if (assistedForceStopClickedPrimary) forceStopConfirmLabels else forceStopPrimaryLabels
        val match = findClickableNodeByLabels(root, targets) ?: return

        if (match.isEnabled) {
            val clicked = runCatching { match.performAction(AccessibilityNodeInfo.ACTION_CLICK) }.getOrDefault(false)
            if (clicked) {
                if (!assistedForceStopClickedPrimary) {
                    assistedForceStopClickedPrimary = true
                    assistedForceStopUntilMs = now + 4500L
                } else {
                    // Confirmation clicked — finish assisted cycle cleanly
                    assistedForceStopPkg = null
                    assistedForceStopClickedPrimary = false
                    assistedForceStopUntilMs = 0L
                }
            }
        }
    }

    private fun findClickableNodeByLabels(
        root: AccessibilityNodeInfo,
        labels: List<String>
    ): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        val maxNodes = 80

        while (queue.isNotEmpty() && visited < maxNodes) {
            val node = queue.removeFirst()
            visited++

            val text = node.text?.toString()?.trim()?.lowercase(Locale.ROOT).orEmpty()
            val desc = node.contentDescription?.toString()?.trim()?.lowercase(Locale.ROOT).orEmpty()
            val matched = labels.any { lbl ->
                text == lbl || desc == lbl ||
                    (lbl.length >= 4 && (text.contains(lbl) || desc.contains(lbl)))
            }
            if (matched) {
                val clickable = findClickableAncestorOrSelf(node)
                if (clickable != null && clickable.isEnabled) {
                    return clickable
                }
            }

            val count = node.childCount.coerceAtMost(16)
            for (i in 0 until count) {
                node.getChild(i)?.let { queue.addLast(it) }
            }
        }
        return null
    }

    private fun findClickableAncestorOrSelf(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth < 4) {
            if (current.isClickable) return current
            current = current.parent
            depth++
        }
        return null
    }

    private fun isKeyguardWindow(pkg: String, cls: String): Boolean {
        if (pkg != "com.android.systemui") return false
        val lower = cls.lowercase(Locale.ROOT)
        if (lower.contains("keyguard") || lower.contains("lockscreen") || lower.contains("bouncer")) {
            return true
        }
        val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return km?.isKeyguardLocked == true && lower.contains("statusbar")
    }

    private fun launchKansoRecents() {
        suppressHomeRedirectUntilMs = maxOf(
            suppressHomeRedirectUntilMs,
            System.currentTimeMillis() + RECENTS_HOME_SUPPRESS_MS
        )
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
        cancelPendingHomeRedirect()
        if (instance === this) instance = null
        _isRunning.value = false
        serviceScope.cancel()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cancelPendingHomeRedirect()
        if (instance === this) instance = null
        _isRunning.value = false
        serviceScope.cancel()
        super.onDestroy()
    }
}
