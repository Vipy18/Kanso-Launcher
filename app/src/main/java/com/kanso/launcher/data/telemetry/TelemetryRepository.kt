package com.kanso.launcher.data.telemetry

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.location.LocationManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.StatFs
import android.provider.Settings
import android.view.Display
import android.view.Surface
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.kanso.launcher.data.model.KansoActionId
import com.kanso.launcher.data.model.TelemetryMetric
import com.kanso.launcher.service.KansoNotificationListener
import com.kanso.launcher.service.KansoPerformanceGovernor
import com.kanso.launcher.util.RomDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ClockTelemetry(
    val hours24: String = "21",
    val hours12: String = "09",
    val minutes: String = "03",
    val formattedDate: String = "Today Monday, Aug 18"
)

data class CalendarDaySlot(
    val dayOfWeekShort: String,
    val dayOfMonth: Int,
    val isToday: Boolean
)

data class MetricReadout(
    val primaryValue: String,
    val unitLabel: String,
    val gaugeFraction: Float
)

data class TelemetryState(
    val clock: ClockTelemetry = ClockTelemetry(),
    val batteryPercent: Int = 29,
    val batteryTempC: Float = 31.0f,
    val isCharging: Boolean = false,
    val networkSpeedValue: String = "0.00",
    val networkSpeedUnit: String = "KB/s",
    val networkGaugeFraction: Float = 0.24f,
    val freeRamText: String = "2.4GB",
    val ramUsedGb: String = "3.6",
    val ramTotalGb: String = "8.0",
    val ramUsageFraction: Float = 0.56f,
    val storageUsedGb: String = "48",
    val storageTotalGb: String = "128GB",
    val storageUsageFraction: Float = 0.38f,
    val systemLoadPercent: Int = 22,
    val ringerMode: Int = AudioManager.RINGER_MODE_NORMAL,
    val isWifiConnected: Boolean = false,
    val isMobileConnected: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val isHotspotEnabled: Boolean = false,
    val isTorchEnabled: Boolean = false,
    val isAutoRotateEnabled: Boolean = true,
    val isAirplaneModeEnabled: Boolean = false,
    val isPowerSaveEnabled: Boolean = false,
    val isDndEnabled: Boolean = false,
    val isLocationEnabled: Boolean = false,
    val isNfcEnabled: Boolean = false,
    val mediaVolumeFraction: Float = 0.5f,
    val brightnessFraction: Float = 0.65f,
    val audioOutputLabel: String = "Device Speaker",
    val weekDays: List<CalendarDaySlot> = emptyList()
) {
    fun readoutFor(metric: TelemetryMetric): MetricReadout = when (metric) {
        TelemetryMetric.NET_SPEED -> MetricReadout(
            primaryValue = networkSpeedValue,
            unitLabel = networkSpeedUnit,
            gaugeFraction = networkGaugeFraction
        )
        TelemetryMetric.RAM_USAGE -> MetricReadout(
            primaryValue = "${(ramUsageFraction * 100f).toInt()}%",
            unitLabel = "${ramUsedGb}G RAM",
            gaugeFraction = ramUsageFraction.coerceIn(0.08f, 1f)
        )
        TelemetryMetric.STORAGE_USAGE -> MetricReadout(
            primaryValue = "${(storageUsageFraction * 100f).toInt()}%",
            unitLabel = "${storageUsedGb}G SSD",
            gaugeFraction = storageUsageFraction.coerceIn(0.08f, 1f)
        )
        TelemetryMetric.BATTERY_TEMP -> MetricReadout(
            primaryValue = "$batteryPercent%",
            unitLabel = String.format(Locale.US, "%.0f°C BAT", batteryTempC),
            gaugeFraction = (batteryPercent / 100f).coerceIn(0.08f, 1f)
        )
        TelemetryMetric.CPU_LOAD -> MetricReadout(
            primaryValue = "$systemLoadPercent%",
            unitLabel = "SYS LOAD",
            gaugeFraction = (systemLoadPercent / 100f).coerceIn(0.08f, 1f)
        )
    }

    fun isActionActive(action: KansoActionId, isMediaPlaying: Boolean = false): Boolean = when (action) {
        KansoActionId.WIFI -> isWifiConnected
        KansoActionId.MOBILE_DATA -> isMobileConnected
        KansoActionId.BLUETOOTH -> isBluetoothEnabled
        KansoActionId.HOTSPOT -> isHotspotEnabled
        KansoActionId.AIRPLANE -> isAirplaneModeEnabled
        KansoActionId.DND -> isDndEnabled
        KansoActionId.TORCH -> isTorchEnabled
        KansoActionId.AUTO_ROTATE -> isAutoRotateEnabled
        KansoActionId.POWER_SAVING -> isPowerSaveEnabled
        KansoActionId.SILENT_MODE -> ringerMode != AudioManager.RINGER_MODE_SILENT
        KansoActionId.MEDIA_PLAY_PAUSE -> isMediaPlaying
        KansoActionId.LOCATION -> isLocationEnabled
        KansoActionId.NFC -> isNfcEnabled
        else -> false
    }
}

