package com.kanso.launcher.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kanso.launcher.data.model.ActionTileBinding
import com.kanso.launcher.data.model.KansoActionId
import com.kanso.launcher.data.model.MechaWidgetTheme
import com.kanso.launcher.data.model.ModularWidgetConfig
import com.kanso.launcher.data.model.TelemetryMetric
import com.kanso.launcher.ui.theme.KansoAccents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.kansoDataStore: DataStore<Preferences> by preferencesDataStore(name = "kanso_prefs")

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

enum class IconPackMode {
    KANSO_ACCENT,
    KANSO_MONOCHROME,
    SYSTEM_DEFAULT,
    THIRD_PARTY
}

enum class DrawerViewMode {
    GRID,
    LIST
}

object BuiltInWidgetKey {
    const val VOLUME_SWITCH = "VOLUME_SWITCH"
    const val BATTERY = "BATTERY"
    const val NETWORK = "NETWORK"
    const val QUICK_TOGGLES = "QUICK_TOGGLES"
    const val MEDIA_HUD = "MEDIA_HUD"
    const val WEATHER = "WEATHER"
    const val CALENDAR = "CALENDAR"

    val DEFAULT_ORDER: List<String> = listOf(
        VOLUME_SWITCH,
        BATTERY,
        NETWORK,
        QUICK_TOGGLES,
        MEDIA_HUD,
        WEATHER,
        CALENDAR
    )
}

data class CachedWeather(
    val cityName: String = "San Francisco",
    val latitude: Double = 37.7749,
    val longitude: Double = -122.4194,
    val temperatureC: Int = 20,
    val humidityPercent: Int = 86,
    val weatherCode: Int = 61,
    val updatedAtMs: Long = 0L,
    val isUsingAutoLocation: Boolean = true
)

data class KansoSettingsState(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentColorArgb: Long = KansoAccents.DEFAULT_ACCENT_ARGB,
    val useMaterialYouAccent: Boolean = false,
    val useWallpaperOverlay: Boolean = false,
    val wallpaperTransparencyPercent: Int = 28,
    val iconPackMode: IconPackMode = IconPackMode.KANSO_ACCENT,
    val selectedIconPackPackage: String = "",
    val showHomeSearchBar: Boolean = false,
    val dockSlotCount: Int = 4,
    val homeGridColumns: Int = 4,
    val workspacePageCount: Int = 1,
    val drawerViewMode: DrawerViewMode = DrawerViewMode.GRID,
    val use24HourClock: Boolean = true,
    val lockscreenCompanionEnabled: Boolean = false,
    val forceRecentsOverrideEnabled: Boolean = false,
    val performanceGovernorEnabled: Boolean = true,
    val page1Widgets: List<String> = BuiltInWidgetKey.DEFAULT_ORDER,
    val page1TelemetryMetric: TelemetryMetric = TelemetryMetric.NET_SPEED,
    val page1TelemetryAction: KansoActionId = KansoActionId.WIFI,
    val page1VolumeToggleAction: KansoActionId = KansoActionId.SILENT_MODE,
    val page1BatteryAction: KansoActionId = KansoActionId.BATTERY_SETTINGS,
    val page1WidgetThemes: Map<String, MechaWidgetTheme> = emptyMap(),
    val controlPanelTiles: List<ActionTileBinding> = ModularWidgetConfig.defaultControlPanelTiles(),
    val weather: CachedWeather = CachedWeather()
) {
    fun themeForShowcaseWidget(key: String): MechaWidgetTheme =
        page1WidgetThemes[key] ?: MechaWidgetTheme.DEFAULT
}

