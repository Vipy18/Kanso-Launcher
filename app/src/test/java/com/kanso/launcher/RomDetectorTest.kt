package com.kanso.launcher

import com.kanso.launcher.util.RomDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RomDetectorTest {

    @Test
    fun classifyRom_detectsChineseHonorMagic4Pro() {
        val info = RomDetector.classifyRom(
            manufacturer = "HONOR",
            model = "LGE-AN10",
            countryProp = "cn",
            regionProp = "CN"
        )
        assertTrue(info.isChineseRom)
        assertEquals("CN OEM ROM (HONOR)", info.regionLabel)
        assertTrue(RomDetector.isHonorOrHuawei("HONOR"))
    }

    @Test
    fun classifyRom_detectsChineseHuaweiXiaomiOppoVivo() {
        assertTrue(
            RomDetector.classifyRom(
                manufacturer = "HUAWEI",
                model = "ALN-AL00",
                countryProp = "cn"
            ).isChineseRom
        )
        assertTrue(
            RomDetector.classifyRom(
                manufacturer = "Xiaomi",
                model = "24031PN0DC",
                miuiRegion = "cn"
            ).isChineseRom
        )
        assertTrue(
            RomDetector.classifyRom(
                manufacturer = "OPPO",
                model = "PJD110",
                oplusRegion = "CN"
            ).isChineseRom
        )
        assertTrue(
            RomDetector.classifyRom(
                manufacturer = "vivo",
                model = "V2309A",
                vivoOverseas = "no"
            ).isChineseRom
        )
    }

    @Test
    fun classifyRom_distinguishesGlobalOemFromChineseRom() {
        val globalHonor = RomDetector.classifyRom(
            manufacturer = "HONOR",
            model = "LGE-NX9",
            countryProp = "eu",
            regionProp = "DE",
            localeProp = "en-GB"
        )
        assertFalse(globalHonor.isChineseRom)
        assertEquals("GLOBAL OEM (HONOR)", globalHonor.regionLabel)

        val globalVivo = RomDetector.classifyRom(
            manufacturer = "vivo",
            model = "V2309",
            vivoOverseas = "yes",
            regionProp = "IN"
        )
        assertFalse(globalVivo.isChineseRom)
        assertEquals("GLOBAL OEM (VIVO)", globalVivo.regionLabel)
    }

    @Test
    fun classifyRom_identifiesGlobalAospDevicesEvenWithChinaLocale() {
        val pixel = RomDetector.classifyRom(
            manufacturer = "Google",
            model = "Pixel 9 Pro",
            regionProp = "US",
            localeProp = "en-US"
        )
        assertFalse(pixel.isChineseRom)
        assertEquals("GLOBAL ANDROID (GOOGLE)", pixel.regionLabel)
        assertFalse(RomDetector.isHonorOrHuawei("Google"))
    }
}