data class PermissionsHealth(
    val isChineseRom: Boolean = false,
    val romRegionLabel: String = "GLOBAL ANDROID",
    val isDefaultHomeLauncher: Boolean = false,
    val notificationAccessGranted: Boolean = false,
    val dndPolicyGranted: Boolean = false,
    val bluetoothConnectGranted: Boolean = true,
    val coarseLocationGranted: Boolean = false,
    val audioVisualizerGranted: Boolean = false,
    val writeSettingsGranted: Boolean = false,
    val writeSecureSettingsGranted: Boolean = false,
    val accessibilityGranted: Boolean = false,
    val usageStatsGranted: Boolean = false,
    val overlayGranted: Boolean = false
)

class TelemetryRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val ACTION_WIFI_AP_STATE_CHANGED = "android.net.wifi.WIFI_AP_STATE_CHANGED"
        private const val EXTRA_WIFI_AP_STATE = "wifi_state"
        private const val WIFI_AP_STATE_ENABLING = 12
        private const val WIFI_AP_STATE_ENABLED = 13
        private const val LOCAL_SOUND_SWITCH_GUARD_MS = 1200L
        private val TETHER_INTERFACE_PREFIXES = listOf("ap0", "swlan0", "wlan1", "wlan2", "softap")

        /**
         * Strict 3-state sound cycle: NORMAL (Ring) -> VIBRATE -> SILENT -> NORMAL.
         */
        fun nextRingerMode(current: Int): Int = when (current) {
            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
            AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
            else -> AudioManager.RINGER_MODE_NORMAL
        }

        /**
         * Pure multi-signal sound-state resolver supporting Global AOSP and Chinese OEM ROMs
         * (Honor MagicOS, Huawei EMUI/HarmonyOS, Xiaomi HyperOS/MIUI, OPPO ColorOS, Vivo OriginOS).
         */
        fun computeEffectiveRingerMode(
            rawMode: Int,
            ringVol: Int,
            ringMuted: Boolean,
            totalSilenceDnd: Boolean,
            globalModeRinger: Int = -1,
            softwareSilentFallbackActive: Boolean = false
        ): Int {
            if (rawMode == AudioManager.RINGER_MODE_SILENT || totalSilenceDnd) {
                return AudioManager.RINGER_MODE_SILENT
            }

            if (softwareSilentFallbackActive && (ringVol == 0 || ringMuted)) {
                return AudioManager.RINGER_MODE_SILENT
            }

            // Chinese OEM tie-breaker: Settings.Global["mode_ringer"] distinguishes SILENT (0) vs VIBRATE (1) vs NORMAL (2)
            if (globalModeRinger == AudioManager.RINGER_MODE_SILENT && ringVol == 0) {
                return AudioManager.RINGER_MODE_SILENT
            }

            if (rawMode == AudioManager.RINGER_MODE_VIBRATE) {
                return AudioManager.RINGER_MODE_VIBRATE
            }

            if (ringVol > 0 && !ringMuted) {
                return AudioManager.RINGER_MODE_NORMAL
            }

            if (globalModeRinger == AudioManager.RINGER_MODE_VIBRATE) {
                return AudioManager.RINGER_MODE_VIBRATE
            }

            if (ringMuted || ringVol == 0) {
                return AudioManager.RINGER_MODE_SILENT
            }

            return AudioManager.RINGER_MODE_NORMAL
        }

        /**
         * Broadcast race guard: decides whether an incoming volume/ringer broadcast should clear
         * any software-silent flag. Prevents OEM Vibrate/Volume broadcasts from snapping Silent back.
         */
        fun shouldClearSoftwareSilentOnBroadcast(
            sinceLocalSwitchMs: Long,
            rawMode: Int,
            ringVol: Int
        ): Boolean {
            if (sinceLocalSwitchMs <= LOCAL_SOUND_SWITCH_GUARD_MS) return false
            if (ringVol > 0) return true
            return rawMode == AudioManager.RINGER_MODE_NORMAL
        }
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private val _telemetry = MutableStateFlow(TelemetryState(weekDays = buildCurrentWeek()))
    val telemetry: StateFlow<TelemetryState> = _telemetry.asStateFlow()

    private val _permissionsHealth = MutableStateFlow(PermissionsHealth())
    val permissionsHealth: StateFlow<PermissionsHealth> = _permissionsHealth.asStateFlow()

    private var lastTrafficBytes: Long = -1L
    private var lastTrafficTimeMs: Long = 0L
    private var torchCameraId: String? = null
    private var lastAudibleRingVolume: Int = -1
    private var lastAudibleNotifVolume: Int = -1
    @Volatile
    private var softwareSilentFallbackActive: Boolean = false
    @Volatile
    private var lastLocalSoundSwitchMs: Long = 0L

    private val mainHandler = Handler(Looper.getMainLooper())

    private val settingsObserver = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean) {
            if (System.currentTimeMillis() - lastLocalSoundSwitchMs > LOCAL_SOUND_SWITCH_GUARD_MS) {
                refreshRingerAndAudioState()
            }
            updateSystemToggles()
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = updateConnectivityFlags()
        override fun onLost(network: Network) = updateConnectivityFlags()
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) =
            updateConnectivityFlags()
    }

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (torchCameraId == null || cameraId == torchCameraId) {
                _telemetry.update { it.copy(isTorchEnabled = enabled) }
            }
        }
    }

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_TIME_TICK,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_DATE_CHANGED -> updateClockAndCalendar()

                Intent.ACTION_BATTERY_CHANGED -> parseBatteryIntent(intent)

                AudioManager.RINGER_MODE_CHANGED_ACTION,
                "android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION",
                "android.media.VOLUME_CHANGED_ACTION" -> {
                    val sinceLocalSwitch = System.currentTimeMillis() - lastLocalSoundSwitchMs
                    if (sinceLocalSwitch > LOCAL_SOUND_SWITCH_GUARD_MS) {
                        val ringVol = runCatching { audioManager.getStreamVolume(AudioManager.STREAM_RING) }.getOrDefault(0)
                        val raw = runCatching { audioManager.ringerMode }.getOrDefault(AudioManager.RINGER_MODE_NORMAL)
                        if (shouldClearSoftwareSilentOnBroadcast(sinceLocalSwitch, raw, ringVol)) {
                            softwareSilentFallbackActive = false
                        }
                        refreshRingerAndAudioState()
                    }
                }

                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    if (state == BluetoothAdapter.STATE_ON || state == BluetoothAdapter.STATE_OFF) {
                        _telemetry.update { it.copy(isBluetoothEnabled = state == BluetoothAdapter.STATE_ON) }
                    }
                }

                ACTION_WIFI_AP_STATE_CHANGED -> {
                    val apState = intent.getIntExtra(EXTRA_WIFI_AP_STATE, -1)
                    val enabled = if (apState != -1) {
                        apState == WIFI_AP_STATE_ENABLED || apState == WIFI_AP_STATE_ENABLING
                    } else {
                        detectHotspotEnabled()
                    }
                    _telemetry.update { it.copy(isHotspotEnabled = enabled) }
                }

                Intent.ACTION_AIRPLANE_MODE_CHANGED,
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED,
                NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED,
                LocationManager.PROVIDERS_CHANGED_ACTION -> {
                    if (System.currentTimeMillis() - lastLocalSoundSwitchMs > LOCAL_SOUND_SWITCH_GUARD_MS) {
                        refreshRingerAndAudioState()
                    }
                    updateSystemToggles()
                }
            }
        }
    }

    init {
        resolveTorchCamera()
        registerReceivers()
        updateClockAndCalendar()
        readStickyBattery()
        refreshRingerAndAudioState()
        updateRamStorageAndConnectivity()
        updateSystemToggles()
        refreshPermissionsHealth()
        startTelemetryPolling()
    }

    private fun resolveTorchCamera() {
        torchCameraId = runCatching {
            cameraManager?.cameraIdList?.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val isBack = chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
                hasFlash && isBack
            } ?: cameraManager?.cameraIdList?.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }

    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION")
            addAction("android.media.VOLUME_CHANGED_ACTION")
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(ACTION_WIFI_AP_STATE_CHANGED)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
        }
        runCatching {
            ContextCompat.registerReceiver(
                context,
                systemReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
        }
        runCatching {
            context.contentResolver.registerContentObserver(
                Settings.System.CONTENT_URI,
                true,
                settingsObserver
            )
        }
        runCatching {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        }
        runCatching {
            cameraManager?.registerTorchCallback(torchCallback, null)
        }
    }

    private fun startTelemetryPolling() {
        scope.launch(Dispatchers.Default) {
            while (isActive) {
                val gov = KansoPerformanceGovernor.state.value
                if (!gov.isAnimationBurstActive) {
                    sampleNetworkTraffic()
                    updateRamStorageAndConnectivity()
                    updateSystemToggles()
                    if (System.currentTimeMillis() - lastLocalSoundSwitchMs > LOCAL_SOUND_SWITCH_GUARD_MS) {
                        refreshRingerAndAudioState()
                    }
                }
                delay(gov.telemetryPollIntervalMs.coerceIn(1200L, 5000L))
            }
        }
    }

    /**
     * Multi-signal device sound-state detector:
     * Inspects AudioManager.ringerMode, ring stream volume, stream mute status, DND total-silence
     * filter, and Settings.Global["mode_ringer"] tie-breaker for Chinese OEM accuracy.
     */
    fun resolveEffectiveRingerMode(): Int {
        val rawMode = runCatching { audioManager.ringerMode }.getOrDefault(AudioManager.RINGER_MODE_NORMAL)
        val ringVol = runCatching { audioManager.getStreamVolume(AudioManager.STREAM_RING) }.getOrDefault(1)
        val ringMuted = runCatching { audioManager.isStreamMute(AudioManager.STREAM_RING) }.getOrDefault(false)
        val totalSilenceDnd = runCatching {
            notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE
        }.getOrDefault(false)
        val globalModeRinger = runCatching {
            Settings.Global.getInt(context.contentResolver, "mode_ringer", -1)
        }.getOrDefault(-1)

        val resolved = computeEffectiveRingerMode(
            rawMode = rawMode,
            ringVol = ringVol,
            ringMuted = ringMuted,
            totalSilenceDnd = totalSilenceDnd,
            globalModeRinger = globalModeRinger,
            softwareSilentFallbackActive = softwareSilentFallbackActive
        )

        if (resolved == AudioManager.RINGER_MODE_NORMAL && ringVol > 0 && !ringMuted) {
            lastAudibleRingVolume = ringVol
            softwareSilentFallbackActive = false
        }
        return resolved
    }

    fun refreshRingerAndAudioState() {
        val effective = resolveEffectiveRingerMode()
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val volFrac = (curVol.toFloat() / maxVol.toFloat()).coerceIn(0f, 1f)
        _telemetry.update {
            it.copy(
                ringerMode = effective,
                mediaVolumeFraction = volFrac
            )
        }
    }

    /**
     * Strict 3-state animated cycle: Ring -> Vibrate -> Silent -> Ring.
     */
    fun cycleVolumeOrShowHud() {
        val nextTarget = nextRingerMode(_telemetry.value.ringerMode)
        setRingerMode(nextTarget)
    }

    fun setRingerMode(target: Int) {
        val nextTarget = when (target) {
            AudioManager.RINGER_MODE_SILENT -> AudioManager.RINGER_MODE_SILENT
            AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_VIBRATE
            else -> AudioManager.RINGER_MODE_NORMAL
        }

        // Enforce DND Policy Access when entering Silent mode (prevents Honor/Android 7+ SecurityException & snap-back)
        if (nextTarget == AudioManager.RINGER_MODE_SILENT &&
            !runCatching { notificationManager.isNotificationPolicyAccessGranted }.getOrDefault(false)
        ) {
            mainHandler.post {
                Toast.makeText(
                    context,
                    "Grant Do Not Disturb access to enable Silent mode",
                    Toast.LENGTH_SHORT
                ).show()
            }
            openDndPolicyAccessSettings()
            return
        }

        // Snapshot audible volumes ONLY while currently in RINGER_MODE_NORMAL with volume > 0
        val currentRawMode = runCatching { audioManager.ringerMode }.getOrDefault(_telemetry.value.ringerMode)
        if (currentRawMode == AudioManager.RINGER_MODE_NORMAL) {
            val curRing = runCatching { audioManager.getStreamVolume(AudioManager.STREAM_RING) }.getOrDefault(0)
            if (curRing > 0) lastAudibleRingVolume = curRing
            val curNotif = runCatching { audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) }.getOrDefault(0)
            if (curNotif > 0) lastAudibleNotifVolume = curNotif
        }

        lastLocalSoundSwitchMs = System.currentTimeMillis()
        val hasDndAccess = runCatching { notificationManager.isNotificationPolicyAccessGranted }.getOrDefault(false)

        when (nextTarget) {
            AudioManager.RINGER_MODE_VIBRATE -> {
                softwareSilentFallbackActive = false
                if (hasDndAccess &&
                    runCatching { notificationManager.currentInterruptionFilter }.getOrDefault(
                        NotificationManager.INTERRUPTION_FILTER_ALL
                    ) == NotificationManager.INTERRUPTION_FILTER_NONE
                ) {
                    runCatching {
                        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                    }
                }
                // Pure vibrate transition: never call ADJUST_UNMUTE or raise STREAM_RING above 0
                runCatching { audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE }
            }

            AudioManager.RINGER_MODE_SILENT -> {
                softwareSilentFallbackActive = false
                // Clear conflicting DND interruption filter first if active on Honor/Huawei so setRingerMode sticks
                if (hasDndAccess) {
                    val curFilter = runCatching { notificationManager.currentInterruptionFilter }
                        .getOrDefault(NotificationManager.INTERRUPTION_FILTER_ALL)
                    if (curFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
                        runCatching {
                            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                        }
                    }
                }
                // Do NOT call adjustStreamVolume(ADJUST_MUTE) after setRingerMode(SILENT), which reverts Honor/Xiaomi back to VIBRATE/NORMAL
                val silentApplied = runCatching {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT
                }.getOrDefault(false)

                if (!silentApplied) {
                    softwareSilentFallbackActive = true
                    runCatching { audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0) }
                    runCatching { audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0) }
                }
            }

            else -> {
                softwareSilentFallbackActive = false
                if (hasDndAccess &&
                    runCatching { notificationManager.currentInterruptionFilter }.getOrDefault(
                        NotificationManager.INTERRUPTION_FILTER_ALL
                    ) == NotificationManager.INTERRUPTION_FILTER_NONE
                ) {
                    runCatching {
                        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                    }
                }
                runCatching { audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL }
                if (runCatching { audioManager.isStreamMute(AudioManager.STREAM_RING) }.getOrDefault(false)) {
                    runCatching { audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0) }
                }
                if (runCatching { audioManager.isStreamMute(AudioManager.STREAM_NOTIFICATION) }.getOrDefault(false)) {
                    runCatching { audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0) }
                }

                val maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING).coerceAtLeast(1)
                val currentRing = runCatching { audioManager.getStreamVolume(AudioManager.STREAM_RING) }.getOrDefault(0)
                if (currentRing == 0) {
                    val restoreVol = if (lastAudibleRingVolume in 1..maxRing) {
                        lastAudibleRingVolume
                    } else {
                        (maxRing / 2).coerceAtLeast(1)
                    }
                    runCatching { audioManager.setStreamVolume(AudioManager.STREAM_RING, restoreVol, 0) }
                }
                val maxNotif = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION).coerceAtLeast(1)
                val currentNotif = runCatching { audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) }.getOrDefault(0)
                if (currentNotif == 0) {
                    val restoreNotif = if (lastAudibleNotifVolume in 1..maxNotif) {
                        lastAudibleNotifVolume
                    } else {
                        (maxNotif / 2).coerceAtLeast(1)
                    }
                    runCatching { audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, restoreNotif, 0) }
                }
            }
        }

        _telemetry.update { it.copy(ringerMode = nextTarget) }
        mainHandler.postDelayed({
            if (System.currentTimeMillis() - lastLocalSoundSwitchMs >= LOCAL_SOUND_SWITCH_GUARD_MS - 50L) {
                refreshRingerAndAudioState()
            }
        }, LOCAL_SOUND_SWITCH_GUARD_MS + 50L)
    }

    private fun updateClockAndCalendar() {
        val now = Date()
        val h24 = SimpleDateFormat("HH", Locale.getDefault()).format(now)
        val h12 = SimpleDateFormat("hh", Locale.getDefault()).format(now)
        val min = SimpleDateFormat("mm", Locale.getDefault()).format(now)
        val dateStr = "Today " + SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(now)
        _telemetry.update {
            it.copy(
                clock = ClockTelemetry(
                    hours24 = h24,
                    hours12 = h12,
                    minutes = min,
                    formattedDate = dateStr
                ),
                weekDays = buildCurrentWeek()
            )
        }
    }

    private fun buildCurrentWeek(): List<CalendarDaySlot> {
        val todayCal = Calendar.getInstance()
        val todayDayOfYear = todayCal.get(Calendar.DAY_OF_YEAR)
        val todayYear = todayCal.get(Calendar.YEAR)

        val startOfWeek = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.SUNDAY
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }

        val labels = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
        return (0 until 7).map { offset ->
            val cal = (startOfWeek.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, offset) }
            CalendarDaySlot(
                dayOfWeekShort = labels[offset],
                dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
                isToday = cal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear &&
                    cal.get(Calendar.YEAR) == todayYear
            )
        }
    }

    private fun readStickyBattery() {
        val intent = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()
        if (intent != null) parseBatteryIntent(intent)
    }

    private fun parseBatteryIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val pct = if (level >= 0 && scale > 0) (level * 100) / scale else 50
        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 310)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        _telemetry.update {
            it.copy(
                batteryPercent = pct.coerceIn(0, 100),
                batteryTempC = (tempTenths / 10f).coerceIn(10f, 70f),
                isCharging = charging
            )
        }
    }

    private fun sampleNetworkTraffic() {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        val nowMs = System.currentTimeMillis()

        if (rx == TrafficStats.UNSUPPORTED.toLong() || tx == TrafficStats.UNSUPPORTED.toLong()) {
            return
        }
        val total = rx + tx
        if (lastTrafficBytes >= 0 && nowMs > lastTrafficTimeMs) {
            val deltaBytes = (total - lastTrafficBytes).coerceAtLeast(0L)
            val elapsedSec = ((nowMs - lastTrafficTimeMs) / 1000.0).coerceAtLeast(0.5)
            val bytesPerSec = deltaBytes / elapsedSec
            val kbPerSec = bytesPerSec / 1024.0
            val (formattedVal, unit) = if (kbPerSec >= 1024.0) {
                String.format(Locale.US, "%.2f", kbPerSec / 1024.0) to "MB/s"
            } else {
                String.format(Locale.US, "%.2f", kbPerSec) to "KB/s"
            }
            val gauge = (kotlin.math.ln(1.0 + kbPerSec) / kotlin.math.ln(1.0 + 2048.0))
                .toFloat()
                .coerceIn(0.08f, 1.0f)

            _telemetry.update {
                it.copy(
                    networkSpeedValue = formattedVal,
                    networkSpeedUnit = unit,
                    networkGaugeFraction = gauge
                )
            }
        }
        lastTrafficBytes = total
        lastTrafficTimeMs = nowMs
    }

    private fun updateConnectivityFlags() {
        val caps = runCatching {
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        }.getOrNull()
        val wifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val mobile = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val hotspot = detectHotspotEnabled()
        _telemetry.update {
            it.copy(
                isWifiConnected = wifi,
                isMobileConnected = mobile,
                isHotspotEnabled = hotspot
            )
        }
    }

    private fun updateRamStorageAndConnectivity() {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val totalMem = memInfo.totalMem.coerceAtLeast(1L).toDouble()
        val availMem = memInfo.availMem.coerceAtLeast(0L).toDouble()
        val usedMem = (totalMem - availMem).coerceAtLeast(0.0)
        val availGb = availMem / (1024.0 * 1024.0 * 1024.0)
        val usedGb = usedMem / (1024.0 * 1024.0 * 1024.0)
        val totalGb = totalMem / (1024.0 * 1024.0 * 1024.0)
        val ramStr = String.format(Locale.US, "%.1fGB", availGb)
        val ramUsedStr = String.format(Locale.US, "%.1f", usedGb)
        val ramTotalStr = String.format(Locale.US, "%.1f", totalGb)
        val ramFraction = (usedMem / totalMem).toFloat().coerceIn(0.05f, 1f)

        val statFs = runCatching { StatFs(Environment.getDataDirectory().path) }.getOrNull()
        val totalBytes = statFs?.totalBytes?.coerceAtLeast(1L)?.toDouble() ?: (128.0 * 1024 * 1024 * 1024)
        val availBytes = statFs?.availableBytes?.coerceAtLeast(0L)?.toDouble() ?: (80.0 * 1024 * 1024 * 1024)
        val usedBytes = (totalBytes - availBytes).coerceAtLeast(0.0)
        val storageUsedStr = String.format(Locale.US, "%.0f", usedBytes / (1024.0 * 1024.0 * 1024.0))
        val storageTotalStr = String.format(Locale.US, "%.0fGB", totalBytes / (1024.0 * 1024.0 * 1024.0))
        val storageFraction = (usedBytes / totalBytes).toFloat().coerceIn(0.05f, 1f)

        val loadPct = ((ramFraction * 0.65f + _telemetry.value.networkGaugeFraction * 0.35f) * 100f)
            .toInt()
            .coerceIn(5, 99)

        val caps = runCatching {
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        }.getOrNull()
        val wifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val mobile = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

        val btEnabled = runCatching {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val hasPerm = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            if (hasPerm) bm?.adapter?.isEnabled == true else false
        }.getOrDefault(false)

        val hotspot = detectHotspotEnabled()
        val effectiveRinger = if (System.currentTimeMillis() - lastLocalSoundSwitchMs > LOCAL_SOUND_SWITCH_GUARD_MS) {
            resolveEffectiveRingerMode()
        } else {
            _telemetry.value.ringerMode
        }

        _telemetry.update {
            it.copy(
                freeRamText = ramStr,
                ramUsedGb = ramUsedStr,
                ramTotalGb = ramTotalStr,
                ramUsageFraction = ramFraction,
                storageUsedGb = storageUsedStr,
                storageTotalGb = storageTotalStr,
                storageUsageFraction = storageFraction,
                systemLoadPercent = loadPct,
                ringerMode = effectiveRinger,
                isWifiConnected = wifi,
                isMobileConnected = mobile,
                isBluetoothEnabled = btEnabled,
                isHotspotEnabled = hotspot
            )
        }
    }

    private fun updateSystemToggles() {
        val autoRotate = runCatching {
            Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
        }.getOrDefault(false)

        val airplane = runCatching {
            Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
        }.getOrDefault(false)

        val powerSave = runCatching { powerManager.isPowerSaveMode }.getOrDefault(false)

        val dndActive = runCatching {
            val filter = notificationManager.currentInterruptionFilter
            filter != NotificationManager.INTERRUPTION_FILTER_ALL &&
                filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        }.getOrDefault(false)

        val locationOn = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                locationManager?.isLocationEnabled == true
            } else {
                @Suppress("DEPRECATION")
                Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE, 0) != 0
            }
        }.getOrDefault(false)

        val nfcOn = runCatching {
            NfcAdapter.getDefaultAdapter(context)?.isEnabled == true
        }.getOrDefault(false)

        val hotspotOn = detectHotspotEnabled()

        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val volFrac = (curVol.toFloat() / maxVol.toFloat()).coerceIn(0f, 1f)

        val brightRaw = runCatching {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 165)
        }.getOrDefault(165)
        val brightFrac = (brightRaw / 255f).coerceIn(0.05f, 1f)

        val audioOutput = resolveAudioOutputLabel()

        _telemetry.update {
            it.copy(
                isAutoRotateEnabled = autoRotate,
                isAirplaneModeEnabled = airplane,
                isPowerSaveEnabled = powerSave,
                isDndEnabled = dndActive,
                isLocationEnabled = locationOn,
                isNfcEnabled = nfcOn,
                isHotspotEnabled = hotspotOn,
                mediaVolumeFraction = volFrac,
                brightnessFraction = brightFrac,
                audioOutputLabel = audioOutput
            )
        }
    }

    @SuppressLint("PrivateApi")
    fun detectHotspotEnabled(): Boolean {
        val wm = wifiManager
        if (wm != null) {
            val viaBool = runCatching {
                val m = wm.javaClass.getDeclaredMethod("isWifiApEnabled")
                m.isAccessible = true
                m.invoke(wm) as? Boolean
            }.getOrNull()
            if (viaBool != null) return viaBool

            val viaState = runCatching {
                val m = wm.javaClass.getDeclaredMethod("getWifiApState")
                m.isAccessible = true
                m.invoke(wm) as? Int
            }.getOrNull()
            if (viaState != null) {
                return viaState == WIFI_AP_STATE_ENABLED || viaState == WIFI_AP_STATE_ENABLING
            }
        }

        // Fallback: inspect active tether network interfaces (ap0, swlan0, wlan1, wlan2)
        return runCatching {
            val ifaces = NetworkInterface.getNetworkInterfaces() ?: return@runCatching false
            while (ifaces.hasMoreElements()) {
                val nif = ifaces.nextElement()
                val name = nif.name?.lowercase(Locale.ROOT).orEmpty()
                if (nif.isUp && !nif.isLoopback && TETHER_INTERFACE_PREFIXES.any { name.startsWith(it) }) {
                    return@runCatching true
                }
            }
            false
        }.getOrDefault(false)
    }

    private fun resolveAudioOutputLabel(): String {
        val devices = runCatching {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        }.getOrNull() ?: return "Device Speaker"

        val external = devices.firstOrNull { d ->
            d.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                d.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && d.type == AudioDeviceInfo.TYPE_BLE_HEADSET) ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && d.type == AudioDeviceInfo.TYPE_BLE_SPEAKER) ||
                d.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                d.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                d.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                d.type == AudioDeviceInfo.TYPE_USB_DEVICE
        } ?: return "Device Speaker"

        val name = external.productName?.toString()?.trim().orEmpty()
        return when {
            name.isNotBlank() && !name.equals(Build.MODEL, ignoreCase = true) -> name
            external.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                external.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth Audio"
            external.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                external.type == AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset"
            external.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                external.type == AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Audio"
            else -> "External Audio"
        }
    }

    fun openAudioOutputSettings() {
        val btIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(btIntent) }.onFailure {
            val soundIntent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(soundIntent) }
        }
    }

    fun refreshPermissionsHealth() {
        val rom = RomDetector.detect()

        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ).orEmpty()
        val notifGranted = enabledListeners.contains(context.packageName) ||
            KansoNotificationListener.isConnected.value

        val btGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val locGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val audioGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val writeSettings = Settings.System.canWrite(context)
        val dndGranted = runCatching { notificationManager.isNotificationPolicyAccessGranted }.getOrDefault(false)

        _permissionsHealth.value = PermissionsHealth(
            isChineseRom = rom.isChineseRom,
            romRegionLabel = rom.regionLabel,
            isDefaultHomeLauncher = RomDetector.isDefaultHomeLauncher(context),
            notificationAccessGranted = notifGranted,
            dndPolicyGranted = dndGranted,
            bluetoothConnectGranted = btGranted,
            coarseLocationGranted = locGranted,
            audioVisualizerGranted = audioGranted,
            writeSettingsGranted = writeSettings,
            writeSecureSettingsGranted = RomDetector.hasWriteSecureSettings(context),
            accessibilityGranted = RomDetector.isAccessibilityServiceEnabled(context),
            usageStatsGranted = RomDetector.hasUsageStatsPermission(context),
            overlayGranted = RomDetector.hasOverlayPermission(context)
        )
    }

    fun setMediaVolumeFraction(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val target = (clamped * maxVol).toInt().coerceIn(0, maxVol)
        runCatching {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        }
        _telemetry.update { it.copy(mediaVolumeFraction = clamped) }
    }

    fun setBrightnessFraction(fraction: Float): Boolean {
        val clamped = fraction.coerceIn(0.05f, 1f)
        if (!Settings.System.canWrite(context)) {
            openWriteSettingsPermission()
            return false
        }
        runCatching {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                (clamped * 255f).toInt().coerceIn(10, 255)
            )
        }
        _telemetry.update { it.copy(brightnessFraction = clamped) }
        return true
    }

    fun toggleTorch(): Boolean {
        val camId = torchCameraId ?: run {
            resolveTorchCamera()
            torchCameraId
        } ?: return false
        val target = !_telemetry.value.isTorchEnabled
        return runCatching {
            cameraManager?.setTorchMode(camId, target)
            _telemetry.update { it.copy(isTorchEnabled = target) }
            true
        }.getOrDefault(false)
    }

    /**
     * Direct-API Auto-Rotate toggle:
     * Writes ACCELEROMETER_ROTATION (and USER_ROTATION when locking) if permitted;
     * otherwise opens Write Settings permission screen directly.
     */
    fun toggleAutoRotate(): Boolean {
        val canWrite = Settings.System.canWrite(context) || RomDetector.hasWriteSecureSettings(context)
        if (!canWrite) {
            openWriteSettingsPermission()
            return false
        }
        val next = !_telemetry.value.isAutoRotateEnabled
        val wrote = try {
            val ok = Settings.System.putInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                if (next) 1 else 0
            )
            if (ok && !next) {
                runCatching {
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.USER_ROTATION,
                        currentDisplayRotation()
                    )
                }
            }
            ok
        } catch (_: SecurityException) {
            false
        } catch (_: Throwable) {
            false
        }

        if (wrote) {
            _telemetry.update { it.copy(isAutoRotateEnabled = next) }
            return true
        }
        openWriteSettingsPermission()
        return false
    }

    private fun currentDisplayRotation(): Int {
        return runCatching {
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            dm?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: Surface.ROTATION_0
        }.getOrDefault(Surface.ROTATION_0)
    }

    /**
     * Direct-API Battery Saver toggle:
     * Uses WRITE_SECURE_SETTINGS (`Settings.Global["low_power"]` + Honor/Huawei `SmartModeStatus`).
     * Opens Battery Saver Settings directly if blocked or ignored by OEM power manager.
     */
    fun togglePowerSaving(): Boolean {
        val enable = !_telemetry.value.isPowerSaveEnabled
        if (!RomDetector.hasWriteSecureSettings(context)) {
            openPowerSavingSettings()
            return false
        }

        val wroteGlobal = runCatching {
            Settings.Global.putInt(context.contentResolver, "low_power", if (enable) 1 else 0)
        }.getOrDefault(false)

        if (RomDetector.isHonorOrHuawei() && Settings.System.canWrite(context)) {
            runCatching {
                Settings.System.putInt(context.contentResolver, "SmartModeStatus", if (enable) 4 else 1)
            }
        }

        if (!wroteGlobal) {
            openPowerSavingSettings()
            return false
        }

        _telemetry.update { it.copy(isPowerSaveEnabled = enable) }
        mainHandler.postDelayed({
            val actual = runCatching { powerManager.isPowerSaveMode }.getOrDefault(enable)
            _telemetry.update { it.copy(isPowerSaveEnabled = actual) }
            if (actual != enable) {
                openPowerSavingSettings()
            }
        }, 320L)
        return true
    }

    /**
     * Direct-API Hotspot toggle:
     * Attempts ConnectivityManager/TetheringManager or WifiManager reflection when permitted;
     * otherwise opens Tether / Personal Hotspot settings directly.
     */
    @SuppressLint("PrivateApi")
    fun toggleHotspot(): Boolean {
        val enable = !_telemetry.value.isHotspotEnabled
        val toggledDirectly = runCatching {
            if (enable) {
                val startMethod = connectivityManager.javaClass.methods.firstOrNull {
                    it.name == "startTethering" && it.parameterTypes.size >= 2
                }
                if (startMethod != null) {
                    val args = when (startMethod.parameterTypes.size) {
                        2 -> arrayOf<Any?>(0, false)
                        3 -> arrayOf<Any?>(0, false, null)
                        else -> arrayOf<Any?>(0, false, null, mainHandler)
                    }
                    startMethod.invoke(connectivityManager, *args)
                    true
                } else {
                    false
                }
            } else {
                val stopMethod = connectivityManager.javaClass.getMethod("stopTethering", Int::class.javaPrimitiveType)
                stopMethod.invoke(connectivityManager, 0)
                true
            }
        }.getOrDefault(false)

        if (toggledDirectly) {
            _telemetry.update { it.copy(isHotspotEnabled = enable) }
            mainHandler.postDelayed({
                _telemetry.update { it.copy(isHotspotEnabled = detectHotspotEnabled()) }
            }, 600L)
            return true
        }

        val viaWifiAp = runCatching {
            val wm = wifiManager ?: return@runCatching false
            val setAp = wm.javaClass.getMethod(
                "setWifiApEnabled",
                @Suppress("DEPRECATION") android.net.wifi.WifiConfiguration::class.java,
                Boolean::class.javaPrimitiveType
            )
            setAp.invoke(wm, null, enable) as? Boolean ?: false
        }.getOrDefault(false)

        if (viaWifiAp) {
            _telemetry.update { it.copy(isHotspotEnabled = enable) }
            return true
        }

        openHotspotSettings()
        return false
    }

    fun toggleDnd(): Boolean {
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            openDndPolicyAccessSettings()
            return false
        }
        val enableDnd = !_telemetry.value.isDndEnabled
        return runCatching {
            notificationManager.setInterruptionFilter(
                if (enableDnd) NotificationManager.INTERRUPTION_FILTER_PRIORITY
                else NotificationManager.INTERRUPTION_FILTER_ALL
            )
            _telemetry.update { it.copy(isDndEnabled = enableDnd) }
            true
        }.getOrDefault(false)
    }

    fun openDndPolicyAccessSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openWriteSettingsPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openVolumePanel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val intent = Intent(Settings.Panel.ACTION_VOLUME).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (runCatching { context.startActivity(intent); true }.getOrDefault(false)) return
        }
        runCatching {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_SAME,
                AudioManager.FLAG_SHOW_UI
            )
        }
    }

    fun openInternetPanel() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

        val launched = runCatching { context.startActivity(intent); true }.getOrDefault(false)
        if (!launched) {
            val fallback = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(fallback) }
        }
    }

    fun openMobileDataPanel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            openInternetPanel()
        } else {
            val fallback = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(fallback) }
        }
    }

    fun openBluetoothControls() {
        val hasPerm = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = btManager?.adapter

        val intent = if (hasPerm && adapter != null && !adapter.isEnabled) {
            Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        } else {
            Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        }.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

        runCatching { context.startActivity(intent) }.onFailure {
            val fallback = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(fallback) }
        }
    }

    fun openHotspotSettings() {
        val tetherIntent = Intent(Intent.ACTION_MAIN).apply {
            component = ComponentName("com.android.settings", "com.android.settings.TetherSettings")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val launched = runCatching { context.startActivity(tetherIntent); true }.getOrDefault(false)
        if (!launched) {
            val fallback = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(fallback) }
        }
    }

    fun openAirplaneSettings() {
        val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openPowerSavingSettings() {
        val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openBatterySettings() {
        val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }.onFailure {
            openPowerSavingSettings()
        }
    }

    fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openNfcSettings() {
        val intent = Intent(Settings.ACTION_NFC_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }.onFailure {
            openSystemSettings()
        }
    }

    fun openCastSettings() {
        val intent = Intent(Settings.ACTION_CAST_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }.onFailure {
            openDisplaySettings()
        }
    }

    fun openDisplaySettings() {
        val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openSystemSettings() {
        val intent = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun unregister() {
        runCatching { context.unregisterReceiver(systemReceiver) }
        runCatching { context.contentResolver.unregisterContentObserver(settingsObserver) }
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        runCatching { cameraManager?.unregisterTorchCallback(torchCallback) }
    }
}
