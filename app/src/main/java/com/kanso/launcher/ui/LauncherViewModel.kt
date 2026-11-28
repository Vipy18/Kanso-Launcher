package com.kanso.launcher.ui

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kanso.launcher.KansoApplication
import com.kanso.launcher.data.apps.AppRepository
import com.kanso.launcher.data.apps.InstalledApp
import com.kanso.launcher.data.audio.AudioVisualizerEngine
import com.kanso.launcher.data.db.DockSlotEntity
import com.kanso.launcher.data.db.FolderAppEntity
import com.kanso.launcher.data.db.FolderEntity
import com.kanso.launcher.data.db.HiddenAppEntity
import com.kanso.launcher.data.db.HomeItemEntity
import com.kanso.launcher.data.db.HomeItemType
import com.kanso.launcher.data.db.IconOverrideEntity
import com.kanso.launcher.data.db.KansoDao
import com.kanso.launcher.data.db.SurfacePage
import com.kanso.launcher.data.iconpacks.IconPackInfo
import com.kanso.launcher.data.iconpacks.IconPackManager
import com.kanso.launcher.data.model.ActionTileBinding
import com.kanso.launcher.data.model.KansoActionId
import com.kanso.launcher.data.model.MechaWidgetTheme
import com.kanso.launcher.data.model.ModularWidgetConfig
import com.kanso.launcher.data.model.TelemetryMetric
import com.kanso.launcher.data.prefs.BuiltInWidgetKey
import com.kanso.launcher.data.prefs.DrawerViewMode
import com.kanso.launcher.data.prefs.IconPackMode
import com.kanso.launcher.data.prefs.KansoPreferences
import com.kanso.launcher.data.prefs.KansoSettingsState
import com.kanso.launcher.data.prefs.ThemeMode
import com.kanso.launcher.data.telemetry.PermissionsHealth
import com.kanso.launcher.data.telemetry.TelemetryRepository
import com.kanso.launcher.data.telemetry.TelemetryState
import com.kanso.launcher.data.weather.CityOption
import com.kanso.launcher.data.weather.WeatherRepository
import com.kanso.launcher.service.GovernorState
import com.kanso.launcher.service.KansoAccessibilityService
import com.kanso.launcher.service.KansoLifecycleService
import com.kanso.launcher.service.KansoNotificationItem
import com.kanso.launcher.service.KansoNotificationListener
import com.kanso.launcher.service.KansoPerformanceGovernor
import com.kanso.launcher.service.MediaHudState
import com.kanso.launcher.ui.lockscreen.LockscreenActivity
import com.kanso.launcher.ui.recents.RecentsActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.ceil

data class FolderWithApps(
    val folder: FolderEntity,
    val apps: List<FolderAppEntity>
)

data class ResolvedDockSlot(
    val slotIndex: Int,
    val fallbackRole: String,
    val app: InstalledApp?
)

