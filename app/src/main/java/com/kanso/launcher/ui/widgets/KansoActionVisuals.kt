package com.kanso.launcher.ui.widgets

import android.media.AudioManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.AirplanemodeInactive
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DoNotDisturbOff
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ScreenLockRotation
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsApplications
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.ui.graphics.vector.ImageVector
import com.kanso.launcher.data.model.KansoActionId
import com.kanso.launcher.data.telemetry.TelemetryState

fun iconForAction(
    action: KansoActionId,
    telemetry: TelemetryState,
    isMediaPlaying: Boolean
): ImageVector = when (action) {
    KansoActionId.WIFI -> if (telemetry.isWifiConnected) Icons.Rounded.Wifi else Icons.Rounded.WifiOff
    KansoActionId.MOBILE_DATA -> Icons.Rounded.SwapVert
    KansoActionId.BLUETOOTH -> if (telemetry.isBluetoothEnabled) Icons.Rounded.Bluetooth else Icons.Rounded.BluetoothDisabled
    KansoActionId.HOTSPOT -> Icons.Rounded.WifiTethering
    KansoActionId.AIRPLANE -> if (telemetry.isAirplaneModeEnabled) Icons.Rounded.AirplanemodeActive else Icons.Rounded.AirplanemodeInactive
    KansoActionId.DND -> if (telemetry.isDndEnabled) Icons.Rounded.DoNotDisturbOn else Icons.Rounded.DoNotDisturbOff
    KansoActionId.TORCH -> if (telemetry.isTorchEnabled) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff
    KansoActionId.AUTO_ROTATE -> if (telemetry.isAutoRotateEnabled) Icons.Rounded.ScreenRotation else Icons.Rounded.ScreenLockRotation
    KansoActionId.POWER_SAVING -> Icons.Rounded.BatterySaver
    KansoActionId.SILENT_MODE -> when (telemetry.ringerMode) {
        AudioManager.RINGER_MODE_SILENT -> Icons.AutoMirrored.Rounded.VolumeOff
        AudioManager.RINGER_MODE_VIBRATE -> Icons.Rounded.Vibration
        else -> Icons.AutoMirrored.Rounded.VolumeUp
    }
    KansoActionId.MEDIA_PLAY_PAUSE -> if (isMediaPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow
    KansoActionId.LOCK_SCREEN -> Icons.Rounded.Lock
    KansoActionId.RECENTS -> Icons.Rounded.Layers
    KansoActionId.SCREENSHOT -> Icons.Rounded.Screenshot
    KansoActionId.VOLUME_PANEL -> Icons.Rounded.Tune
    KansoActionId.BATTERY_SETTINGS -> Icons.Rounded.BatteryChargingFull
    KansoActionId.LOCATION -> if (telemetry.isLocationEnabled) Icons.Rounded.LocationOn else Icons.Rounded.LocationOff
    KansoActionId.NFC -> Icons.Rounded.Nfc
    KansoActionId.CAST -> Icons.Rounded.Cast
    KansoActionId.DARK_MODE -> Icons.Rounded.DarkMode
    KansoActionId.DISPLAY_SETTINGS -> Icons.Rounded.Brightness6
    KansoActionId.SYSTEM_SETTINGS -> Icons.Rounded.SettingsApplications
    KansoActionId.OPEN_CLOCK -> Icons.Rounded.Schedule
    KansoActionId.OPEN_CALENDAR -> Icons.Rounded.CalendarMonth
    KansoActionId.OPEN_SETTINGS -> Icons.Rounded.Settings
    KansoActionId.LAUNCH_APP -> Icons.Rounded.Apps
}

fun statusLabelForAction(
    action: KansoActionId,
    telemetry: TelemetryState,
    isMediaPlaying: Boolean
): String = when (action) {
    KansoActionId.WIFI -> if (telemetry.isWifiConnected) "ON" else "OFF"
    KansoActionId.MOBILE_DATA -> if (telemetry.isMobileConnected) "LIVE" else "OFF"
    KansoActionId.BLUETOOTH -> if (telemetry.isBluetoothEnabled) "ON" else "OFF"
    KansoActionId.HOTSPOT -> "OPEN"
    KansoActionId.AIRPLANE -> if (telemetry.isAirplaneModeEnabled) "ON" else "OFF"
    KansoActionId.DND -> if (telemetry.isDndEnabled) "ON" else "OFF"
    KansoActionId.TORCH -> if (telemetry.isTorchEnabled) "ON" else "OFF"
    KansoActionId.AUTO_ROTATE -> if (telemetry.isAutoRotateEnabled) "AUTO" else "LOCK"
    KansoActionId.POWER_SAVING -> if (telemetry.isPowerSaveEnabled) "ECO" else "STD"
    KansoActionId.SILENT_MODE -> when (telemetry.ringerMode) {
        AudioManager.RINGER_MODE_SILENT -> "SILENT"
        AudioManager.RINGER_MODE_VIBRATE -> "VIBRATE"
        else -> "RING"
    }
    KansoActionId.MEDIA_PLAY_PAUSE -> if (isMediaPlaying) "PLAY" else "IDLE"
    KansoActionId.LOCATION -> if (telemetry.isLocationEnabled) "ON" else "OFF"
    KansoActionId.NFC -> if (telemetry.isNfcEnabled) "ON" else "OFF"
    else -> action.shortLabel
}
