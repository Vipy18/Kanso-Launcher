package com.kanso.launcher.service

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.LruCache
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import androidx.core.graphics.scale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory LRU cache holding downscaled Android 11+ Accessibility window snapshots
 * plus persistent dismissed-task cutoffs and real background process termination
 * for Kanso's Multitasking / Recents carousel.
 */
object RecentsSnapshotCache {

    private const val MAX_SNAPSHOTS = 10
    private const val TARGET_MAX_WIDTH = 360
    private const val PREFS_NAME = "kanso_recents_dismissed_cutoffs"

    private val cache = object : LruCache<String, Bitmap>(MAX_SNAPSHOTS) {}
    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    fun get(packageName: String): Bitmap? = synchronized(cache) {
        val bmp = cache.get(packageName)
        if (bmp != null && !bmp.isRecycled) bmp else null
    }

    fun put(packageName: String, bitmap: Bitmap) {
        if (packageName.isBlank() || bitmap.isRecycled) return
        synchronized(cache) {
            cache.put(packageName, bitmap)
        }
        _revision.value++
    }

    fun remove(packageName: String) {
        synchronized(cache) {
            cache.remove(packageName)
        }
        _revision.value++
    }

    fun clear() {
        synchronized(cache) {
            cache.evictAll()
        }
        _revision.value++
    }

    fun getDismissedCutoffs(context: Context): Map<String, Long> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val result = HashMap<String, Long>()
        for ((key, value) in prefs.all) {
            if (value is Long && value > 0L) {
                result[key] = value
            }
        }
        return result
    }

    fun recordDismissed(context: Context, packageName: String, timestampMs: Long = System.currentTimeMillis()) {
        if (packageName.isBlank()) return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putLong(packageName, timestampMs)
        }
    }

    fun recordDismissedBatch(
        context: Context,
        packages: Collection<String>,
        timestampMs: Long = System.currentTimeMillis()
    ) {
        if (packages.isEmpty()) return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            packages.forEach { pkg ->
                if (pkg.isNotBlank()) putLong(pkg, timestampMs)
            }
        }
    }

    fun clearDismissedCutoff(context: Context, packageName: String) {
        if (packageName.isBlank()) return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            remove(packageName)
        }
    }

    /**
     * Terminates background processes for a dismissed app, evicts its cached window snapshot,
     * and persists a dismissal cutoff timestamp so it does not repopulate from 24h UsageStats
     * until genuinely launched again.
     */
    fun closeRecentTask(
        context: Context,
        packageName: String,
        timestampMs: Long = System.currentTimeMillis()
    ) {
        if (packageName.isBlank()) return
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        runCatching { am?.killBackgroundProcesses(packageName) }
        remove(packageName)
        recordDismissed(context, packageName, timestampMs)
    }

    /**
     * Batch-closes all specified unpinned recent apps: kills background processes,
     * removes snapshots, and records dismissal cutoffs in a single atomic commit.
     */
    fun clearAllRecentTasks(
        context: Context,
        packages: Collection<String>,
        timestampMs: Long = System.currentTimeMillis()
    ) {
        if (packages.isEmpty()) return
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        packages.forEach { pkg ->
            if (pkg.isNotBlank()) {
                runCatching { am?.killBackgroundProcesses(pkg) }
                remove(pkg)
            }
        }
        recordDismissedBatch(context, packages, timestampMs)
    }

    /**
     * Pure filtering predicate: excludes tasks whose lastTimeUsed predates or equals
     * their persisted dismissal cutoff. Reappears only after genuine subsequent usage.
     */
    fun shouldIncludeTask(
        packageName: String,
        lastTimeUsedMs: Long,
        dismissedCutoffs: Map<String, Long>
    ): Boolean {
        if (lastTimeUsedMs <= 0L) return false
        val cutoff = dismissedCutoffs[packageName] ?: 0L
        return lastTimeUsedMs > cutoff
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun storeFromScreenshotResult(packageName: String, result: AccessibilityService.ScreenshotResult) {
        val hwBuffer = result.hardwareBuffer
        try {
            val wrapped = Bitmap.wrapHardwareBuffer(hwBuffer, result.colorSpace) ?: return
            try {
                val sw = wrapped.copy(Bitmap.Config.RGB_565, false) ?: return
                val scaled = if (sw.width > TARGET_MAX_WIDTH) {
                    val ratio = TARGET_MAX_WIDTH.toFloat() / sw.width.toFloat()
                    val h = (sw.height * ratio).toInt().coerceAtLeast(1)
                    val out = sw.scale(TARGET_MAX_WIDTH, h)
                    if (out !== sw) sw.recycle()
                    out
                } else {
                    sw
                }
                put(packageName, scaled)
            } finally {
                wrapped.recycle()
            }
        } catch (_: Throwable) {
            // Silently ignore OEM HardwareBuffer restrictions; Recents UI uses styled fallback
        } finally {
            runCatching { hwBuffer.close() }
        }
    }
}
