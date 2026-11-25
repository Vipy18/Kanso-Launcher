package com.kanso.launcher.ui.home

import android.Manifest
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanso.launcher.R
import com.kanso.launcher.data.db.HomeItemEntity
import com.kanso.launcher.data.db.SurfacePage
import com.kanso.launcher.data.model.KansoActionId
import com.kanso.launcher.data.model.MechaWidgetTheme
import com.kanso.launcher.data.model.TelemetryMetric
import com.kanso.launcher.data.prefs.BuiltInWidgetKey
import com.kanso.launcher.data.prefs.CachedWeather
import com.kanso.launcher.data.prefs.ThemeMode
import com.kanso.launcher.data.telemetry.TelemetryState
import com.kanso.launcher.service.KansoNotificationListener
import com.kanso.launcher.service.KansoPerformanceGovernor
import com.kanso.launcher.service.MediaHudState
import com.kanso.launcher.ui.FolderWithApps
import com.kanso.launcher.ui.LauncherViewModel
import com.kanso.launcher.ui.ResolvedDockSlot
import com.kanso.launcher.ui.panels.KansoShadeOverlay
import com.kanso.launcher.ui.panels.ShadeStage
import com.kanso.launcher.ui.theme.ColorBlockHeaderBackdrop
import com.kanso.launcher.ui.theme.DotMatrixClock
import com.kanso.launcher.ui.theme.JetBrainsMonoFontFamily
import com.kanso.launcher.ui.theme.LocalKansoColors
import com.kanso.launcher.ui.theme.SpaceGroteskFontFamily
import com.kanso.launcher.ui.theme.kansoIconNeumorphic
import com.kanso.launcher.ui.theme.kansoMechaAccents
import com.kanso.launcher.ui.theme.kansoNeumorphic
import com.kanso.launcher.ui.theme.kansoTactileClickable
import com.kanso.launcher.ui.widgets.BatteryCapsuleWidget
import com.kanso.launcher.ui.widgets.CalendarRibbonWidget
import com.kanso.launcher.ui.widgets.KansoAppIcon
import com.kanso.launcher.ui.widgets.MediaHudWidget
import com.kanso.launcher.ui.widgets.ModularWidgetAddDialog
import com.kanso.launcher.ui.widgets.NetworkTelemetryWidget
import com.kanso.launcher.ui.widgets.QuickToggleClusterWidget
import com.kanso.launcher.ui.widgets.StatusVolumeCapsule
import com.kanso.launcher.ui.widgets.WeatherBadgeWidget
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    appWidgetHost: AppWidgetHost,
    onRequestBindExternalWidget: (AppWidgetProviderInfo, Int) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val colors = LocalKansoColors.current
    val scope = rememberCoroutineScope()

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val health by viewModel.permissionsHealth.collectAsStateWithLifecycle()
    val media by viewModel.mediaState.collectAsStateWithLifecycle()
    val badges by viewModel.notificationBadges.collectAsStateWithLifecycle()
    val notifications by viewModel.activeNotifications.collectAsStateWithLifecycle()
    val audioBands by viewModel.audioBands.collectAsStateWithLifecycle()
    val dockSlots by viewModel.dockSlots.collectAsStateWithLifecycle()
    val homeItems by viewModel.homeItems.collectAsStateWithLifecycle()
    val folders by viewModel.foldersWithApps.collectAsStateWithLifecycle()
    val overrides by viewModel.iconOverrides.collectAsStateWithLifecycle()
    val visibleApps by viewModel.visibleApps.collectAsStateWithLifecycle()

    var isEditMode by remember { mutableStateOf(false) }
    var selectedShowcaseWidgetKey by remember(isEditMode) { mutableStateOf<String?>(null) }
    var shadeStage by remember { mutableStateOf(ShadeStage.CLOSED) }
    var openFolder by remember { mutableStateOf<FolderWithApps?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showWidgetAddDialog by remember { mutableStateOf(false) }
    var editingShowcaseWidgetKey by remember { mutableStateOf<String?>(null) }
    var editingWidgetItem by remember { mutableStateOf<HomeItemEntity?>(null) }
    var editingDockSlotIndex by remember { mutableStateOf<Int?>(null) }
    var verticalDragAccum by remember { mutableFloatStateOf(0f) }

    // Dynamic pager: Page 0 = In-Page Kanso Discover, Page 1 = Home Showcase, Pages 2..N = Workspace screens (1..6)
    val workspaceCount = settings.workspacePageCount.coerceIn(1, 6)
    val totalPages = 2 + workspaceCount
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { totalPages })

    LaunchedEffect(totalPages) {
        if (pagerState.currentPage >= totalPages) {
            pagerState.scrollToPage((totalPages - 1).coerceAtLeast(1))
        }
    }

    LaunchedEffect(pagerState.isScrollInProgress) {
        if (pagerState.isScrollInProgress) {
            KansoPerformanceGovernor.notifyAnimationBurst(750L)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshPermissions()
        if (granted) {
            viewModel.enableAutoLocationWeather()
        } else {
            viewModel.refreshWeather(force = true)
        }
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermissions()
        onPauseOrDispose {}
    }

    BackHandler(enabled = isEditMode) {
        isEditMode = false
    }

    val canvasColor = if (settings.useWallpaperOverlay) {
        val alpha = (1f - settings.wallpaperTransparencyPercent.coerceIn(0, 100) / 100f).coerceIn(0f, 1f)
        colors.canvasBg.copy(alpha = alpha)
    } else {
        colors.canvasBg
    }

    val openShadeStep: () -> Unit = {
        shadeStage = when (shadeStage) {
            ShadeStage.CLOSED -> ShadeStage.NOTIFICATIONS
            ShadeStage.NOTIFICATIONS -> ShadeStage.CONTROLS
            ShadeStage.CONTROLS -> ShadeStage.CONTROLS
        }
    }

    val homeVerticalNestedScroll = rememberHomeVerticalGestureConnection(
        enabled = shadeStage == ShadeStage.CLOSED && !isEditMode,
        onOpenDrawer = onOpenDrawer,
        onOpenShade = openShadeStep
    )

    val activeWorkspacePagerIndex = if (pagerState.currentPage >= 2) pagerState.currentPage else 2
    val activeWorkspaceDbPage = SurfacePage.workspacePageIndex(
        (activeWorkspacePagerIndex - 2).coerceIn(0, workspaceCount - 1)
    )

    val openNotificationAccess = {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        Unit
    }

    val handleWeatherClick = {
        if (!health.coarseLocationGranted) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        } else {
            viewModel.enableAutoLocationWeather()
        }
    }

    val triggerPowerLock = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.executeAction(context, KansoActionId.LOCK_SCREEN)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasColor)
            .pointerInput(settings.lockscreenCompanionEnabled) {
                detectTapGestures(
                    onDoubleTap = {
                        viewModel.executeAction(context, KansoActionId.LOCK_SCREEN)
                    },
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isEditMode = !isEditMode
                    }
                )
            }
            .pointerInput(shadeStage, pagerState.currentPage) {
                detectVerticalDragGestures(
                    onDragStart = { verticalDragAccum = 0f },
                    onVerticalDrag = { _, dragAmount -> verticalDragAccum += dragAmount },
                    onDragEnd = {
                        if (pagerState.currentPage != 0) {
                            when {
                                verticalDragAccum < -105f && shadeStage == ShadeStage.CLOSED -> onOpenDrawer()
                                verticalDragAccum > 105f -> {
                                    shadeStage = when (shadeStage) {
                                        ShadeStage.CLOSED -> ShadeStage.NOTIFICATIONS
                                        ShadeStage.NOTIFICATIONS -> ShadeStage.CONTROLS
                                        ShadeStage.CONTROLS -> ShadeStage.CONTROLS
                                    }
                                }
                            }
                        }
                        verticalDragAccum = 0f
                    }
                )
            }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val absOffset = abs(pageOffset).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f - absOffset)
                    .graphicsLayer {
                        cameraDistance = 14f * density
                        rotationY = (pageOffset * -9.5f).coerceIn(-16f, 16f)
                        val scale = 1f - absOffset * 0.075f
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - absOffset * 0.14f
                        translationX = pageOffset * -28f
                    }
            ) {
                when (page) {
                    0 -> KansoDiscoverFeedPage(
                        apps = visibleApps,
                        iconPackManager = viewModel.iconPackManager,
                        iconPackMode = settings.iconPackMode,
                        selectedIconPackPackage = settings.selectedIconPackPackage,
                        overrides = overrides,
                        onLaunchApp = { pkg, act -> viewModel.appRepository.launchApp(pkg, act) },
                        onVoiceSearch = { viewModel.appRepository.openGoogleVoice() },
                        onReturnHome = { scope.launch { pagerState.animateScrollToPage(1) } }
                    )

                    1 -> HomeShowcasePage(
                        telemetry = telemetry,
                        use24HourClock = settings.use24HourClock,
                        media = media,
                        audioBands = audioBands,
                        weather = settings.weather,
                        notificationAccessGranted = health.notificationAccessGranted,
                        enabledWidgets = settings.page1Widgets,
                        widgetThemes = settings.page1WidgetThemes,
                        telemetryMetric = settings.page1TelemetryMetric,
                        volumeToggleAction = settings.page1VolumeToggleAction,
                        showOptionalSearchBar = settings.showHomeSearchBar,
                        isEditMode = isEditMode,
                        selectedWidgetKey = selectedShowcaseWidgetKey,
                        nestedScrollConnection = homeVerticalNestedScroll,
                        onEnterEditMode = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isEditMode = true
                        },
                    onSelectShowcaseWidget = { widgetKey ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isEditMode = true
                        selectedShowcaseWidgetKey = widgetKey
                    },
                    onClearSelection = { selectedShowcaseWidgetKey = null },
                    onConfigureShowcaseWidget = { widgetKey ->
                        editingShowcaseWidgetKey = widgetKey
                    },
                    onLockDevice = triggerPowerLock,
                    onClockClick = { viewModel.appRepository.launchRoleFallback("CLOCK") },
                    onVolumeToggle = {
                        viewModel.executeAction(
                            context = context,
                            action = settings.page1VolumeToggleAction,
                            onOpenSettings = onOpenSettings
                        )
                    },
                    onBatteryClick = {
                        viewModel.executeAction(
                            context = context,
                            action = settings.page1BatteryAction,
                            onOpenSettings = onOpenSettings
                        )
                    },
                    onTelemetryClick = {
                        viewModel.executeAction(
                            context = context,
                            action = settings.page1TelemetryAction,
                            onOpenSettings = onOpenSettings
                        )
                    },
                    onPlayPause = { KansoNotificationListener.togglePlayPause(context) },
                    onMediaPrev = { KansoNotificationListener.skipPrevious(context) },
                    onMediaNext = { KansoNotificationListener.skipNext(context) },
                    onOpenPlayerApp = { pkg -> viewModel.appRepository.launchApp(pkg, "") },
                    onRequestMediaPermission = openNotificationAccess,
                    onWifiClick = { viewModel.telemetryRepository.openInternetPanel() },
                    onMobileDataClick = { viewModel.telemetryRepository.openMobileDataPanel() },
                    onBluetoothClick = { viewModel.telemetryRepository.openBluetoothControls() },
                    onWeatherClick = handleWeatherClick,
                    onCalendarClick = { viewModel.appRepository.launchRoleFallback("CALENDAR") },
                    onSearchBarClick = onOpenDrawer
                )

                else -> {
                    val workspaceSlot = (page - 2).coerceAtLeast(0)
                    val dbPageIndex = SurfacePage.workspacePageIndex(workspaceSlot)
                    HomeWorkspacePage(
                        workspaceSlot = workspaceSlot,
                        workspacePageCount = workspaceCount,
                        items = homeItems.filter { it.pageIndex == dbPageIndex },
                        columns = settings.homeGridColumns,
                        folders = folders,
                        overrides = overrides,
                        badges = badges,
                        iconPackManager = viewModel.iconPackManager,
                        iconPackMode = settings.iconPackMode,
                        selectedIconPackPackage = settings.selectedIconPackPackage,
                        telemetry = telemetry,
                        media = media,
                        weather = settings.weather,
                        audioBands = audioBands,
                        notificationAccessGranted = health.notificationAccessGranted,
                        appWidgetHost = appWidgetHost,
                        isEditMode = isEditMode,
                        onEnterEditMode = { isEditMode = true },
                        onLaunchApp = { pkg, act -> viewModel.appRepository.launchApp(pkg, act) },
                        onOpenFolder = { openFolder = it },
                        onExecuteAction = { action, pkg, act ->
                            viewModel.executeAction(context, action, pkg, act, onOpenSettings)
                        },
                        onMediaPlayPause = { KansoNotificationListener.togglePlayPause(context) },
                        onMediaPrev = { KansoNotificationListener.skipPrevious(context) },
                        onMediaNext = { KansoNotificationListener.skipNext(context) },
                        onOpenPlayerApp = { pkg -> viewModel.appRepository.launchApp(pkg, "") },
                        onRequestMediaPermission = openNotificationAccess,
                        onWeatherClick = handleWeatherClick,
                        onCalendarClick = { viewModel.appRepository.launchRoleFallback("CALENDAR") },
                        onMoveItemToCell = { item, col, row ->
                            viewModel.moveHomeItemToGridCell(item, col, row, settings.homeGridColumns)
                        },
                        onConfigureItem = { editingWidgetItem = it },
                        onRemoveItem = { item ->
                            item.appWidgetId?.let { appWidgetHost.deleteAppWidgetId(it) }
                            viewModel.removeHomeItem(item)
                        },
                        onAddFolder = { showCreateFolderDialog = true },
                        onAddWidget = { showWidgetAddDialog = true },
                        onAddWorkspacePage = {
                            val newTargetPage = viewModel.addWorkspacePage()
                            scope.launch { pagerState.animateScrollToPage(newTargetPage) }
                        },
                        onRemoveWorkspacePage = {
                            if (workspaceCount > 1) {
                                val fallbackPage = (page - 1).coerceAtLeast(2)
                                scope.launch {
                                    if (page >= totalPages - 1) {
                                        pagerState.animateScrollToPage(fallbackPage)
                                    }
                                    viewModel.removeWorkspacePage(workspaceSlot)
                                }
                            }
                        },
                        nestedScrollConnection = homeVerticalNestedScroll,
                        onOpenDrawer = onOpenDrawer
                    )
                }
                }
            }
        }

        // Dynamic page indicator dots + Floating Mecha Dock (hidden on Discover Page 0)
        AnimatedVisibility(
            visible = pagerState.currentPage != 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    repeat(totalPages) { idx ->
                        val active = pagerState.currentPage == idx
                        Box(
                            modifier = Modifier
                                .size(width = if (active) 18.dp else 6.dp, height = 6.dp)
                                .clip(CircleShape)
                                .background(if (active) colors.accent else colors.inkSecondary.copy(alpha = 0.35f))
                        )
                    }
                }

                FloatingMechaDock(
                    slots = dockSlots,
                    badges = badges,
                    iconPackManager = viewModel.iconPackManager,
                    iconPackMode = settings.iconPackMode,
                    selectedIconPackPackage = settings.selectedIconPackPackage,
                    overrides = overrides,
                    isEditMode = isEditMode,
                    onSlotLongClick = { slot ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        editingDockSlotIndex = slot.slotIndex
                    },
                    onSlotClick = { slot ->
                        if (isEditMode) {
                            editingDockSlotIndex = slot.slotIndex
                        } else if (slot.app != null) {
                            viewModel.appRepository.launchApp(slot.app.packageName, slot.app.activityName)
                        } else {
                            viewModel.appRepository.launchRoleFallback(slot.fallbackRole)
                        }
                    }
                )
            }
        }

        // Long-Press Rearrange / Edit Mode Floating Toolbar
        AnimatedVisibility(
            visible = isEditMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .testTag("home_edit_toolbar")
                    .fillMaxWidth()
                    .kansoNeumorphic(
                        raised = true,
                        cornerRadius = 16.dp,
                        surfaceColor = colors.darkModuleBg.copy(alpha = 0.96f)
                    )
                    .border(1.dp, colors.accent.copy(alpha = 0.50f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditActionChip(
                        icon = Icons.Rounded.Widgets,
                        label = "Widget",
                        onClick = { showWidgetAddDialog = true },
                        modifier = Modifier.testTag("edit_add_widget")
                    )
                    EditActionChip(
                        icon = Icons.Rounded.CreateNewFolder,
                        label = "Folder",
                        onClick = { showCreateFolderDialog = true },
                        modifier = Modifier.testTag("edit_add_folder")
                    )
                    if (workspaceCount < 6) {
                        EditActionChip(
                            icon = Icons.Rounded.Add,
                            label = "Page",
                            onClick = {
                                val nextPage = viewModel.addWorkspacePage()
                                scope.launch { pagerState.animateScrollToPage(nextPage) }
                            },
                            modifier = Modifier.testTag("edit_add_workspace_page")
                        )
                    }
                    Box(
                        modifier = Modifier
                            .testTag("edit_quick_theme_toggle")
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.darkModuleElevated)
                            .kansoTactileClickable {
                                viewModel.setThemeMode(if (colors.isDark) ThemeMode.LIGHT else ThemeMode.DARK)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (colors.isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            contentDescription = "Toggle Light or Dark mode",
                            tint = colors.accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .testTag("edit_open_settings")
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.darkModuleElevated)
                            .kansoTactileClickable {
                                isEditMode = false
                                onOpenSettings()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.cd_open_settings),
                            tint = Color(0xFFF2F3F5),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .testTag("edit_mode_done")
                        .size(36.dp)
                        .kansoIconNeumorphic(cornerRadius = 11.dp, shadowScale = 0.55f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(colors.accent)
                        .kansoTactileClickable { isEditMode = false },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = stringResource(R.string.cd_edit_mode_done),
                        tint = colors.onAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Two-Stage Custom Notification & Customizable Control Panel Overlay
        KansoShadeOverlay(
            stage = shadeStage,
            notifications = notifications,
            notificationAccessGranted = health.notificationAccessGranted,
            telemetry = telemetry,
            media = media,
            controlTiles = settings.controlPanelTiles,
            apps = visibleApps,
            iconPackManager = viewModel.iconPackManager,
            iconPackMode = settings.iconPackMode,
            selectedIconPackPackage = settings.selectedIconPackPackage,
            overrides = overrides,
            onStageChange = { shadeStage = it },
            onOpenNotificationAccessSettings = openNotificationAccess,
            onDismissNotification = { key -> KansoNotificationListener.cancelNotificationByKey(key) },
            onClearAllNotifications = { KansoNotificationListener.clearAllNotifications() },
            onExecuteTile = { tile ->
                if (tile.action == KansoActionId.OPEN_SETTINGS || tile.action == KansoActionId.LAUNCH_APP) {
                    shadeStage = ShadeStage.CLOSED
                }
                viewModel.executeAction(
                    context = context,
                    action = tile.action,
                    targetPackage = tile.packageName,
                    targetActivity = tile.activityName,
                    onOpenSettings = onOpenSettings
                )
            },
            onAddControlTile = { viewModel.addControlPanelTile(it) },
            onUpdateControlTile = { idx, tile -> viewModel.updateControlPanelTile(idx, tile) },
            onRemoveControlTile = { idx -> viewModel.removeControlPanelTile(idx) },
            onResetControlTiles = { viewModel.resetControlPanelTiles() },
            onBrightnessChange = { viewModel.telemetryRepository.setBrightnessFraction(it) },
            onVolumeChange = { viewModel.telemetryRepository.setMediaVolumeFraction(it) },
            onPlayPause = { KansoNotificationListener.togglePlayPause(context) },
            onPrevTrack = { KansoNotificationListener.skipPrevious(context) },
            onNextTrack = { KansoNotificationListener.skipNext(context) },
            onOpenPlayerApp = { pkg -> viewModel.appRepository.launchApp(pkg, "") }
        )
    }

    openFolder?.let { folder ->
        CircularFolderDialog(
            folder = folder,
            allApps = visibleApps,
            iconPackManager = viewModel.iconPackManager,
            iconPackMode = settings.iconPackMode,
            selectedIconPackPackage = settings.selectedIconPackPackage,
            overrides = overrides,
            badges = badges,
            onLaunchApp = { pkg, act -> viewModel.appRepository.launchApp(pkg, act) },
            onRemoveApp = { pkg, act ->
                viewModel.removeAppFromFolder(folder.folder.folderId, pkg, act)
            },
            onDismiss = { openFolder = null }
        )
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            apps = visibleApps,
            iconPackManager = viewModel.iconPackManager,
            iconPackMode = settings.iconPackMode,
            selectedIconPackPackage = settings.selectedIconPackPackage,
            overrides = overrides,
            onCreate = { title, apps ->
                viewModel.createCircularFolder(title, apps, pageIndex = activeWorkspaceDbPage)
                scope.launch { pagerState.animateScrollToPage(activeWorkspacePagerIndex) }
            },
            onDismiss = { showCreateFolderDialog = false }
        )
    }

    if (showWidgetAddDialog) {
        val manager = remember(context) { AppWidgetManager.getInstance(context) }
        val providers = remember(context) { runCatching { manager.installedProviders }.getOrDefault(emptyList()) }
        ModularWidgetAddDialog(
            telemetry = telemetry,
            media = media,
            weather = settings.weather,
            audioBands = audioBands,
            providers = providers,
            allowExternalWidgets = true,
            onAddKansoWidget = { template ->
                viewModel.addModularWidget(
                    itemType = template.itemType,
                    label = template.title,
                    spanCols = template.spanCols,
                    spanRows = template.spanRows,
                    config = template.config,
                    pageIndex = activeWorkspaceDbPage
                )
                scope.launch { pagerState.animateScrollToPage(activeWorkspacePagerIndex) }
            },
            onSelectExternalProvider = { info ->
                onRequestBindExternalWidget(info, activeWorkspaceDbPage)
                scope.launch { pagerState.animateScrollToPage(activeWorkspacePagerIndex) }
            },
            onDismiss = { showWidgetAddDialog = false }
        )
    }

    editingWidgetItem?.let { item ->
        ModularWidgetConfigDialog(
            item = item,
            maxCols = settings.homeGridColumns,
            apps = visibleApps,
            iconPackManager = viewModel.iconPackManager,
            iconPackMode = settings.iconPackMode,
            selectedIconPackPackage = settings.selectedIconPackPackage,
            overrides = overrides,
            onSaveConfig = { cfg, spanCols, spanRows, label ->
                viewModel.updateHomeItemConfig(item, cfg, label)
                viewModel.resizeHomeItem(item, spanCols, spanRows, settings.homeGridColumns)
            },
            onRemoveWidget = {
                item.appWidgetId?.let { appWidgetHost.deleteAppWidgetId(it) }
                viewModel.removeHomeItem(item)
            },
            onDismiss = { editingWidgetItem = null }
        )
    }

    editingShowcaseWidgetKey?.let { widgetKey ->
        SingleShowcaseWidgetDialog(
            widgetKey = widgetKey,
            settings = settings,
            onSetTheme = { theme -> viewModel.setShowcaseWidgetTheme(widgetKey, theme) },
            onToggleVisibility = { viewModel.togglePage1Widget(widgetKey) },
            onSetTelemetryMetric = { viewModel.setPage1TelemetryMetric(it) },
            onSetTelemetryAction = { viewModel.setPage1TelemetryAction(it) },
            onSetVolumeAction = { viewModel.setPage1VolumeToggleAction(it) },
            onSetBatteryAction = { viewModel.setPage1BatteryAction(it) },
            onDismiss = { editingShowcaseWidgetKey = null }
        )
    }

    editingDockSlotIndex?.let { slotIdx ->
        DockSlotPickerDialog(
            slotIndex = slotIdx,
            apps = visibleApps,
            iconPackManager = viewModel.iconPackManager,
            iconPackMode = settings.iconPackMode,
            selectedIconPackPackage = settings.selectedIconPackPackage,
            overrides = overrides,
            onPickApp = { app -> viewModel.pinAppToDock(app, slotIdx) },
            onResetDefault = { viewModel.resetDockSlot(slotIdx) },
            onDismiss = { editingDockSlotIndex = null }
        )
    }
}

@Composable
private fun rememberHomeVerticalGestureConnection(
    enabled: Boolean,
    onOpenDrawer: () -> Unit,
    onOpenShade: () -> Unit
): NestedScrollConnection {
    return remember(enabled, onOpenDrawer, onOpenShade) {
        object : NestedScrollConnection {
            private var upAccum = 0f
            private var downAccum = 0f
            private var triggered = false

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!enabled || source != NestedScrollSource.UserInput) return Offset.Zero
                if (consumed.y != 0f) {
                    upAccum = 0f
                    downAccum = 0f
                }
                if (available.y < -1f) {
                    upAccum += available.y
                    downAccum = 0f
                    if (!triggered && upAccum <= -88f) {
                        triggered = true
                        upAccum = 0f
                        onOpenDrawer()
                    }
                } else if (available.y > 1f) {
                    downAccum += available.y
                    upAccum = 0f
                    if (!triggered && downAccum >= 96f) {
                        triggered = true
                        downAccum = 0f
                        onOpenShade()
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (enabled && !triggered) {
                    if (available.y < -750f || upAccum <= -52f) {
                        onOpenDrawer()
                    } else if (available.y > 750f || downAccum >= 64f) {
                        onOpenShade()
                    }
                }
                upAccum = 0f
                downAccum = 0f
                triggered = false
                return Velocity.Zero
            }
        }
    }
}

@Composable
private fun EditActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalKansoColors.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.darkModuleElevated)
            .kansoTactileClickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = colors.accent,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            color = Color(0xFFF2F3F5),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ShowcaseSelectableWidgetSlot(
    widgetKey: String,
    isEditMode: Boolean,
    isSelected: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = LocalKansoColors.current
    Box(
        modifier = modifier.then(
            if (isEditMode && isSelected) {
                Modifier
                    .border(1.8.dp, colors.accent, RoundedCornerShape(14.dp))
                    .padding(2.dp)
            } else {
                Modifier
            }
        )
    ) {
        content()
        if (isEditMode && isSelected) {
            val pillShape = RoundedCornerShape(10.dp)
            Row(
                modifier = Modifier
                    .testTag("showcase_widget_settings_$widgetKey")
                    .zIndex(20f)
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-14).dp)
                    .kansoIconNeumorphic(cornerRadius = 10.dp, shadowScale = 0.6f)
                    .clip(pillShape)
                    .background(colors.darkModuleBg)
                    .border(1.dp, colors.accent, pillShape)
                    .kansoTactileClickable(onClick = onOpenSettings)
                    .padding(horizontal = 9.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Configure $widgetKey",
                    tint = colors.accent,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "SETTINGS",
                    color = Color(0xFFF2F3F5),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.5.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

/**
 * Center Default Page (Page 1): Kanso Mecha Showcase Home Screen.
 * - Top-right power button locks the device cleanly.
 * - Dynamically compacts header spacing when the optional Home Search Bar is enabled so Calendar
 *   never gets pushed or hidden beneath the floating dock.
 * - Long-pressing any widget enters Rearrange/Edit Mode and pops a Settings pill on ONLY that widget.
 */
@Composable
private fun HomeShowcasePage(
    telemetry: TelemetryState,
    use24HourClock: Boolean,
    media: MediaHudState,
    audioBands: List<Float>,
    weather: CachedWeather,
    notificationAccessGranted: Boolean,
    enabledWidgets: List<String>,
    widgetThemes: Map<String, MechaWidgetTheme>,
    telemetryMetric: TelemetryMetric,
    volumeToggleAction: KansoActionId,
    showOptionalSearchBar: Boolean,
    isEditMode: Boolean,
    selectedWidgetKey: String?,
    nestedScrollConnection: NestedScrollConnection,
    onEnterEditMode: () -> Unit,
    onSelectShowcaseWidget: (String) -> Unit,
    onClearSelection: () -> Unit,
    onConfigureShowcaseWidget: (String) -> Unit,
    onLockDevice: () -> Unit,
    onClockClick: () -> Unit,
    onVolumeToggle: () -> Unit,
    onBatteryClick: () -> Unit,
    onTelemetryClick: () -> Unit,
    onPlayPause: () -> Unit,
    onMediaPrev: () -> Unit,
    onMediaNext: () -> Unit,
    onOpenPlayerApp: (String) -> Unit,
    onRequestMediaPermission: () -> Unit,
    onWifiClick: () -> Unit,
    onMobileDataClick: () -> Unit,
    onBluetoothClick: () -> Unit,
    onWeatherClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onSearchBarClick: () -> Unit
) {
    val colors = LocalKansoColors.current
    val hours = if (use24HourClock) telemetry.clock.hours24 else telemetry.clock.hours12
    val headerHeight = if (showOptionalSearchBar) 240.dp else 278.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isEditMode) {
                if (isEditMode) {
                    detectTapGestures(onTap = { onClearSelection() })
                }
            }
    ) {
        // Curved Top Accent S-Curve Color-Block
        ColorBlockHeaderBackdrop(
            height = headerHeight,
            leftDropFraction = 0.98f,
            rightShelfFraction = 0.72f,
            curveCenterXFraction = 0.50f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(bottom = 104.dp)
                .nestedScroll(nestedScrollConnection)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(if (showOptionalSearchBar) 10.dp else 14.dp)
        ) {
            // Top Zone: Left Dot-Matrix Clock + Date | Right Animated Sound Switch + Battery Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = if (isEditMode) 60.dp else if (showOptionalSearchBar) 10.dp else 14.dp,
                        bottom = 4.dp
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .testTag("widget_hero_clock")
                        .kansoTactileClickable(onLongClick = onEnterEditMode, onClick = onClockClick)
                        .padding(top = if (showOptionalSearchBar) 6.dp else 12.dp)
                ) {
                    DotMatrixClock(
                        hours = hours,
                        minutes = telemetry.clock.minutes,
                        pixelSize = if (showOptionalSearchBar) 5.2.dp else 5.6.dp,
                        pixelGap = 2.0.dp,
                        digitColor = colors.onAccent,
                        dividerColor = colors.onAccent
                    )
                    Spacer(modifier = Modifier.height(if (showOptionalSearchBar) 6.dp else 10.dp))
                    Text(
                        text = telemetry.clock.formattedDate,
                        color = colors.onAccent,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        letterSpacing = (-0.2).sp
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(top = if (showOptionalSearchBar) 44.dp else 52.dp)
                ) {
                    ShowcaseSelectableWidgetSlot(
                        widgetKey = BuiltInWidgetKey.VOLUME_SWITCH,
                        isEditMode = isEditMode,
                        isSelected = selectedWidgetKey == BuiltInWidgetKey.VOLUME_SWITCH,
                        onOpenSettings = { onConfigureShowcaseWidget(BuiltInWidgetKey.VOLUME_SWITCH) }
                    ) {
                        StatusVolumeCapsule(
                            telemetry = telemetry,
                            action = volumeToggleAction,
                            mechaTheme = widgetThemes[BuiltInWidgetKey.VOLUME_SWITCH] ?: MechaWidgetTheme.DEFAULT,
                            isMediaPlaying = media.isPlaying,
                            onLongClick = { onSelectShowcaseWidget(BuiltInWidgetKey.VOLUME_SWITCH) },
                            onToggle = {
                                if (isEditMode) onSelectShowcaseWidget(BuiltInWidgetKey.VOLUME_SWITCH)
                                else onVolumeToggle()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(if (showOptionalSearchBar) 12.dp else 22.dp))

                    if (BuiltInWidgetKey.BATTERY in enabledWidgets) {
                        ShowcaseSelectableWidgetSlot(
                            widgetKey = BuiltInWidgetKey.BATTERY,
                            isEditMode = isEditMode,
                            isSelected = selectedWidgetKey == BuiltInWidgetKey.BATTERY,
                            onOpenSettings = { onConfigureShowcaseWidget(BuiltInWidgetKey.BATTERY) }
                        ) {
                            BatteryCapsuleWidget(
                                batteryPercent = telemetry.batteryPercent,
                                isCharging = telemetry.isCharging,
                                mechaTheme = widgetThemes[BuiltInWidgetKey.BATTERY] ?: MechaWidgetTheme.DEFAULT,
                                onLongClick = { onSelectShowcaseWidget(BuiltInWidgetKey.BATTERY) },
                                onClick = {
                                    if (isEditMode) onSelectShowcaseWidget(BuiltInWidgetKey.BATTERY)
                                    else onBatteryClick()
                                }
                            )
                        }
                    }
                }
            }

            // Middle Asymmetric Row: Telemetry Card + Audio Dot Visualizer (Left) | Quick-Toggles (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (BuiltInWidgetKey.NETWORK in enabledWidgets) {
                    ShowcaseSelectableWidgetSlot(
                        widgetKey = BuiltInWidgetKey.NETWORK,
                        isEditMode = isEditMode,
                        isSelected = selectedWidgetKey == BuiltInWidgetKey.NETWORK,
                        onOpenSettings = { onConfigureShowcaseWidget(BuiltInWidgetKey.NETWORK) }
                    ) {
                        NetworkTelemetryWidget(
                            readout = telemetry.readoutFor(telemetryMetric),
                            metric = telemetryMetric,
                            mechaTheme = widgetThemes[BuiltInWidgetKey.NETWORK] ?: MechaWidgetTheme.DEFAULT,
                            audioBands = audioBands,
                            isAudioActive = media.isPlaying,
                            showAudioVisualizer = true,
                            onLongClick = { onSelectShowcaseWidget(BuiltInWidgetKey.NETWORK) },
                            onClick = {
                                if (isEditMode) onSelectShowcaseWidget(BuiltInWidgetKey.NETWORK)
                                else onTelemetryClick()
                            }
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(12.dp))
                }

                if (BuiltInWidgetKey.QUICK_TOGGLES in enabledWidgets) {
                    ShowcaseSelectableWidgetSlot(
                        widgetKey = BuiltInWidgetKey.QUICK_TOGGLES,
                        isEditMode = isEditMode,
                        isSelected = selectedWidgetKey == BuiltInWidgetKey.QUICK_TOGGLES,
                        onOpenSettings = { onConfigureShowcaseWidget(BuiltInWidgetKey.QUICK_TOGGLES) },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        QuickToggleClusterWidget(
                            isMediaPlaying = media.isPlaying,
                            isWifiConnected = telemetry.isWifiConnected,
                            isMobileConnected = telemetry.isMobileConnected,
                            isBluetoothEnabled = telemetry.isBluetoothEnabled,
                            mechaTheme = widgetThemes[BuiltInWidgetKey.QUICK_TOGGLES] ?: MechaWidgetTheme.DEFAULT,
                            onLongClick = { onSelectShowcaseWidget(BuiltInWidgetKey.QUICK_TOGGLES) },
                            onTogglePlayPause = {
                                if (isEditMode) onSelectShowcaseWidget(BuiltInWidgetKey.QUICK_TOGGLES)
                                else onPlayPause()
                            },
                            onOpenWifiPanel = {
                                if (isEditMode) onSelectShowcaseWidget(BuiltInWidgetKey.QUICK_TOGGLES)
                                else onWifiClick()
                            },
                            onOpenMobileDataPanel = {
                                if (isEditMode) onSelectShowcaseWidget(BuiltInWidgetKey.QUICK_TOGGLES)
                                else onMobileDataClick()
                            },
                            onOpenBluetooth = {
                                if (isEditMode) onSelectShowcaseWidget(BuiltInWidgetKey.QUICK_TOGGLES)
                                else onBluetoothClick()
                            }
                        )
                    }
                }
            }

            // Lower-Middle Row: Media HUD Card (Left) + Weather Medallion (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (BuiltInWidgetKey.MEDIA_HUD in enabledWidgets) {
                    ShowcaseSelectableWidgetSlot(
                        widgetKey = BuiltInWidgetKey.MEDIA_HUD,
                        isEditMode = isEditMode,
                        isSelected = selectedWidgetKey == BuiltInWidgetKey.MEDIA_HUD,
                        onOpenSettings = { onConfigureShowcaseWidget(BuiltInWidgetKey.MEDIA_HUD) },
                        modifier = Modifier.weight(1f)
                    ) {
                        MediaHudWidget(
                            media = media,
                            notificationAccessGranted = notificationAccessGranted,
                            mechaTheme = widgetThemes[BuiltInWidgetKey.MEDIA_HUD] ?: MechaWidgetTheme.DEFAULT,
                            onLongClick = { onSelectShowcaseWidget(BuiltInWidgetKey.MEDIA_HUD) },
                            onRequestMediaPermission = {
                                if (isEditMode) onSelectShowcaseWidget(BuiltInWidgetKey.MEDIA_HUD)
                                else onRequestMediaPermission()
                            },
                            onOpenPlayerApp = { pkg ->
                                if (isEditMode) onSelectShowcaseWidget(BuiltInWidgetKey.MEDIA_HUD)
                                else onOpenPlayerApp(pkg)
                            },
                            onPlayPause = {
                                if (isEditMode) onSelectShowcaseWidget(BuiltInWidgetKey.MEDIA_HUD)
                                else onPlayPause()
                            },
                            onPrevious = {
                                if (isEditMode) onSelectShowcaseWidget(BuiltInWidgetKey.MEDIA_HUD)
                                else onMediaPrev()
                            },
                            onNext = {
                                if (isEditMode) onSelectShowcaseWidget(BuiltInWidgetKey.MEDIA_HUD)
                                else onMediaNext()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (BuiltInWidgetKey.WEATHER in enabledWidgets) {
                    ShowcaseSelectableWidgetSlot(
                        widgetKey = BuiltInWidgetKey.WEATHER,
                        isEditMode = isEditMode,
                        isSelected = selectedWidgetKey == BuiltInWidgetKey.WEATHER,
                        onOpenSettings = { onConfigureShowcaseWidget(BuiltInWidgetKey.WEATHER) }
                    ) {
                        WeatherBadgeWidget(
                            weather = weather,
                            mechaTheme = widgetThemes[BuiltInWidgetKey.WEATHER] ?: MechaWidgetTheme.DEFAULT,
                            onLongClick = { onSelectShowcaseWidget(BuiltInWidgetKey.WEATHER) },
                            onClick = {
                                if (isEditMode) onSelectShowcaseWidget(BuiltInWidgetKey.WEATHER)
                                else onWeatherClick()
                            }
                        )
                    }
                }
            }

            if (showOptionalSearchBar) {
                Row(
                    modifier = Modifier
                        .testTag("home_optional_search_bar")
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .height(42.dp)
                        .kansoNeumorphic(raised = false, cornerRadius = 15.dp)
                        .kansoTactileClickable(onLongClick = onEnterEditMode, onClick = onSearchBarClick)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.search_apps_hint),
                        color = colors.inkSecondary,
                        fontSize = 12.5.sp
                    )
                }
            }

            // Full-Width 7-Day Calendar Ribbon — always visible above the floating dock
            if (BuiltInWidgetKey.CALENDAR in enabledWidgets) {
                ShowcaseSelectableWidgetSlot(
                    widgetKey = BuiltInWidgetKey.CALENDAR,
                    isEditMode = isEditMode,
                    isSelected = selectedWidgetKey == BuiltInWidgetKey.CALENDAR,
                    onOpenSettings = { onConfigureShowcaseWidget(BuiltInWidgetKey.CALENDAR) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                ) {
                    CalendarRibbonWidget(
                        days = telemetry.weekDays,
                        mechaTheme = widgetThemes[BuiltInWidgetKey.CALENDAR] ?: MechaWidgetTheme.DEFAULT,
                        onLongClick = { onSelectShowcaseWidget(BuiltInWidgetKey.CALENDAR) },
                        onClick = {
                            if (isEditMode) onSelectShowcaseWidget(BuiltInWidgetKey.CALENDAR)
                            else onCalendarClick()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }

        // Top-Right Corner Power Lock Button (hidden only when Edit Toolbar covers the top bar)
        if (!isEditMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 6.dp, end = 12.dp)
                    .size(48.dp)
                    .testTag("home_power_lock_btn")
                    .kansoTactileClickable(onClick = onLockDevice),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .kansoIconNeumorphic(cornerRadius = 11.dp, shadowScale = 0.6f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(colors.darkModuleBg.copy(alpha = 0.90f))
                        .border(1.dp, colors.onAccent.copy(alpha = 0.35f), RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PowerSettingsNew,
                        contentDescription = stringResource(R.string.cd_power_lock),
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Modular Workspace Screen (supports multiple user-added workspace screens, Accent Color Blocks,
 * and rearrange-first widget drag-and-drop).
 */
@Composable
private fun HomeWorkspacePage(
    workspaceSlot: Int,
    workspacePageCount: Int,
    items: List<HomeItemEntity>,
    columns: Int,
    folders: Map<Long, FolderWithApps>,
    overrides: Map<String, com.kanso.launcher.data.db.IconOverrideEntity>,
    badges: Map<String, Int>,
    iconPackManager: com.kanso.launcher.data.iconpacks.IconPackManager,
    iconPackMode: com.kanso.launcher.data.prefs.IconPackMode,
    selectedIconPackPackage: String,
    telemetry: TelemetryState,
    media: MediaHudState,
    weather: CachedWeather,
    audioBands: List<Float>,
    notificationAccessGranted: Boolean,
    appWidgetHost: AppWidgetHost,
    isEditMode: Boolean,
    onEnterEditMode: () -> Unit,
    onLaunchApp: (String, String) -> Unit,
    onOpenFolder: (FolderWithApps) -> Unit,
    onExecuteAction: (KansoActionId, String, String) -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaPrev: () -> Unit,
    onMediaNext: () -> Unit,
    onOpenPlayerApp: (String) -> Unit,
    onRequestMediaPermission: () -> Unit,
    onWeatherClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onMoveItemToCell: (HomeItemEntity, Int, Int) -> Unit,
    onConfigureItem: (HomeItemEntity) -> Unit,
    onRemoveItem: (HomeItemEntity) -> Unit,
    onAddFolder: () -> Unit,
    onAddWidget: () -> Unit,
    onAddWorkspacePage: () -> Unit,
    onRemoveWorkspacePage: () -> Unit,
    nestedScrollConnection: NestedScrollConnection,
    onOpenDrawer: () -> Unit
) {
    val colors = LocalKansoColors.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(
                        top = if (isEditMode) 128.dp else 20.dp,
                        bottom = 114.dp,
                        start = 18.dp,
                        end = 18.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .kansoNeumorphic(raised = true, cornerRadius = 24.dp)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Workspace Screen ${workspaceSlot + 1}",
                        color = colors.inkPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Add Neumorphic or Mecha widgets, edge-to-edge Accent Color Blocks, app folders, or third-party Android widgets. Long-press anywhere to edit.",
                        color = colors.inkSecondary,
                        fontSize = 12.5.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .testTag("workspace_open_drawer_btn")
                                .kansoIconNeumorphic(cornerRadius = 12.dp, shadowScale = 0.6f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.darkModuleBg)
                                .kansoTactileClickable(onClick = onOpenDrawer)
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Browse Apps",
                                color = Color(0xFFF2F3F5),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .kansoIconNeumorphic(cornerRadius = 12.dp, shadowScale = 0.6f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.accent)
                                .kansoTactileClickable(onClick = onAddWidget)
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "+ Widgets & Blocks",
                                color = colors.onAccent,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }
            }
        } else {
            // Full-bleed edge-to-edge scrollable Workspace canvas (Accent Blocks extend behind status bar & dock)
            ModularGridCanvas(
                items = items,
                columns = columns,
                cellHeight = 90.dp,
                minRows = 6,
                folders = folders,
                overrides = overrides,
                badges = badges,
                iconPackManager = iconPackManager,
                iconPackMode = iconPackMode,
                selectedIconPackPackage = selectedIconPackPackage,
                telemetry = telemetry,
                media = media,
                weather = weather,
                audioBands = audioBands,
                notificationAccessGranted = notificationAccessGranted,
                appWidgetHost = appWidgetHost,
                isEditMode = isEditMode,
                onEnterEditMode = onEnterEditMode,
                onLaunchApp = onLaunchApp,
                onOpenFolder = onOpenFolder,
                onExecuteAction = onExecuteAction,
                onMediaPlayPause = onMediaPlayPause,
                onMediaPrev = onMediaPrev,
                onMediaNext = onMediaNext,
                onOpenPlayerApp = onOpenPlayerApp,
                onRequestMediaPermission = onRequestMediaPermission,
                onWeatherClick = onWeatherClick,
                onCalendarClick = onCalendarClick,
                onMoveItemToCell = onMoveItemToCell,
                onConfigureItem = onConfigureItem,
                onRemoveItem = onRemoveItem,
                contentTopPadding = statusBarTop + if (isEditMode) 124.dp else 14.dp,
                contentBottomPadding = navBarBottom + 114.dp,
                horizontalContentInset = 14.dp,
                fullBleedAccentBlocks = true,
                nestedScrollConnection = nestedScrollConnection,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Workspace management bar is hidden in normal mode and appears ONLY when long-pressed to edit
        AnimatedVisibility(
            visible = isEditMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 68.dp, start = 12.dp, end = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .kansoNeumorphic(
                        raised = true,
                        cornerRadius = 15.dp,
                        surfaceColor = colors.surfaceRaised.copy(alpha = 0.96f)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WORKSPACE ${workspaceSlot + 1} / $workspacePageCount",
                        color = colors.inkPrimary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "DRAG TO MOVE • TAP ITEM FOR SETTINGS",
                        color = colors.accent,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 8.sp
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (workspacePageCount > 1) {
                        Box(
                            modifier = Modifier
                                .testTag("workspace_remove_page_btn")
                                .size(32.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(colors.darkModuleBg.copy(alpha = 0.14f))
                                .border(1.dp, colors.inkSecondary.copy(alpha = 0.30f), RoundedCornerShape(9.dp))
                                .kansoTactileClickable(onClick = onRemoveWorkspacePage),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = stringResource(R.string.cd_remove_workspace_page),
                                tint = colors.inkPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (workspacePageCount < 6) {
                        Box(
                            modifier = Modifier
                                .testTag("workspace_add_page_btn")
                                .clip(RoundedCornerShape(9.dp))
                                .background(colors.darkModuleBg.copy(alpha = 0.14f))
                                .border(1.dp, colors.accent.copy(alpha = 0.50f), RoundedCornerShape(9.dp))
                                .kansoTactileClickable(onClick = onAddWorkspacePage)
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "+ Page",
                                color = colors.inkPrimary,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .testTag("workspace_add_folder_btn")
                            .clip(RoundedCornerShape(9.dp))
                            .background(colors.darkModuleBg.copy(alpha = 0.14f))
                            .kansoTactileClickable(onClick = onAddFolder)
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "+ Folder",
                            color = colors.inkPrimary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Box(
                        modifier = Modifier
                            .testTag("workspace_add_widget_btn")
                            .clip(RoundedCornerShape(9.dp))
                            .background(colors.accent)
                            .kansoTactileClickable(onClick = onAddWidget)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "+ Widget",
                            color = colors.onAccent,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Floating Mecha Dock with customizable slots and active IconPack rendering.
 */
@Composable
private fun FloatingMechaDock(
    slots: List<ResolvedDockSlot>,
    badges: Map<String, Int>,
    iconPackManager: com.kanso.launcher.data.iconpacks.IconPackManager,
    iconPackMode: com.kanso.launcher.data.prefs.IconPackMode,
    selectedIconPackPackage: String,
    overrides: Map<String, com.kanso.launcher.data.db.IconOverrideEntity>,
    isEditMode: Boolean,
    onSlotLongClick: (ResolvedDockSlot) -> Unit,
    onSlotClick: (ResolvedDockSlot) -> Unit
) {
    val colors = LocalKansoColors.current

    Row(
        modifier = Modifier
            .testTag("floating_mecha_dock")
            .fillMaxWidth()
            .height(72.dp)
            .kansoNeumorphic(
                raised = true,
                cornerRadius = 36.dp,
                surfaceColor = colors.darkModuleBg
            )
            .kansoMechaAccents(accentActive = isEditMode, showCornerBrackets = false, showLedPip = true)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        slots.forEach { slot ->
            val app = slot.app
            val badgeCount = app?.let { badges[it.packageName] ?: 0 } ?: 0

            Box(
                modifier = Modifier
                    .testTag("dock_slot_${slot.slotIndex}")
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(colors.darkModuleElevated)
                    .then(
                        if (isEditMode) Modifier.border(1.5.dp, colors.accent, CircleShape)
                        else Modifier
                    )
                    .kansoTactileClickable(
                        onLongClick = { onSlotLongClick(slot) },
                        onClick = { onSlotClick(slot) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (app != null) {
                    KansoAppIcon(
                        packageName = app.packageName,
                        activityName = app.activityName,
                        contentDescription = app.label,
                        iconPackManager = iconPackManager,
                        iconPackMode = iconPackMode,
                        selectedIconPackPackage = selectedIconPackPackage,
                        iconOverride = overrides[app.componentKey],
                        badgeCount = badgeCount,
                        iconSize = 52.dp,
                        cornerRadius = 26.dp,
                        tintOverride = Color(0xFFF2F3F5)
                    )
                } else {
                    val fallbackGlyph = when (slot.fallbackRole) {
                        "PHONE" -> Icons.Rounded.Call
                        "MESSAGES" -> Icons.AutoMirrored.Rounded.Chat
                        "BROWSER" -> Icons.Rounded.Language
                        "CAMERA" -> Icons.Rounded.CameraAlt
                        else -> Icons.Rounded.Apps
                    }
                    Icon(
                        imageVector = fallbackGlyph,
                        contentDescription = slot.fallbackRole,
                        tint = Color(0xFFF2F3F5),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
