package com.kanso.launcher

import android.media.AudioManager
import com.kanso.launcher.data.model.KansoActionId
import com.kanso.launcher.data.telemetry.TelemetryRepository
import com.kanso.launcher.data.telemetry.TelemetryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetrySoundModeTest {

    @Test
    fun nextRingerMode_cyclesStrictlyNormalVibrateSilentNormal() {
        val afterNormal = TelemetryRepository.nextRingerMode(AudioManager.RINGER_MODE_NORMAL)
        assertEquals(AudioManager.RINGER_MODE_VIBRATE, afterNormal)

        val afterVibrate = TelemetryRepository.nextRingerMode(afterNormal)
        assertEquals(AudioManager.RINGER_MODE_SILENT, afterVibrate)

        val afterSilent = TelemetryRepository.nextRingerMode(afterVibrate)
        assertEquals(AudioManager.RINGER_MODE_NORMAL, afterSilent)
    }

    @Test
    fun nextRingerMode_unknownValueResetsToNormal() {
        assertEquals(AudioManager.RINGER_MODE_NORMAL, TelemetryRepository.nextRingerMode(-1))
        assertEquals(AudioManager.RINGER_MODE_NORMAL, TelemetryRepository.nextRingerMode(99))
    }

    @Test
    fun computeEffectiveRingerMode_respectsDirectRawSilentAndDndTotalSilence() {
        assertEquals(
            AudioManager.RINGER_MODE_SILENT,
            TelemetryRepository.computeEffectiveRingerMode(
                rawMode = AudioManager.RINGER_MODE_SILENT,
                ringVol = 0,
                ringMuted = true,
                totalSilenceDnd = false
            )
        )
        assertEquals(
            AudioManager.RINGER_MODE_SILENT,
            TelemetryRepository.computeEffectiveRingerMode(
                rawMode = AudioManager.RINGER_MODE_NORMAL,
                ringVol = 5,
                ringMuted = false,
                totalSilenceDnd = true
            )
        )
    }

    @Test
    fun computeEffectiveRingerMode_disambiguatesHonorOemSilentVsVibrateViaGlobalModeRinger() {
        // Honor MagicOS may report rawMode = VIBRATE with ringVol = 0 while global mode_ringer = SILENT (0)
        val oemSilent = TelemetryRepository.computeEffectiveRingerMode(
            rawMode = AudioManager.RINGER_MODE_VIBRATE,
            ringVol = 0,
            ringMuted = false,
            totalSilenceDnd = false,
            globalModeRinger = AudioManager.RINGER_MODE_SILENT,
            softwareSilentFallbackActive = false
        )
        assertEquals(AudioManager.RINGER_MODE_SILENT, oemSilent)

        // Genuine Vibrate: rawMode = VIBRATE, globalModeRinger = VIBRATE
        val oemVibrate = TelemetryRepository.computeEffectiveRingerMode(
            rawMode = AudioManager.RINGER_MODE_VIBRATE,
            ringVol = 0,
            ringMuted = false,
            totalSilenceDnd = false,
            globalModeRinger = AudioManager.RINGER_MODE_VIBRATE,
            softwareSilentFallbackActive = false
        )
        assertEquals(AudioManager.RINGER_MODE_VIBRATE, oemVibrate)
    }

    @Test
    fun computeEffectiveRingerMode_preservesSoftwareSilentFallbackWhenRingZero() {
        val resolved = TelemetryRepository.computeEffectiveRingerMode(
            rawMode = AudioManager.RINGER_MODE_VIBRATE,
            ringVol = 0,
            ringMuted = true,
            totalSilenceDnd = false,
            globalModeRinger = AudioManager.RINGER_MODE_VIBRATE,
            softwareSilentFallbackActive = true
        )
        assertEquals(AudioManager.RINGER_MODE_SILENT, resolved)
    }

    @Test
    fun computeEffectiveRingerMode_reportsNormalWhenRingVolumeAudible() {
        val resolved = TelemetryRepository.computeEffectiveRingerMode(
            rawMode = AudioManager.RINGER_MODE_NORMAL,
            ringVol = 7,
            ringMuted = false,
            totalSilenceDnd = false
        )
        assertEquals(AudioManager.RINGER_MODE_NORMAL, resolved)
    }

    @Test
    fun shouldClearSoftwareSilentOnBroadcast_guardsAgainstImmediateOemRace() {
        // Within 1200ms guard window, Vibrate/Volume broadcasts must NOT clear software-silent
        assertFalse(
            TelemetryRepository.shouldClearSoftwareSilentOnBroadcast(
                sinceLocalSwitchMs = 150L,
                rawMode = AudioManager.RINGER_MODE_VIBRATE,
                ringVol = 0
            )
        )
        assertFalse(
            TelemetryRepository.shouldClearSoftwareSilentOnBroadcast(
                sinceLocalSwitchMs = 1200L,
                rawMode = AudioManager.RINGER_MODE_NORMAL,
                ringVol = 5
            )
        )

        // After guard window: Vibrate with ringVol=0 still does not clear; Normal or audible ringVol does
        assertFalse(
            TelemetryRepository.shouldClearSoftwareSilentOnBroadcast(
                sinceLocalSwitchMs = 1500L,
                rawMode = AudioManager.RINGER_MODE_VIBRATE,
                ringVol = 0
            )
        )
        assertTrue(
            TelemetryRepository.shouldClearSoftwareSilentOnBroadcast(
                sinceLocalSwitchMs = 1500L,
                rawMode = AudioManager.RINGER_MODE_NORMAL,
                ringVol = 0
            )
        )
        assertTrue(
            TelemetryRepository.shouldClearSoftwareSilentOnBroadcast(
                sinceLocalSwitchMs = 1500L,
                rawMode = AudioManager.RINGER_MODE_VIBRATE,
                ringVol = 3
            )
        )
    }

    @Test
    fun telemetryState_isActionActive_reflectsHotspotPowerRotateAndSilentStates() {
        val activeState = TelemetryState(
            isHotspotEnabled = true,
            isPowerSaveEnabled = true,
            isAutoRotateEnabled = true,
            ringerMode = AudioManager.RINGER_MODE_NORMAL
        )
        assertTrue(activeState.isActionActive(KansoActionId.HOTSPOT))
        assertTrue(activeState.isActionActive(KansoActionId.POWER_SAVING))
        assertTrue(activeState.isActionActive(KansoActionId.AUTO_ROTATE))
        assertTrue(activeState.isActionActive(KansoActionId.SILENT_MODE))

        val inactiveState = TelemetryState(
            isHotspotEnabled = false,
            isPowerSaveEnabled = false,
            isAutoRotateEnabled = false,
            ringerMode = AudioManager.RINGER_MODE_SILENT
        )
        assertFalse(inactiveState.isActionActive(KansoActionId.HOTSPOT))
        assertFalse(inactiveState.isActionActive(KansoActionId.POWER_SAVING))
        assertFalse(inactiveState.isActionActive(KansoActionId.AUTO_ROTATE))
        assertFalse(inactiveState.isActionActive(KansoActionId.SILENT_MODE))
    }
}
