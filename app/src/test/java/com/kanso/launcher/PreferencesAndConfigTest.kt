package com.kanso.launcher

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.kanso.launcher.data.model.AccentBlockStyle
import com.kanso.launcher.data.model.ActionTileBinding
import com.kanso.launcher.data.model.KansoActionId
import com.kanso.launcher.data.model.MechaWidgetTheme
import com.kanso.launcher.data.model.ModularWidgetConfig
import com.kanso.launcher.data.model.TelemetryMetric
import com.kanso.launcher.data.prefs.BuiltInWidgetKey
import com.kanso.launcher.data.prefs.CachedWeather
import com.kanso.launcher.data.prefs.DrawerViewMode
import com.kanso.launcher.data.prefs.IconPackMode
import com.kanso.launcher.data.prefs.KansoPreferences
import com.kanso.launcher.data.prefs.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class PreferencesAndConfigTest {

    @Test
    fun modularWidgetConfig_encodeDecodeRoundTripAndMalformedFallback() {
        val original = ModularWidgetConfig(
            telemetryMetric = TelemetryMetric.RAM_USAGE,
            tapAction = KansoActionId.BATTERY_SETTINGS,
            toggleAction = KansoActionId.TORCH,
            customLabel = "MAIN LAMP",
            targetPackage = "com.example.tool",
            targetActivity = "com.example.tool.Main",
            matrixRows = 2,
            matrixCols = 4,
            matrixTiles = ModularWidgetConfig.defaultMatrixTiles(8),
            showAudioVisualizer = false,
            mechaTheme = MechaWidgetTheme.MACHINED_MECHA,
            accentBlockStyle = AccentBlockStyle.DIAGONAL_SLAB,
            accentBlockFlipped = true,
            accentBlockRotated = true
        )
        val encoded = ModularWidgetConfig.encode(original)
        val decoded = ModularWidgetConfig.decode(encoded)

        assertEquals(original.telemetryMetric, decoded.telemetryMetric)
        assertEquals(original.tapAction, decoded.tapAction)
        assertEquals(original.toggleAction, decoded.toggleAction)
        assertEquals(original.customLabel, decoded.customLabel)
        assertEquals(original.targetPackage, decoded.targetPackage)
        assertEquals(original.matrixRows, decoded.matrixRows)
        assertEquals(original.matrixCols, decoded.matrixCols)
        assertEquals(8, decoded.matrixTiles.size)
        assertEquals(MechaWidgetTheme.MACHINED_MECHA, decoded.mechaTheme)
        assertEquals(AccentBlockStyle.DIAGONAL_SLAB, decoded.accentBlockStyle)
        assertTrue(decoded.accentBlockFlipped)
        assertTrue(decoded.accentBlockRotated)

        val emptyFallback = ModularWidgetConfig.decode("")
        assertEquals(MechaWidgetTheme.DEFAULT, emptyFallback.mechaTheme)

        val brokenFallback = ModularWidgetConfig.decode("{not-valid-json")
        assertEquals(TelemetryMetric.NET_SPEED, brokenFallback.telemetryMetric)

        assertTrue(ModularWidgetConfig.decodeTiles("invalid").isNullOrEmpty())
        assertTrue(ModularWidgetConfig.defaultControlPanelTiles().size >= 6)
    }

    @Test
    fun kansoPreferences_persistsThemeClampingWidgetsBookmarksAndWeather() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = KansoPreferences(context)

        prefs.setThemeMode(ThemeMode.LIGHT)
        prefs.setAccentColor(0xFF00E5FF)
        prefs.setIconPack(IconPackMode.KANSO_MONOCHROME)
        prefs.setDrawerViewMode(DrawerViewMode.LIST)
        prefs.setDockSlotCount(99) // Clamps to 5
        prefs.setHomeGridColumns(1) // Clamps to 4
        prefs.setWorkspacePageCount(10) // Clamps to 6
        prefs.setWidgetBevelDepth(5.0f) // Clamps to 1.8f
        prefs.setWidgetHudGlow(-2.0f) // Clamps to 0f
        prefs.setForceRecentsOverrideEnabled(true)
        prefs.setShowcaseWidgetTheme(BuiltInWidgetKey.BATTERY, MechaWidgetTheme.DATA_STREAM)
        prefs.toggleSavedDiscoverUrl("https://example.com/dispatch-1")

        val customTiles = listOf(
            ActionTileBinding(KansoActionId.WIFI, "WI-FI"),
            ActionTileBinding(KansoActionId.TORCH, "TORCH")
        )
        prefs.setControlPanelTiles(customTiles)

        val weather = CachedWeather(
            cityName = "Tokyo",
            latitude = 35.6762,
            longitude = 139.6503,
            temperatureC = 24,
            humidityPercent = 62,
            weatherCode = 1,
            updatedAtMs = 123456789L,
            isUsingAutoLocation = false
        )
        prefs.updateWeather(weather)

        val state = prefs.settingsFlow.first()
        assertEquals(ThemeMode.LIGHT, state.themeMode)
        assertEquals(0xFF00E5FF, state.accentColorArgb)
        assertFalse(state.useMaterialYouAccent)
        assertEquals(IconPackMode.KANSO_MONOCHROME, state.iconPackMode)
        assertEquals(DrawerViewMode.LIST, state.drawerViewMode)
        assertEquals(5, state.dockSlotCount)
        assertEquals(4, state.homeGridColumns)
        assertEquals(6, state.workspacePageCount)
        assertEquals(1.8f, state.widgetBevelDepth, 0.001f)
        assertEquals(0f, state.widgetHudGlow, 0.001f)
        assertTrue(state.forceRecentsOverrideEnabled)
        assertEquals(MechaWidgetTheme.DATA_STREAM, state.themeForShowcaseWidget(BuiltInWidgetKey.BATTERY))
        assertTrue("https://example.com/dispatch-1" in state.savedDiscoverUrls)
        assertEquals(2, state.controlPanelTiles.size)
        assertEquals("Tokyo", state.weather.cityName)
        assertEquals(24, state.weather.temperatureC)

        // Toggle bookmark off + reset themes
        prefs.toggleSavedDiscoverUrl("https://example.com/dispatch-1")
        prefs.resetShowcaseWidgetThemes()
        val updated = prefs.settingsFlow.first()
        assertFalse("https://example.com/dispatch-1" in updated.savedDiscoverUrls)
        assertEquals(MechaWidgetTheme.DEFAULT, updated.themeForShowcaseWidget(BuiltInWidgetKey.BATTERY))
    }
}