class LauncherViewModel(
    val appRepository: AppRepository,
    private val dao: KansoDao,
    private val preferences: KansoPreferences,
    val telemetryRepository: TelemetryRepository,
    private val weatherRepository: WeatherRepository,
    val iconPackManager: IconPackManager,
    val audioVisualizerEngine: AudioVisualizerEngine
) : ViewModel() {

    val settings: StateFlow<KansoSettingsState> = preferences.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KansoSettingsState())

    val telemetry: StateFlow<TelemetryState> = telemetryRepository.telemetry
    val permissionsHealth: StateFlow<PermissionsHealth> = telemetryRepository.permissionsHealth
    val governorState: StateFlow<GovernorState> = KansoPerformanceGovernor.state
    val mediaState: StateFlow<MediaHudState> = KansoNotificationListener.mediaState
    val notificationBadges: StateFlow<Map<String, Int>> = KansoNotificationListener.badgeCounts
    val activeNotifications: StateFlow<List<KansoNotificationItem>> = KansoNotificationListener.activeNotificationItems
    val audioBands: StateFlow<List<Float>> = audioVisualizerEngine.bands

    val allInstalledApps: StateFlow<List<InstalledApp>> = appRepository.installedApps
    val recentAppPackages: StateFlow<List<String>> = appRepository.recentPackages

    val hiddenPackages: StateFlow<Set<String>> = dao.observeHiddenApps()
        .map { list -> list.map { it.packageName }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val visibleApps: StateFlow<List<InstalledApp>> = combine(allInstalledApps, hiddenPackages) { apps, hidden ->
        apps.filterNot { it.packageName in hidden }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val homeItems: StateFlow<List<HomeItemEntity>> = dao.observeHomeItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val foldersWithApps: StateFlow<Map<Long, FolderWithApps>> = combine(
        dao.observeFolders(),
        dao.observeFolderApps()
    ) { folders, apps ->
        val grouped = apps.groupBy { it.folderId }
        folders.associate { folder ->
            folder.folderId to FolderWithApps(folder, grouped[folder.folderId].orEmpty())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val iconOverrides: StateFlow<Map<String, IconOverrideEntity>> = dao.observeIconOverrides()
        .map { list -> list.associateBy { it.componentKey } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val dockSlots: StateFlow<List<ResolvedDockSlot>> = combine(
        dao.observeDockSlots(),
        allInstalledApps,
        settings
    ) { savedSlots, installed, prefs ->
        val defaultRoles = listOf("PHONE", "MESSAGES", "BROWSER", "CAMERA", "APP_5")
        val savedByIndex = savedSlots.associateBy { it.slotIndex }
        (0 until prefs.dockSlotCount).map { idx ->
            val role = defaultRoles.getOrElse(idx) { "APP_$idx" }
            val saved = savedByIndex[idx]
            val resolvedApp = when {
                saved != null && saved.packageName.isNotBlank() ->
                    installed.firstOrNull {
                        it.packageName == saved.packageName &&
                            (saved.activityName.isBlank() || it.activityName == saved.activityName)
                    } ?: installed.firstOrNull { it.packageName == saved.packageName }
                idx < 4 -> appRepository.resolveDefaultAppForRole(role)
                else -> installed.getOrNull(idx)
            }
            ResolvedDockSlot(slotIndex = idx, fallbackRole = role, app = resolvedApp)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _installedIconPacks = MutableStateFlow<List<IconPackInfo>>(emptyList())
    val installedIconPacks: StateFlow<List<IconPackInfo>> = _installedIconPacks.asStateFlow()

    private val _citySearchResults = MutableStateFlow<List<CityOption>>(emptyList())
    val citySearchResults: StateFlow<List<CityOption>> = _citySearchResults.asStateFlow()

    private val _isRefreshingWeather = MutableStateFlow(false)
    val isRefreshingWeather: StateFlow<Boolean> = _isRefreshingWeather.asStateFlow()

    init {
        refreshIconPacks()
        refreshWeather(force = false)
        seedDefaultLockscreenWidgetsIfNeeded()
    }

    private fun seedDefaultLockscreenWidgetsIfNeeded() {
        viewModelScope.launch {
            if (dao.countItemsOnPage(SurfacePage.LOCKSCREEN) == 0) {
                dao.insertHomeItem(
                    HomeItemEntity(
                        pageIndex = SurfacePage.LOCKSCREEN,
                        gridCol = 0,
                        gridRow = 0,
                        spanColumns = 2,
                        spanRows = 1,
                        orderIndex = 0,
                        itemType = HomeItemType.TOGGLE_SWITCH,
                        label = "Torch",
                        configJson = ModularWidgetConfig.encode(
                            ModularWidgetConfig(
                                mechaTheme = MechaWidgetTheme.REACTOR_CORE,
                                toggleAction = KansoActionId.TORCH,
                                customLabel = "TORCH"
                            )
                        )
                    )
                )
                dao.insertHomeItem(
                    HomeItemEntity(
                        pageIndex = SurfacePage.LOCKSCREEN,
                        gridCol = 2,
                        gridRow = 0,
                        spanColumns = 2,
                        spanRows = 1,
                        orderIndex = 1,
                        itemType = HomeItemType.TOGGLE_SWITCH,
                        label = "Sound",
                        configJson = ModularWidgetConfig.encode(
                            ModularWidgetConfig(
                                mechaTheme = MechaWidgetTheme.TACTICAL_GAUGE,
                                toggleAction = KansoActionId.SILENT_MODE,
                                customLabel = "SOUND"
                            )
                        )
                    )
                )
                dao.insertHomeItem(
                    HomeItemEntity(
                        pageIndex = SurfacePage.LOCKSCREEN,
                        gridCol = 0,
                        gridRow = 1,
                        spanColumns = 4,
                        spanRows = 1,
                        orderIndex = 2,
                        itemType = HomeItemType.MEDIA_HUD,
                        label = "Media HUD"
                    )
                )
            }
        }
    }

    fun refreshIconPacks() {
        viewModelScope.launch {
            _installedIconPacks.value = iconPackManager.discoverInstalledIconPacks()
        }
    }

    fun refreshWeather(force: Boolean = true) {
        viewModelScope.launch {
            _isRefreshingWeather.value = true
            weatherRepository.refreshWeather(force = force)
            _isRefreshingWeather.value = false
        }
    }

    fun enableAutoLocationWeather() {
        viewModelScope.launch {
            _isRefreshingWeather.value = true
            weatherRepository.enableAutoLocationAndRefresh()
            _isRefreshingWeather.value = false
        }
    }

    fun searchWeatherCities(query: String) {
        viewModelScope.launch {
            _citySearchResults.value = weatherRepository.searchCities(query)
        }
    }

    fun selectWeatherCity(option: CityOption) {
        viewModelScope.launch {
            _isRefreshingWeather.value = true
            weatherRepository.setManualCity(option)
            _citySearchResults.value = emptyList()
            _isRefreshingWeather.value = false
        }
    }

    fun refreshPermissions() {
        telemetryRepository.refreshPermissionsHealth()
        telemetryRepository.refreshRingerAndAudioState()
        audioVisualizerEngine.refreshFftAttachment()
    }

    // --- Unified Action Dispatcher for Widgets, Matrices, Toggles & Customizable Control Panel ---

    fun executeAction(
        context: Context,
        action: KansoActionId,
        targetPackage: String = "",
        targetActivity: String = "",
        onOpenSettings: () -> Unit = {}
    ) {
        when (action) {
            KansoActionId.WIFI -> telemetryRepository.openInternetPanel()
            KansoActionId.MOBILE_DATA -> telemetryRepository.openMobileDataPanel()
            KansoActionId.BLUETOOTH -> telemetryRepository.openBluetoothControls()
            KansoActionId.HOTSPOT -> telemetryRepository.toggleHotspot()
            KansoActionId.AIRPLANE -> telemetryRepository.openAirplaneSettings()
            KansoActionId.DND -> telemetryRepository.toggleDnd()
            KansoActionId.TORCH -> telemetryRepository.toggleTorch()
            KansoActionId.AUTO_ROTATE -> telemetryRepository.toggleAutoRotate()
            KansoActionId.POWER_SAVING -> telemetryRepository.togglePowerSaving()
            KansoActionId.SILENT_MODE -> telemetryRepository.cycleVolumeOrShowHud()
            KansoActionId.MEDIA_PLAY_PAUSE -> KansoNotificationListener.togglePlayPause(context)
            KansoActionId.LOCK_SCREEN -> {
                val lockedByService = KansoAccessibilityService.performLockScreen()
                if (settings.value.lockscreenCompanionEnabled || !lockedByService) {
                    val intent = Intent(context, LockscreenActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_NO_ANIMATION
                        )
                    }
                    runCatching { context.startActivity(intent) }
                }
            }
            KansoActionId.RECENTS -> {
                val shouldOpenKansoRecents = settings.value.forceRecentsOverrideEnabled ||
                    !KansoAccessibilityService.performSystemRecents()
                if (shouldOpenKansoRecents) {
                    KansoAccessibilityService.notifyRecentsOpened()
                    val intent = Intent(context, RecentsActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_NO_ANIMATION
                        )
                    }
                    runCatching { context.startActivity(intent) }
                }
            }
            KansoActionId.SCREENSHOT -> {
                if (!KansoAccessibilityService.performTakeScreenshot()) {
                    telemetryRepository.openSystemSettings()
                }
            }
            KansoActionId.VOLUME_PANEL -> telemetryRepository.openVolumePanel()
            KansoActionId.BATTERY_SETTINGS -> telemetryRepository.openBatterySettings()
            KansoActionId.LOCATION -> telemetryRepository.openLocationSettings()
            KansoActionId.NFC -> telemetryRepository.openNfcSettings()
            KansoActionId.CAST -> telemetryRepository.openCastSettings()
            KansoActionId.DARK_MODE -> {
                val next = if (settings.value.themeMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
                setThemeMode(next)
            }
            KansoActionId.DISPLAY_SETTINGS -> telemetryRepository.openDisplaySettings()
            KansoActionId.SYSTEM_SETTINGS -> telemetryRepository.openSystemSettings()
            KansoActionId.OPEN_CLOCK -> appRepository.launchRoleFallback("CLOCK")
            KansoActionId.OPEN_CALENDAR -> appRepository.launchRoleFallback("CALENDAR")
            KansoActionId.OPEN_SETTINGS -> onOpenSettings()
            KansoActionId.LAUNCH_APP -> {
                if (targetPackage.isNotBlank()) {
                    appRepository.launchApp(targetPackage, targetActivity)
                }
            }
        }
    }

    // --- Customizable Control Panel Mutations ---

    fun addControlPanelTile(tile: ActionTileBinding) {
        viewModelScope.launch {
            val next = settings.value.controlPanelTiles + tile
            preferences.setControlPanelTiles(next)
        }
    }

    fun updateControlPanelTile(index: Int, tile: ActionTileBinding) {
        viewModelScope.launch {
            val current = settings.value.controlPanelTiles.toMutableList()
            if (index in current.indices) {
                current[index] = tile
                preferences.setControlPanelTiles(current)
            }
        }
    }

    fun removeControlPanelTile(index: Int) {
        viewModelScope.launch {
            val current = settings.value.controlPanelTiles.toMutableList()
            if (index in current.indices && current.size > 1) {
                current.removeAt(index)
                preferences.setControlPanelTiles(current)
            }
        }
    }

    fun resetControlPanelTiles() {
        viewModelScope.launch {
            preferences.setControlPanelTiles(ModularWidgetConfig.defaultControlPanelTiles())
        }
    }

    fun setRingerMode(mode: Int) {
        telemetryRepository.setRingerMode(mode)
    }

    fun openAudioOutputSettings() {
        telemetryRepository.openAudioOutputSettings()
    }

    fun toggleDiscoverBookmark(url: String) {
        viewModelScope.launch { preferences.toggleSavedDiscoverUrl(url) }
    }

    fun setWidgetBevelDepth(depth: Float) {
        viewModelScope.launch { preferences.setWidgetBevelDepth(depth) }
    }

    fun setWidgetHudGlow(glow: Float) {
        viewModelScope.launch { preferences.setWidgetHudGlow(glow) }
    }

    fun applyThemeToAllWorkspaceWidgets(theme: MechaWidgetTheme) {
        viewModelScope.launch {
            val modularTypes = setOf(
                HomeItemType.TELEMETRY_CAPSULE,
                HomeItemType.ACTION_MATRIX,
                HomeItemType.TOGGLE_SWITCH,
                HomeItemType.BATTERY_CAPSULE,
                HomeItemType.MEDIA_HUD,
                HomeItemType.WEATHER_BADGE,
                HomeItemType.CALENDAR_RIBBON,
                HomeItemType.QUICK_TOGGLE_CLUSTER,
                HomeItemType.TRI_STATE_SWITCH,
                HomeItemType.DUAL_SLIDER_CAPSULE,
                HomeItemType.STITCH_CLOCK
            )
            homeItems.value
                .filter { it.itemType in modularTypes }
                .forEach { item ->
                    val decoded = ModularWidgetConfig.decode(item.configJson)
                    val updated = decoded.copy(mechaTheme = theme)
                    dao.updateHomeItemConfig(item.id, ModularWidgetConfig.encode(updated), item.label)
                }
        }
    }

    // --- Settings mutations ---

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setAccentColor(argb: Long) {
        viewModelScope.launch {
            preferences.setAccentColor(argb)
            iconPackManager.clearBitmapCache()
        }
    }

    fun setUseMaterialYouAccent(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setUseMaterialYouAccent(enabled)
            iconPackManager.clearBitmapCache()
        }
    }

    fun setUseWallpaperOverlay(enabled: Boolean) {
        viewModelScope.launch { preferences.setUseWallpaperOverlay(enabled) }
    }

    fun setWallpaperTransparencyPercent(percent: Int) {
        viewModelScope.launch { preferences.setWallpaperTransparencyPercent(percent) }
    }

    fun setIconPack(mode: IconPackMode, packageName: String = "") {
        viewModelScope.launch {
            preferences.setIconPack(mode, packageName)
            iconPackManager.clearBitmapCache()
        }
    }

    fun setShowHomeSearchBar(show: Boolean) {
        viewModelScope.launch { preferences.setShowHomeSearchBar(show) }
    }

    fun setDockSlotCount(count: Int) {
        viewModelScope.launch { preferences.setDockSlotCount(count) }
    }

    fun setHomeGridColumns(cols: Int) {
        viewModelScope.launch { preferences.setHomeGridColumns(cols) }
    }

    fun toggleDrawerViewMode() {
        viewModelScope.launch {
            val next = if (settings.value.drawerViewMode == DrawerViewMode.GRID) {
                DrawerViewMode.LIST
            } else {
                DrawerViewMode.GRID
            }
            preferences.setDrawerViewMode(next)
        }
    }

    fun setUse24HourClock(use24h: Boolean) {
        viewModelScope.launch { preferences.setUse24HourClock(use24h) }
    }

    fun setLockscreenCompanionEnabled(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            preferences.setLockscreenCompanionEnabled(enabled)
            if (enabled || settings.value.performanceGovernorEnabled) {
                KansoLifecycleService.start(context)
            }
        }
    }

    fun setPerformanceGovernorEnabled(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            preferences.setPerformanceGovernorEnabled(enabled)
            KansoPerformanceGovernor.setEnabled(context, enabled)
            if (enabled) {
                KansoLifecycleService.start(context)
            } else if (!settings.value.lockscreenCompanionEnabled) {
                KansoLifecycleService.stop(context)
            }
        }
    }

    fun setForceRecentsOverrideEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setForceRecentsOverrideEnabled(enabled) }
    }

    fun setWorkspacePageCount(count: Int) {
        viewModelScope.launch { preferences.setWorkspacePageCount(count) }
    }

    fun addWorkspacePage(): Int {
        val current = settings.value.workspacePageCount
        val nextCount = (current + 1).coerceAtMost(6)
        if (nextCount != current) {
            viewModelScope.launch { preferences.setWorkspacePageCount(nextCount) }
        }
        return 1 + nextCount
    }

    fun removeWorkspacePage(workspaceSlot: Int) {
        val currentCount = settings.value.workspacePageCount
        if (currentCount <= 1) return
        val slot = workspaceSlot.coerceIn(0, currentCount - 1)
        viewModelScope.launch {
            val removedPageIndex = SurfacePage.workspacePageIndex(slot)
            val removedItems = dao.getItemsOnPage(removedPageIndex)
            removedItems.forEach { item ->
                if (item.itemType == HomeItemType.FOLDER && item.folderId != null) {
                    dao.deleteFolderAndHomeItem(item.folderId, item.id)
                } else {
                    dao.deleteHomeItem(item.id)
                }
            }
            for (s in (slot + 1) until currentCount) {
                val fromPage = SurfacePage.workspacePageIndex(s)
                val toPage = SurfacePage.workspacePageIndex(s - 1)
                dao.getItemsOnPage(fromPage).forEach { item ->
                    dao.moveHomeItem(item.id, toPage, item.orderIndex)
                }
            }
            preferences.setWorkspacePageCount(currentCount - 1)
        }
    }

    fun setPage1TelemetryMetric(metric: TelemetryMetric) {
        viewModelScope.launch { preferences.setPage1TelemetryMetric(metric) }
    }

    fun setPage1TelemetryAction(action: KansoActionId) {
        viewModelScope.launch { preferences.setPage1TelemetryAction(action) }
    }

    fun setPage1VolumeToggleAction(action: KansoActionId) {
        viewModelScope.launch { preferences.setPage1VolumeToggleAction(action) }
    }

    fun setPage1BatteryAction(action: KansoActionId) {
        viewModelScope.launch { preferences.setPage1BatteryAction(action) }
    }

    fun setShowcaseWidgetTheme(widgetKey: String, theme: MechaWidgetTheme) {
        viewModelScope.launch { preferences.setShowcaseWidgetTheme(widgetKey, theme) }
    }

    fun togglePage1Widget(widgetKey: String) {
        viewModelScope.launch {
            val current = settings.value.page1Widgets.toMutableList()
            if (widgetKey in current) current.remove(widgetKey) else current.add(widgetKey)
            preferences.setPage1Widgets(current)
        }
    }

    fun resetPage1Layout() {
        viewModelScope.launch {
            preferences.setPage1Widgets(BuiltInWidgetKey.DEFAULT_ORDER)
            preferences.setPage1TelemetryMetric(TelemetryMetric.NET_SPEED)
            preferences.setPage1TelemetryAction(KansoActionId.WIFI)
            preferences.setPage1VolumeToggleAction(KansoActionId.SILENT_MODE)
            preferences.setPage1BatteryAction(KansoActionId.BATTERY_SETTINGS)
            preferences.resetShowcaseWidgetThemes()
        }
    }

    // --- Drag-and-Drop Grid, Dynamic Actual-Size Widgets, Modular Widgets, Folders, Dock ---

    /**
     * Resolves actual dynamic cell spans from an Android AppWidgetProviderInfo
     * (using targetCellWidth/targetCellHeight on Android 12+ and dp-to-cell formula on older APIs).
     */
    fun computeDynamicExternalWidgetSpan(
        info: AppWidgetProviderInfo?,
        gridCols: Int = settings.value.homeGridColumns
    ): Pair<Int, Int> {
        if (info == null) return 2 to 2
        val targetCols = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.targetCellWidth > 0) {
            info.targetCellWidth
        } else {
            // Standard Android launcher cell formula: cells = ceil((minWidth + 30) / 70)
            ceil((info.minWidth.coerceAtLeast(40) + 30f) / 70f).toInt()
        }
        val targetRows = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.targetCellHeight > 0) {
            info.targetCellHeight
        } else {
            ceil((info.minHeight.coerceAtLeast(40) + 30f) / 70f).toInt()
        }
        return targetCols.coerceIn(1, gridCols) to targetRows.coerceIn(1, 5)
    }

    private suspend fun findFirstFreeCell(
        pageIndex: Int,
        spanCols: Int,
        spanRows: Int,
        gridCols: Int,
        isAccentBlock: Boolean = false
    ): Pair<Int, Int> {
        val existing = dao.getItemsOnPage(pageIndex).filter {
            (it.itemType == HomeItemType.ACCENT_BLOCK) == isAccentBlock
        }
        for (row in 0 until 14) {
            for (col in 0..(gridCols - spanCols).coerceAtLeast(0)) {
                val overlaps = existing.any { item ->
                    val colOverlap = col < item.gridCol + item.spanColumns && col + spanCols > item.gridCol
                    val rowOverlap = row < item.gridRow + item.spanRows && row + spanRows > item.gridRow
                    colOverlap && rowOverlap
                }
                if (!overlaps) return col to row
            }
        }
        return 0 to (existing.maxOfOrNull { it.gridRow + it.spanRows } ?: 0)
    }

    fun addAppToHome(app: InstalledApp, pageIndex: Int = SurfacePage.WORKSPACE) {
        viewModelScope.launch {
            val cols = settings.value.homeGridColumns
            val (col, row) = findFirstFreeCell(pageIndex, 1, 1, cols)
            val order = dao.countItemsOnPage(pageIndex)
            dao.insertHomeItem(
                HomeItemEntity(
                    pageIndex = pageIndex,
                    gridCol = col,
                    gridRow = row,
                    spanColumns = 1,
                    spanRows = 1,
                    orderIndex = order,
                    itemType = HomeItemType.APP,
                    packageName = app.packageName,
                    activityName = app.activityName,
                    label = app.label
                )
            )
        }
    }

    fun addModularWidget(
        itemType: String,
        label: String,
        spanCols: Int,
        spanRows: Int,
        config: ModularWidgetConfig = ModularWidgetConfig(),
        pageIndex: Int = SurfacePage.WORKSPACE
    ) {
        viewModelScope.launch {
            val cols = settings.value.homeGridColumns
            val clampedSpanCols = spanCols.coerceIn(1, cols)
            val clampedSpanRows = spanRows.coerceIn(1, 5)
            val isAccentBlock = itemType == HomeItemType.ACCENT_BLOCK
            val (col, row) = findFirstFreeCell(pageIndex, clampedSpanCols, clampedSpanRows, cols, isAccentBlock)
            val order = dao.countItemsOnPage(pageIndex)
            dao.insertHomeItem(
                HomeItemEntity(
                    pageIndex = pageIndex,
                    gridCol = col,
                    gridRow = row,
                    spanColumns = clampedSpanCols,
                    spanRows = clampedSpanRows,
                    orderIndex = order,
                    itemType = itemType,
                    label = label,
                    configJson = ModularWidgetConfig.encode(config)
                )
            )
        }
    }

    fun addExternalWidgetToHome(
        appWidgetId: Int,
        label: String,
        info: AppWidgetProviderInfo? = null,
        pageIndex: Int = SurfacePage.WORKSPACE
    ) {
        viewModelScope.launch {
            val cols = settings.value.homeGridColumns
            val (spanCols, spanRows) = computeDynamicExternalWidgetSpan(info, cols)
            val (col, row) = findFirstFreeCell(pageIndex, spanCols, spanRows, cols)
            val order = dao.countItemsOnPage(pageIndex)
            dao.insertHomeItem(
                HomeItemEntity(
                    pageIndex = pageIndex,
                    gridCol = col,
                    gridRow = row,
                    spanColumns = spanCols,
                    spanRows = spanRows,
                    orderIndex = order,
                    itemType = HomeItemType.EXTERNAL_WIDGET,
                    label = label,
                    appWidgetId = appWidgetId
                )
            )
        }
    }

    fun moveHomeItemToGridCell(item: HomeItemEntity, targetCol: Int, targetRow: Int, maxCols: Int) {
        viewModelScope.launch {
            val safeCol = targetCol.coerceIn(0, (maxCols - item.spanColumns).coerceAtLeast(0))
            val safeRow = targetRow.coerceIn(0, 12)
            val isAccentBlock = item.itemType == HomeItemType.ACCENT_BLOCK
            val siblings = dao.getItemsOnPage(item.pageIndex).filter {
                it.id != item.id && (it.itemType == HomeItemType.ACCENT_BLOCK) == isAccentBlock
            }
            val colliding = siblings.filter { other ->
                val colOverlap = safeCol < other.gridCol + other.spanColumns && safeCol + item.spanColumns > other.gridCol
                val rowOverlap = safeRow < other.gridRow + other.spanRows && safeRow + item.spanRows > other.gridRow
                colOverlap && rowOverlap
            }
            if (colliding.isEmpty()) {
                dao.updateHomeItemPosition(item.id, safeCol, safeRow)
            } else if (colliding.size == 1 &&
                colliding[0].spanColumns == item.spanColumns &&
                colliding[0].spanRows == item.spanRows
            ) {
                dao.updateHomeItemPosition(colliding[0].id, item.gridCol, item.gridRow)
                dao.updateHomeItemPosition(item.id, safeCol, safeRow)
            }
        }
    }

    fun resizeHomeItem(item: HomeItemEntity, newSpanCols: Int, newSpanRows: Int, maxCols: Int) {
        viewModelScope.launch {
            val cols = newSpanCols.coerceIn(1, maxCols)
            val rows = newSpanRows.coerceIn(1, 5)
            val clampedCol = item.gridCol.coerceIn(0, (maxCols - cols).coerceAtLeast(0))
            dao.updateHomeItemPosition(item.id, clampedCol, item.gridRow)
            dao.updateHomeItemSpan(item.id, cols, rows)
        }
    }

    fun updateHomeItemConfig(item: HomeItemEntity, config: ModularWidgetConfig, newLabel: String = item.label) {
        viewModelScope.launch {
            dao.updateHomeItemConfig(item.id, ModularWidgetConfig.encode(config), newLabel)
        }
    }

    fun removeHomeItem(item: HomeItemEntity) {
        viewModelScope.launch {
            if (item.itemType == HomeItemType.FOLDER && item.folderId != null) {
                dao.deleteFolderAndHomeItem(item.folderId, item.id)
            } else {
                dao.deleteHomeItem(item.id)
            }
        }
    }

    fun createCircularFolder(title: String, apps: List<InstalledApp>, pageIndex: Int = SurfacePage.WORKSPACE) {
        if (apps.isEmpty()) return
        viewModelScope.launch {
            val cols = settings.value.homeGridColumns
            val (col, row) = findFirstFreeCell(pageIndex, 1, 1, cols)
            dao.createFolderWithApps(
                title = title.ifBlank { "Folder" },
                pageIndex = pageIndex,
                gridCol = col,
                gridRow = row,
                apps = apps.map { it.packageName to it.activityName }
            )
        }
    }

    fun renameFolder(folderId: Long, newTitle: String) {
        viewModelScope.launch {
            dao.renameFolder(folderId, newTitle.ifBlank { "Folder" })
        }
    }

    fun removeAppFromFolder(folderId: Long, packageName: String, activityName: String) {
        viewModelScope.launch {
            dao.removeFolderApp(folderId, packageName, activityName)
        }
    }

    fun pinAppToDock(app: InstalledApp, slotIndex: Int) {
        viewModelScope.launch {
            val defaultRoles = listOf("PHONE", "MESSAGES", "BROWSER", "CAMERA", "APP_5")
            dao.upsertDockSlot(
                DockSlotEntity(
                    slotIndex = slotIndex,
                    packageName = app.packageName,
                    activityName = app.activityName,
                    fallbackRole = defaultRoles.getOrElse(slotIndex) { "APP_$slotIndex" }
                )
            )
        }
    }

    fun resetDockSlot(slotIndex: Int) {
        viewModelScope.launch { dao.deleteDockSlot(slotIndex) }
    }

    fun hideApp(packageName: String) {
        viewModelScope.launch { dao.hideApp(HiddenAppEntity(packageName)) }
    }

    fun unhideApp(packageName: String) {
        viewModelScope.launch { dao.unhideApp(packageName) }
    }

    fun setPerAppIconOverride(componentKey: String, packPackage: String, drawableName: String) {
        viewModelScope.launch {
            dao.upsertIconOverride(
                IconOverrideEntity(
                    componentKey = componentKey,
                    iconPackPackage = packPackage,
                    drawableName = drawableName
                )
            )
            iconPackManager.clearBitmapCache()
        }
    }

    fun clearPerAppIconOverride(componentKey: String) {
        viewModelScope.launch {
            dao.deleteIconOverride(componentKey)
            iconPackManager.clearBitmapCache()
        }
    }

    class Factory(private val application: KansoApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LauncherViewModel(
                appRepository = application.appRepository,
                dao = application.database.kansoDao(),
                preferences = application.preferences,
                telemetryRepository = application.telemetryRepository,
                weatherRepository = application.weatherRepository,
                iconPackManager = application.iconPackManager,
                audioVisualizerEngine = application.audioVisualizerEngine
            ) as T
        }
    }
}
