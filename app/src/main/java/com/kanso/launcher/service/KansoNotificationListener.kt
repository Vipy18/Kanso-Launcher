package com.kanso.launcher.service

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.KeyEvent
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

data class MediaHudState(
    val title: String = "Media HUD",
    val artist: String = "Tap to play",
    val sourceAppLabel: String = "",
    val packageName: String = "",
    val albumArt: Bitmap? = null,
    val isPlaying: Boolean = false,
    val hasActiveSession: Boolean = false
)

data class KansoNotificationItem(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val postTimeMs: Long,
    val isClearable: Boolean,
    val contentIntent: PendingIntent?
)

class KansoNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var mediaSessionManager: MediaSessionManager? = null
    private val controllerLock = Any()
    private val trackedControllers = mutableListOf<MediaController>()
    private val controllerCallbacks = mutableMapOf<MediaController, MediaController.Callback>()
    private var lastArtUri: String? = null
    private var lastArtBitmap: Bitmap? = null

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            bindAllControllers(controllers.orEmpty())
        }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instanceRef = this
        _isConnected.value = true
        refreshNotifications()
        attachMediaSessionListener()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instanceRef === this) instanceRef = null
        _isConnected.value = false
        detachMediaSessionListener()
    }

    override fun onDestroy() {
        if (instanceRef === this) instanceRef = null
        detachMediaSessionListener()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshNotifications()
        refreshMediaSessionsSnapshot()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshNotifications()
    }

    private fun refreshNotifications() {
        val counts = linkedMapOf<String, Int>()
        val items = mutableListOf<KansoNotificationItem>()
        val active = runCatching { activeNotifications }.getOrNull().orEmpty()
            .sortedByDescending { it.postTime }

        for (sbn in active) {
            val pkg = sbn.packageName ?: continue
            if (pkg == packageName) continue
            val notif = sbn.notification ?: continue
            val isGroupSummary = (notif.flags and Notification.FLAG_GROUP_SUMMARY) != 0
            counts[pkg] = (counts[pkg] ?: 0) + 1
            if (isGroupSummary) continue

            val extras = notif.extras
            val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            val text = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                ?: extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
            if (title.isBlank() && text.isBlank()) continue

            val appName = resolveAppLabel(pkg)
            items.add(
                KansoNotificationItem(
                    key = sbn.key,
                    packageName = pkg,
                    appName = appName,
                    title = title.ifBlank { appName },
                    text = text,
                    postTimeMs = sbn.postTime,
                    isClearable = sbn.isClearable,
                    contentIntent = notif.contentIntent
                )
            )
        }
        _badgeCounts.value = counts
        _activeNotificationItems.value = items
    }

    private fun resolveAppLabel(pkg: String): String {
        return runCatching {
            val ai = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(ai).toString()
        }.getOrDefault(pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() })
    }

    private fun attachMediaSessionListener() {
        val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return
        mediaSessionManager = msm
        val component = ComponentName(this, KansoNotificationListener::class.java)
        runCatching {
            msm.addOnActiveSessionsChangedListener(sessionsChangedListener, component)
            bindAllControllers(msm.getActiveSessions(component))
        }
    }

    private fun refreshMediaSessionsSnapshot() {
        val msm = mediaSessionManager ?: return
        val component = ComponentName(this, KansoNotificationListener::class.java)
        runCatching {
            val active = msm.getActiveSessions(component)
            val trackedSize = synchronized(controllerLock) { trackedControllers.size }
            if (active.size != trackedSize) {
                bindAllControllers(active)
            } else {
                publishBestController()
            }
        }
    }

    private fun detachMediaSessionListener() {
        clearTrackedControllers()
        runCatching { mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener) }
        currentControllerRef = null
    }

    private fun clearTrackedControllers() {
        val snapshot = synchronized(controllerLock) {
            val entries = controllerCallbacks.entries.map { it.key to it.value }
            controllerCallbacks.clear()
            trackedControllers.clear()
            entries
        }
        snapshot.forEach { (controller, cb) ->
            runCatching { controller.unregisterCallback(cb) }
        }
    }

    /**
     * Subscribes to EVERY active media session (Spotify, YouTube, YT Music, Apple Music,
     * Prime Music, SoundCloud, etc.) so switching playback between apps immediately updates the HUD.
     */
    private fun bindAllControllers(controllers: List<MediaController>) {
        clearTrackedControllers()
        val pairs = mutableListOf<Pair<MediaController, MediaController.Callback>>()
        synchronized(controllerLock) {
            trackedControllers.addAll(controllers)
            controllers.forEach { controller ->
                val cb = object : MediaController.Callback() {
                    override fun onPlaybackStateChanged(state: PlaybackState?) {
                        publishBestController()
                    }

                    override fun onMetadataChanged(metadata: MediaMetadata?) {
                        publishBestController()
                    }

                    override fun onSessionDestroyed() {
                        synchronized(controllerLock) {
                            trackedControllers.remove(controller)
                            controllerCallbacks.remove(controller)
                        }
                        publishBestController()
                    }
                }
                controllerCallbacks[controller] = cb
                pairs.add(controller to cb)
            }
        }
        pairs.forEach { (controller, cb) ->
            runCatching { controller.registerCallback(cb) }
        }
        publishBestController()
    }

    private fun publishBestController() {
        val snapshot = synchronized(controllerLock) { trackedControllers.toList() }
        val chosen = snapshot.firstOrNull { isActivelyPlaying(it) }
            ?: snapshot.firstOrNull { it.metadata != null }
            ?: snapshot.firstOrNull()

        currentControllerRef = chosen
        publishMediaFrom(chosen)
    }

    private fun isActivelyPlaying(controller: MediaController): Boolean {
        val state = controller.playbackState?.state ?: return false
        return state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
    }

    private fun publishMediaFrom(controller: MediaController?) {
        if (controller == null) {
            _mediaState.value = MediaHudState()
            return
        }
        val meta = controller.metadata
        val pkg = controller.packageName.orEmpty()
        val appLabel = if (pkg.isNotBlank()) resolveAppLabel(pkg) else ""
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)?.takeIf { it.isNotBlank() }
            ?: appLabel.ifBlank { "Media HUD" }
        val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }
            ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)?.takeIf { it.isNotBlank() }
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)?.takeIf { it.isNotBlank() }
            ?: appLabel.ifBlank { "Tap to play" }

        val directArt = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

        val artUri = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_ART_URI)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)

        val immediateArt = directArt ?: if (artUri != null && artUri == lastArtUri) lastArtBitmap else null
        val playing = isActivelyPlaying(controller)

        _mediaState.value = MediaHudState(
            title = title,
            artist = artist,
            sourceAppLabel = appLabel,
            packageName = pkg,
            albumArt = immediateArt,
            isPlaying = playing,
            hasActiveSession = true
        )

        if (directArt == null && !artUri.isNullOrBlank() && artUri != lastArtUri) {
            serviceScope.launch {
                val loaded = loadBitmapFromUri(artUri)
                if (loaded != null) {
                    lastArtUri = artUri
                    lastArtBitmap = loaded
                    if (_mediaState.value.packageName == pkg && _mediaState.value.title == title) {
                        _mediaState.value = _mediaState.value.copy(albumArt = loaded)
                    }
                }
            }
        }
    }

    private fun loadBitmapFromUri(uriStr: String): Bitmap? = runCatching {
        if (uriStr.startsWith("http://") || uriStr.startsWith("https://")) {
            val conn = (URL(uriStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
            }
            try {
                if (conn.responseCode in 200..299) {
                    conn.inputStream.use { decodeBoundedBitmap(it) }
                } else null
            } finally {
                conn.disconnect()
            }
        } else {
            val uri = uriStr.toUri()
            contentResolver.openInputStream(uri)?.use { decodeBoundedBitmap(it) }
        }
    }.getOrNull()

    private fun decodeBoundedBitmap(input: InputStream, targetPx: Int = 320, maxBytes: Int = 2 * 1024 * 1024): Bitmap? {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var total = 0
        while (true) {
            val n = input.read(buf)
            if (n <= 0) break
            total += n
            if (total > maxBytes) return null
            out.write(buf, 0, n)
        }
        val bytes = out.toByteArray()
        if (bytes.isEmpty()) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (bounds.outWidth / (sampleSize * 2) >= targetPx && bounds.outHeight / (sampleSize * 2) >= targetPx) {
            sampleSize *= 2
        }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    companion object {
        @Volatile
        private var instanceRef: KansoNotificationListener? = null

        @Volatile
        private var currentControllerRef: MediaController? = null

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        private val _mediaState = MutableStateFlow(MediaHudState())
        val mediaState: StateFlow<MediaHudState> = _mediaState.asStateFlow()

        private val _badgeCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
        val badgeCounts: StateFlow<Map<String, Int>> = _badgeCounts.asStateFlow()

        private val _activeNotificationItems = MutableStateFlow<List<KansoNotificationItem>>(emptyList())
        val activeNotificationItems: StateFlow<List<KansoNotificationItem>> = _activeNotificationItems.asStateFlow()

        fun cancelNotificationByKey(key: String) {
            runCatching { instanceRef?.cancelNotification(key) }
        }

        fun clearAllNotifications() {
            runCatching { instanceRef?.cancelAllNotifications() }
        }

        fun togglePlayPause(context: Context) {
            val ctrl = currentControllerRef
            if (ctrl != null) {
                val state = ctrl.playbackState?.state
                val playing = state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
                if (playing) ctrl.transportControls.pause() else ctrl.transportControls.play()
            } else {
                dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            }
        }

        fun skipNext(context: Context) {
            val ctrl = currentControllerRef
            if (ctrl != null) {
                ctrl.transportControls.skipToNext()
            } else {
                dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_NEXT)
            }
        }

        fun skipPrevious(context: Context) {
            val ctrl = currentControllerRef
            if (ctrl != null) {
                ctrl.transportControls.skipToPrevious()
            } else {
                dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            }
        }

        private fun dispatchMediaKey(context: Context, keyCode: Int) {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            runCatching {
                am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            }
        }
    }
}