class KansoPreferences(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = longPreferencesKey("accent_color_argb")
        val USE_MATERIAL_YOU = booleanPreferencesKey("use_material_you_accent")
        val USE_WALLPAPER = booleanPreferencesKey("use_wallpaper_overlay")
        val WALLPAPER_TRANSPARENCY = intPreferencesKey("wallpaper_transparency_percent")
        val ICON_PACK_MODE = stringPreferencesKey("icon_pack_mode")
        val ICON_PACK_PKG = stringPreferencesKey("selected_icon_pack_pkg")
        val SHOW_HOME_SEARCH = booleanPreferencesKey("show_home_search_bar")
        val DOCK_SLOT_COUNT = intPreferencesKey("dock_slot_count")
        val HOME_GRID_COLUMNS = intPreferencesKey("home_grid_columns")
        val WORKSPACE_PAGE_COUNT = intPreferencesKey("workspace_page_count")
        val DRAWER_VIEW_MODE = stringPreferencesKey("drawer_view_mode")
        val USE_24H_CLOCK = booleanPreferencesKey("use_24h_clock")
        val LOCKSCREEN_ENABLED = booleanPreferencesKey("lockscreen_companion_enabled")
        val FORCE_RECENTS_OVERRIDE = booleanPreferencesKey("force_recents_override_enabled")
        val GOVERNOR_ENABLED = booleanPreferencesKey("performance_governor_enabled")
        val PAGE1_WIDGETS = stringPreferencesKey("page1_widgets_csv")
        val PAGE1_TELEMETRY_METRIC = stringPreferencesKey("page1_telemetry_metric")
        val PAGE1_TELEMETRY_ACTION = stringPreferencesKey("page1_telemetry_action")
        val PAGE1_VOLUME_ACTION = stringPreferencesKey("page1_volume_action")
        val PAGE1_BATTERY_ACTION = stringPreferencesKey("page1_battery_action")
        val PAGE1_WIDGET_THEMES = stringPreferencesKey("page1_widget_themes_csv")
        val CONTROL_PANEL_TILES = stringPreferencesKey("control_panel_tiles_json")

        val WEATHER_CITY = stringPreferencesKey("weather_city")
        val WEATHER_LAT = doublePreferencesKey("weather_lat")
        val WEATHER_LON = doublePreferencesKey("weather_lon")
        val WEATHER_TEMP = intPreferencesKey("weather_temp_c")
        val WEATHER_HUMIDITY = intPreferencesKey("weather_humidity")
        val WEATHER_CODE = intPreferencesKey("weather_code")
        val WEATHER_UPDATED = longPreferencesKey("weather_updated_ms")
        val WEATHER_AUTO_LOC = booleanPreferencesKey("weather_auto_location")
    }

    val settingsFlow: Flow<KansoSettingsState> = context.kansoDataStore.data.map { prefs ->
        val savedWidgets = prefs[Keys.PAGE1_WIDGETS]
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: BuiltInWidgetKey.DEFAULT_ORDER

        val themesMap = buildMap {
            prefs[Keys.PAGE1_WIDGET_THEMES]?.split(",")?.forEach { entry ->
                val parts = entry.split("=")
                if (parts.size == 2) {
                    val theme = runCatching { MechaWidgetTheme.valueOf(parts[1]) }.getOrNull()
                    if (theme != null) put(parts[0], theme)
                }
            }
        }

        val controlTiles = prefs[Keys.CONTROL_PANEL_TILES]
            ?.let { ModularWidgetConfig.decodeTiles(it) }
            ?.takeIf { it.isNotEmpty() }
            ?: ModularWidgetConfig.defaultControlPanelTiles()

        KansoSettingsState(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.DARK,
            accentColorArgb = prefs[Keys.ACCENT_COLOR] ?: KansoAccents.DEFAULT_ACCENT_ARGB,
            useMaterialYouAccent = prefs[Keys.USE_MATERIAL_YOU] ?: false,
            useWallpaperOverlay = prefs[Keys.USE_WALLPAPER] ?: false,
            wallpaperTransparencyPercent = (prefs[Keys.WALLPAPER_TRANSPARENCY] ?: 28).coerceIn(0, 100),
            iconPackMode = prefs[Keys.ICON_PACK_MODE]?.let { runCatching { IconPackMode.valueOf(it) }.getOrNull() }
                ?: IconPackMode.KANSO_ACCENT,
            selectedIconPackPackage = prefs[Keys.ICON_PACK_PKG] ?: "",
            showHomeSearchBar = prefs[Keys.SHOW_HOME_SEARCH] ?: false,
            dockSlotCount = (prefs[Keys.DOCK_SLOT_COUNT] ?: 4).coerceIn(4, 5),
            homeGridColumns = (prefs[Keys.HOME_GRID_COLUMNS] ?: 4).coerceIn(4, 5),
            workspacePageCount = (prefs[Keys.WORKSPACE_PAGE_COUNT] ?: 1).coerceIn(1, 6),
            drawerViewMode = prefs[Keys.DRAWER_VIEW_MODE]?.let { runCatching { DrawerViewMode.valueOf(it) }.getOrNull() }
                ?: DrawerViewMode.GRID,
            use24HourClock = prefs[Keys.USE_24H_CLOCK] ?: true,
            lockscreenCompanionEnabled = prefs[Keys.LOCKSCREEN_ENABLED] ?: false,
            forceRecentsOverrideEnabled = prefs[Keys.FORCE_RECENTS_OVERRIDE] ?: false,
            performanceGovernorEnabled = prefs[Keys.GOVERNOR_ENABLED] ?: true,
            page1Widgets = savedWidgets,
            page1TelemetryMetric = prefs[Keys.PAGE1_TELEMETRY_METRIC]
                ?.let { runCatching { TelemetryMetric.valueOf(it) }.getOrNull() }
                ?: TelemetryMetric.NET_SPEED,
            page1TelemetryAction = prefs[Keys.PAGE1_TELEMETRY_ACTION]
                ?.let { runCatching { KansoActionId.valueOf(it) }.getOrNull() }
                ?: KansoActionId.WIFI,
            page1VolumeToggleAction = prefs[Keys.PAGE1_VOLUME_ACTION]
                ?.let { runCatching { KansoActionId.valueOf(it) }.getOrNull() }
                ?: KansoActionId.SILENT_MODE,
            page1BatteryAction = prefs[Keys.PAGE1_BATTERY_ACTION]
                ?.let { runCatching { KansoActionId.valueOf(it) }.getOrNull() }
                ?: KansoActionId.BATTERY_SETTINGS,
            page1WidgetThemes = themesMap,
            controlPanelTiles = controlTiles,
            weather = CachedWeather(
                cityName = prefs[Keys.WEATHER_CITY] ?: "San Francisco",
                latitude = prefs[Keys.WEATHER_LAT] ?: 37.7749,
                longitude = prefs[Keys.WEATHER_LON] ?: -122.4194,
                temperatureC = prefs[Keys.WEATHER_TEMP] ?: 20,
                humidityPercent = prefs[Keys.WEATHER_HUMIDITY] ?: 86,
                weatherCode = prefs[Keys.WEATHER_CODE] ?: 61,
                updatedAtMs = prefs[Keys.WEATHER_UPDATED] ?: 0L,
                isUsingAutoLocation = prefs[Keys.WEATHER_AUTO_LOC] ?: true
            )
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.kansoDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAccentColor(argb: Long) {
        context.kansoDataStore.edit {
            it[Keys.ACCENT_COLOR] = argb
            it[Keys.USE_MATERIAL_YOU] = false
        }
    }

    suspend fun setUseMaterialYouAccent(enabled: Boolean) {
        context.kansoDataStore.edit { it[Keys.USE_MATERIAL_YOU] = enabled }
    }

    suspend fun setUseWallpaperOverlay(enabled: Boolean) {
        context.kansoDataStore.edit { it[Keys.USE_WALLPAPER] = enabled }
    }

    suspend fun setWallpaperTransparencyPercent(percent: Int) {
        context.kansoDataStore.edit { it[Keys.WALLPAPER_TRANSPARENCY] = percent.coerceIn(0, 100) }
    }

    suspend fun setIconPack(mode: IconPackMode, packageName: String = "") {
        context.kansoDataStore.edit {
            it[Keys.ICON_PACK_MODE] = mode.name
            if (packageName.isNotEmpty() || mode != IconPackMode.THIRD_PARTY) {
                it[Keys.ICON_PACK_PKG] = packageName
            }
        }
    }

    suspend fun setShowHomeSearchBar(show: Boolean) {
        context.kansoDataStore.edit { it[Keys.SHOW_HOME_SEARCH] = show }
    }

    suspend fun setDockSlotCount(count: Int) {
        context.kansoDataStore.edit { it[Keys.DOCK_SLOT_COUNT] = count.coerceIn(4, 5) }
    }

    suspend fun setHomeGridColumns(cols: Int) {
        context.kansoDataStore.edit { it[Keys.HOME_GRID_COLUMNS] = cols.coerceIn(4, 5) }
    }

    suspend fun setWorkspacePageCount(count: Int) {
        context.kansoDataStore.edit { it[Keys.WORKSPACE_PAGE_COUNT] = count.coerceIn(1, 6) }
    }

    suspend fun setDrawerViewMode(mode: DrawerViewMode) {
        context.kansoDataStore.edit { it[Keys.DRAWER_VIEW_MODE] = mode.name }
    }

    suspend fun setUse24HourClock(use24h: Boolean) {
        context.kansoDataStore.edit { it[Keys.USE_24H_CLOCK] = use24h }
    }

    suspend fun setLockscreenCompanionEnabled(enabled: Boolean) {
        context.kansoDataStore.edit { it[Keys.LOCKSCREEN_ENABLED] = enabled }
    }

    suspend fun setForceRecentsOverrideEnabled(enabled: Boolean) {
        context.kansoDataStore.edit { it[Keys.FORCE_RECENTS_OVERRIDE] = enabled }
    }

    suspend fun setPerformanceGovernorEnabled(enabled: Boolean) {
        context.kansoDataStore.edit { it[Keys.GOVERNOR_ENABLED] = enabled }
    }

    suspend fun setPage1Widgets(widgets: List<String>) {
        context.kansoDataStore.edit { it[Keys.PAGE1_WIDGETS] = widgets.joinToString(",") }
    }

    suspend fun setPage1TelemetryMetric(metric: TelemetryMetric) {
        context.kansoDataStore.edit { it[Keys.PAGE1_TELEMETRY_METRIC] = metric.name }
    }

    suspend fun setPage1TelemetryAction(action: KansoActionId) {
        context.kansoDataStore.edit { it[Keys.PAGE1_TELEMETRY_ACTION] = action.name }
    }

    suspend fun setPage1VolumeToggleAction(action: KansoActionId) {
        context.kansoDataStore.edit { it[Keys.PAGE1_VOLUME_ACTION] = action.name }
    }

    suspend fun setPage1BatteryAction(action: KansoActionId) {
        context.kansoDataStore.edit { it[Keys.PAGE1_BATTERY_ACTION] = action.name }
    }

    suspend fun setShowcaseWidgetTheme(widgetKey: String, theme: MechaWidgetTheme) {
        context.kansoDataStore.edit { prefs ->
            val current = mutableMapOf<String, MechaWidgetTheme>()
            prefs[Keys.PAGE1_WIDGET_THEMES]?.split(",")?.forEach { entry ->
                val parts = entry.split("=")
                if (parts.size == 2) {
                    runCatching { MechaWidgetTheme.valueOf(parts[1]) }.getOrNull()?.let {
                        current[parts[0]] = it
                    }
                }
            }
            current[widgetKey] = theme
            prefs[Keys.PAGE1_WIDGET_THEMES] = current.entries.joinToString(",") { "${it.key}=${it.value.name}" }
        }
    }

    suspend fun resetShowcaseWidgetThemes() {
        context.kansoDataStore.edit { it.remove(Keys.PAGE1_WIDGET_THEMES) }
    }

    suspend fun setControlPanelTiles(tiles: List<ActionTileBinding>) {
        context.kansoDataStore.edit { it[Keys.CONTROL_PANEL_TILES] = ModularWidgetConfig.encodeTiles(tiles) }
    }

    suspend fun setUseAutoLocationWeather(enabled: Boolean) {
        context.kansoDataStore.edit { it[Keys.WEATHER_AUTO_LOC] = enabled }
    }

    suspend fun updateWeather(weather: CachedWeather) {
        context.kansoDataStore.edit {
            it[Keys.WEATHER_CITY] = weather.cityName
            it[Keys.WEATHER_LAT] = weather.latitude
            it[Keys.WEATHER_LON] = weather.longitude
            it[Keys.WEATHER_TEMP] = weather.temperatureC
            it[Keys.WEATHER_HUMIDITY] = weather.humidityPercent
            it[Keys.WEATHER_CODE] = weather.weatherCode
            it[Keys.WEATHER_UPDATED] = weather.updatedAtMs
            it[Keys.WEATHER_AUTO_LOC] = weather.isUsingAutoLocation
        }
    }
}
