package com.kanso.launcher.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.os.Build
import android.util.LruCache
import androidx.annotation.RequiresApi
import androidx.core.graphics.scale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lightweight in-memory LRU cache holding downscaled Android 11+ Accessibility window snapshots
 * keyed by package name for Kanso's launcher-styled Multitasking / Recents carousel.
 */
object RecentsSnapshotCache {

    private const val MAX_SNAPSHOTS = 10
    private const val TARGET_MAX_WIDTH = 360

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
