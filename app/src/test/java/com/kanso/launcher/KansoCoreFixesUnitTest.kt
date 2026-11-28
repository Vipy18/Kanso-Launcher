package com.kanso.launcher

import android.media.AudioManager
import com.kanso.launcher.data.model.KansoActionId
import com.kanso.launcher.data.telemetry.TelemetryRepository
import com.kanso.launcher.data.telemetry.TelemetryState
import com.kanso.launcher.service.RecentsSnapshotCache
import com.kanso.launcher.util.RomDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KansoCoreFixesUnitTest {

    // --- Issue 1: 3-State Sound Cycling & Broadcast Race Protection ---

    @Test
    fun nextRingerMode_cyclesNormalToVibrateToSilentToNormal() {
        assertEquals(
            AudioManager.RINGER_MODE_VIBRATE,
            TelemetryRepository.nextRingerMode(AudioManager.RINGER_MODE_NORMAL)
        )
        assertEquals(
            AudioManager.RINGER_MODE_SILENT,
            TelemetryRepository.nextRingerMode(AudioManager.RINGER_MODE_VIBRATE)
        )
        assertEquals(
            AudioManager.RINGER_MODE_NORMAL,
            TelemetryRepository.nextRingerMode(AudioManager.RINGER_MODE_SILENT)
        )
    }

    @Test
    fun computeEffectiveRingerMode_handlesHonorAndOemModes() {
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
                rawMode = AudioManager.RINGER_MODE_VIBRATE,
                ringVol = 0,
                ringMuted = true,
                totalSilenceDnd = true
            )
        )

        // Honor/Chinese OEM tie-breaker: Settings.Global["mode_ringer"] == 0 while raw reports VIBRATE
        assertEquals(
            AudioManager.RINGER_MODE_SILENT,
            TelemetryRepository.computeEffectiveRingerMode(
                rawMode = AudioManager.RINGER_MODE_VIBRATE,
                ringVol = 0,
                ringMuted = false,
                totalSilenceDnd = false,
                globalModeRinger = AudioManager.RINGER_MODE_SILENT
            )
        )

        // Vibrate with ringVol == 0 stays VIBRATE (does not snap to NORMAL or SILENT)
        assertEquals(
            AudioManager.RINGER_MODE_VIBRATE,
            TelemetryRepository.computeEffectiveRingerMode(
                rawMode = AudioManager.RINGER_MODE_VIBRATE,
                ringVol = 0,
                ringMuted = false,
                totalSilenceDnd = false,
                globalModeRinger = AudioManager.RINGER_MODE_VIBRATE
            )
        )

        assertEquals(
            AudioManager.RINGER_MODE_NORMAL,
            TelemetryRepository.computeEffectiveRingerMode(
                rawMode = AudioManager.RINGER_MODE_NORMAL,
                ringVol = 5,
                ringMuted = false,
                totalSilenceDnd = false,
                globalModeRinger = AudioManager.RINGER_MODE_NORMAL
            )
        )

        // Software silent guard holds SILENT when ringVol == 0
        assertEquals(
            AudioManager.RINGER_MODE_SILENT,
            TelemetryRepository.computeEffectiveRingerMode(
                rawMode = AudioManager.RINGER_MODE_VIBRATE,
                ringVol = 0,
                ringMuted = false,
                totalSilenceDnd = false,
                softwareSilentFallbackActive = true
            )
        )
    }

    @Test
    fun shouldClearSoftwareSilentOnBroadcast_guardsAgainstVibrateSnapBack() {
        // Inside 1200ms local switch guard window: never clear
        assertFalse(
            TelemetryRepository.shouldClearSoftwareSilentOnBroadcast(
                sinceLocalSwitchMs = 400L,
                rawMode = AudioManager.RINGER_MODE_NORMAL,
                ringVol = 5
            )
        )
        // Outside window, OEM Vibrate broadcast with ringVol 0 must not snap back
        assertFalse(
            TelemetryRepository.shouldClearSoftwareSilentOnBroadcast(
                sinceLocalSwitchMs = 2500L,
                rawMode = AudioManager.RINGER_MODE_VIBRATE,
                ringVol = 0
            )
        )
        // Outside window, user raised ring volume or switched to NORMAL -> clear
        assertTrue(
            TelemetryRepository.shouldClearSoftwareSilentOnBroadcast(
                sinceLocalSwitchMs = 2500L,
                rawMode = AudioManager.RINGER_MODE_VIBRATE,
                ringVol = 3
            )
        )
        assertTrue(
            TelemetryRepository.shouldClearSoftwareSilentOnBroadcast(
                sinceLocalSwitchMs = 2500L,
                rawMode = AudioManager.RINGER_MODE_NORMAL,
                ringVol = 0
            )
        )
    }

    @Test
    fun telemetryState_actionActiveReflectsHotspotAndSoundStates() {
        val offState = TelemetryState(
            isHotspotEnabled = false,
            ringerMode = AudioManager.RINGER_MODE_SILENT
        )
        assertFalse(offState.isActionActive(KansoActionId.HOTSPOT))
        assertFalse(offState.isActionActive(KansoActionId.SILENT_MODE))

        val onState = TelemetryState(
            isHotspotEnabled = true,
            ringerMode = AudioManager.RINGER_MODE_VIBRATE
        )
        assertTrue(onState.isActionActive(KansoActionId.HOTSPOT))
        assertTrue(onState.isActionActive(KansoActionId.SILENT_MODE))
    }

    // --- Issue 3: Recents Dismissed-Task Cutoff Filtering ---

    @Test
    fun shouldIncludeTask_excludesDismissedTasksUntilRelaunchedLater() {
        val cutoffs = mapOf(
            "com.spotify.music" to 10_000L,
            "com.tencent.mm" to 20_000L
        )

        // Zero/invalid timestamp excluded
        assertFalse(RecentsSnapshotCache.shouldIncludeTask("com.android.chrome", 0L, cutoffs))

        // Undismissed app included
        assertTrue(RecentsSnapshotCache.shouldIncludeTask("com.android.chrome", 5_000L, cutoffs))

        // Dismissed app with older or equal lastTimeUsed excluded
        assertFalse(RecentsSnapshotCache.shouldIncludeTask("com.spotify.music", 9_999L, cutoffs))
        assertFalse(RecentsSnapshotCache.shouldIncludeTask("com.spotify.music", 10_000L, cutoffs))

        // Dismissed app genuinely relaunched after cutoff reappears
        assertTrue(RecentsSnapshotCache.shouldIncludeTask("com.spotify.music", 10_001L, cutoffs))
        assertFalse(RecentsSnapshotCache.shouldIncludeTask("com.tencent.mm", 19_500L, cutoffs))
    }

    // --- Issue 5: Multi-OEM Chinese vs Global ROM Classification ---

    @Test
    fun classifyRom_identifiesChineseOemAndGlobalDevicesAccurately() {
        val honorCn = RomDetector.classifyRom(
            manufacturer = "HONOR",
            model = "LGE-AN10",
            countryProp = "cn",
            localeProp = "zh-CN"
        )
        assertTrue(honorCn.isChineseRom)
        assertEquals("CN OEM ROM (HONOR)", honorCn.regionLabel)

        val huaweiCn = RomDetector.classifyRom(
            manufacturer = "HUAWEI",
            model = "ALN-AL00",
            countryProp = "CN"
        )
        assertTrue(huaweiCn.isChineseRom)

        val xiaomiCn = RomDetector.classifyRom(
            manufacturer = "Xiaomi",
            model = "23116PN5BC",
            miuiRegion = "CN"
        )
        assertTrue(xiaomiCn.isChineseRom)

        val oppoCn = RomDetector.classifyRom(
            manufacturer = "OPPO",
            model = "PJD110",
            oplusRegion = "CN"
        )
        assertTrue(oppoCn.isChineseRom)

        val vivoCn = RomDetector.classifyRom(
            manufacturer = "vivo",
            model = "V2309A",
            vivoOverseas = "no"
        )
        assertTrue(vivoCn.isChineseRom)

        val honorGlobal = RomDetector.classifyRom(
            manufacturer = "HONOR",
            model = "LGE-NX9",
            countryProp = "eu",
            regionProp = "GB",
            localeProp = "en-GB"
        )
        assertFalse(honorGlobal.isChineseRom)
        assertEquals("GLOBAL OEM (HONOR)", honorGlobal.regionLabel)

        val pixelGlobal = RomDetector.classifyRom(
            manufacturer = "Google",
            model = "Pixel 9 Pro",
            regionProp = "US",
            localeProp = "en-US"
        )
        assertFalse(pixelGlobal.isChineseRom)
        assertEquals("GLOBAL ANDROID (GOOGLE)", pixelGlobal.regionLabel)

        assertTrue(RomDetector.isHonorOrHuawei("HONOR"))
        assertTrue(RomDetector.isHonorOrHuawei("Huawei"))
        assertFalse(RomDetector.isHonorOrHuawei("Google"))
    }
}
