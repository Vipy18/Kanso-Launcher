package com.kanso.launcher

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.kanso.launcher.data.model.KansoActionId
import com.kanso.launcher.data.telemetry.TelemetryRepository
import com.kanso.launcher.data.telemetry.TelemetryState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class TelemetryRepositoryRobolectricTest {

    private lateinit var app: Application
    private lateinit var scope: CoroutineScope
    private lateinit var repo: TelemetryRepository
    private lateinit var audioManager: AudioManager
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        audioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        notificationManager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        audioManager.setStreamVolume(AudioManager.STREAM_RING, 5, 0)
        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        repo = TelemetryRepository(app, scope)
    }

    @After
    fun tearDown() {
        repo.unregister()
        scope.cancel()
    }

    @Test
    fun cycleVolume_withoutDndAccessOpensDndSettingsInsteadOfBreakingVibrateToSound() {
        val shadowNm = Shadows.shadowOf(notificationManager)
        shadowNm.setNotificationPolicyAccessGranted(false)

        // Step 1: NORMAL -> VIBRATE works without DND access
        repo.cycleVolumeOrShowHud()
        assertEquals(AudioManager.RINGER_MODE_VIBRATE, audioManager.ringerMode)
        assertEquals(AudioManager.RINGER_MODE_VIBRATE, repo.telemetry.value.ringerMode)

        // Clear prior activities then tap again to enter SILENT without DND access
        val shadowApp = Shadows.shadowOf(app)
        shadowApp.clearNextStartedActivities()

        repo.cycleVolumeOrShowHud()

        val startedIntent = shadowApp.nextStartedActivity
        assertNotNull("Expected DND policy settings intent", startedIntent)
        assertEquals(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS, startedIntent.action)
        // Must remain in VIBRATE (never snapped to RINGER_MODE_NORMAL)
        assertEquals(AudioManager.RINGER_MODE_VIBRATE, audioManager.ringerMode)
    }

    @Test
    fun cycleVolume_withDndAccessCyclesCleanlyNormalVibrateSilentNormal() {
        val shadowNm = Shadows.shadowOf(notificationManager)
        shadowNm.setNotificationPolicyAccessGranted(true)

        // NORMAL -> VIBRATE
        repo.cycleVolumeOrShowHud()
        assertEquals(AudioManager.RINGER_MODE_VIBRATE, repo.telemetry.value.ringerMode)

        // VIBRATE -> SILENT
        repo.cycleVolumeOrShowHud()
        assertEquals(AudioManager.RINGER_MODE_SILENT, audioManager.ringerMode)
        assertEquals(AudioManager.RINGER_MODE_SILENT, repo.telemetry.value.ringerMode)

        // SILENT -> NORMAL with audible ring volume restored
        repo.cycleVolumeOrShowHud()
        assertEquals(AudioManager.RINGER_MODE_NORMAL, audioManager.ringerMode)
        assertEquals(AudioManager.RINGER_MODE_NORMAL, repo.telemetry.value.ringerMode)
        assertTrue(audioManager.getStreamVolume(AudioManager.STREAM_RING) >= 1)
    }

    @Test
    fun setMediaVolumeFraction_clampsAndUpdatesAudioStream() {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        repo.setMediaVolumeFraction(0.5f)
        val midVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        assertTrue(midVol in 0..maxVol)
        assertEquals(midVol.toFloat() / maxVol.toFloat(), repo.telemetry.value.mediaVolumeFraction, 0.05f)

        repo.setMediaVolumeFraction(2.0f)
        assertEquals(maxVol, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))

        repo.setMediaVolumeFraction(-1.0f)
        assertEquals(0, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }

    @Test
    fun hardwareTogglesAndSettingsFallbacks_launchSettingsCleanlyWithoutCrashing() {
        val shadowApp = Shadows.shadowOf(app)
        shadowApp.clearNextStartedActivities()

        repo.togglePowerSaving()
        val powerIntent = shadowApp.nextStartedActivity
        assertNotNull(powerIntent)
        assertEquals(Settings.ACTION_BATTERY_SAVER_SETTINGS, powerIntent.action)

        shadowApp.clearNextStartedActivities()
        repo.openBluetoothControls()
        val btIntent = shadowApp.nextStartedActivity
        assertNotNull(btIntent)

        shadowApp.clearNextStartedActivities()
        repo.toggleHotspot()
        val hotspotIntent = shadowApp.nextStartedActivity
        assertNotNull(hotspotIntent)
    }

    @Test
    fun telemetryState_allActionActiveFlagsAndReadoutsWork() {
        val state = TelemetryState(
            batteryPercent = 82,
            isCharging = true,
            networkSpeedValue = "14.2",
            networkSpeedUnit = "MB/s",
            ramUsageFraction = 0.61f,
            freeRamText = "4.8 GB",
            storageUsageFraction = 0.44f,
            storageUsedGb = "112",
            storageTotalGb = "256G",
            systemLoadPercent = 37,
            isWifiConnected = true,
            isMobileConnected = false,
            isBluetoothEnabled = true,
            isTorchEnabled = true,
            isPowerSaveEnabled = true,
            isAutoRotateEnabled = true,
            isDndEnabled = true,
            isHotspotEnabled = true,
            ringerMode = AudioManager.RINGER_MODE_NORMAL
        )
        assertTrue(state.isActionActive(KansoActionId.WIFI))
        assertFalse(state.isActionActive(KansoActionId.MOBILE_DATA))
        assertTrue(state.isActionActive(KansoActionId.BLUETOOTH))
        assertTrue(state.isActionActive(KansoActionId.TORCH))
        assertTrue(state.isActionActive(KansoActionId.POWER_SAVING))
        assertTrue(state.isActionActive(KansoActionId.AUTO_ROTATE))
        assertTrue(state.isActionActive(KansoActionId.DND))
        assertTrue(state.isActionActive(KansoActionId.HOTSPOT))
        assertTrue(state.isActionActive(KansoActionId.SILENT_MODE))
        assertTrue(state.isActionActive(KansoActionId.MEDIA_PLAY_PAUSE, isMediaPlaying = true))
        assertFalse(state.isActionActive(KansoActionId.MEDIA_PLAY_PAUSE, isMediaPlaying = false))
    }
}
