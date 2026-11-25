package com.kanso.launcher.ui.home

import android.appwidget.AppWidgetHost
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kanso.launcher.data.db.HomeItemEntity
import com.kanso.launcher.data.db.HomeItemType
import com.kanso.launcher.data.db.IconOverrideEntity
import com.kanso.launcher.data.iconpacks.IconPackManager
import com.kanso.launcher.data.model.KansoActionId
import com.kanso.launcher.data.model.ModularWidgetConfig
import com.kanso.launcher.data.prefs.CachedWeather
import com.kanso.launcher.data.prefs.IconPackMode
import com.kanso.launcher.data.telemetry.TelemetryState
import com.kanso.launcher.service.KansoPerformanceGovernor
import com.kanso.launcher.service.MediaHudState
import com.kanso.launcher.ui.FolderWithApps
import com.kanso.launcher.ui.theme.JetBrainsMonoFontFamily
import com.kanso.launcher.ui.theme.LocalKansoColors
import com.kanso.launcher.ui.theme.SpaceGroteskFontFamily
import com.kanso.launcher.ui.theme.kansoIconNeumorphic
import com.kanso.launcher.ui.theme.kansoMechaAccents
import com.kanso.launcher.ui.theme.kansoTactileClickable
import com.kanso.launcher.ui.widgets.AccentColorBlockWidget
import com.kanso.launcher.ui.widgets.ActionMatrixWidget
import com.kanso.launcher.ui.widgets.BatteryCapsuleWidget
import com.kanso.launcher.ui.widgets.CalendarRibbonWidget
import com.kanso.launcher.ui.widgets.CircularFolderIcon
import com.kanso.launcher.ui.widgets.ExternalAppWidgetCard
import com.kanso.launcher.ui.widgets.KansoAppIcon
import com.kanso.launcher.ui.widgets.MediaHudWidget
import com.kanso.launcher.ui.widgets.ModularToggleSwitchWidget
import com.kanso.launcher.ui.widgets.NetworkTelemetryWidget
import com.kanso.launcher.ui.widgets.QuickToggleClusterWidget
import com.kanso.launcher.ui.widgets.WeatherBadgeWidget
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Shared Drag-and-Drop + Dynamic-Sizing Modular Grid Canvas for Workspace & Lockscreen.
 * - Rearrange-first UX: long-pressing any item immediately enters Rearrange Mode, lets the user
 *   drag it seamlessly in the same gesture, selects that item, and pops a dedicated Settings/Remove
 *   action bar attached to ONLY the selected item instead of blocking with an immediate settings modal.
 * - Accent Color Blocks render on a lower architectural z-layer (zIndex = 0f) behind widgets & icons.
 */
@Composable
fun ModularGridCanvas(
    items: List<HomeItemEntity>,
    columns: Int,
    cellHeight: Dp = 90.dp,
    minRows: Int = 5,
    folders: Map<Long, FolderWithApps>,
    overrides: Map<String, IconOverrideEntity>,
    badges: Map<String, Int>,
    iconPackManager: IconPackManager,
    iconPackMode: IconPackMode,
    selectedIconPackPackage: String,
    telemetry: TelemetryState,
    media: MediaHudState,
    weather: CachedWeather,
    audioBands: List<Float>,
    notificationAccessGranted: Boolean,
    appWidgetHost: AppWidgetHost?,
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
    contentTopPadding: Dp = 0.dp,
    contentBottomPadding: Dp = 44.dp,
    horizontalContentInset: Dp = 0.dp,
    fullBleedAccentBlocks: Boolean = false,
    nestedScrollConnection: NestedScrollConnection? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalKansoColors.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val safeCols = columns.coerceIn(4, 5)
    val cellGap = 6.dp

    var selectedItemId by remember(isEditMode) { mutableStateOf<Long?>(null) }
    var draggingItemId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }
    var previewCol by remember { mutableIntStateOf(0) }
    var previewRow by remember { mutableIntStateOf(0) }

    val maxOccupiedRow = remember(items) {
        items.maxOfOrNull { it.gridRow + it.spanRows } ?: 0
    }
    val totalRows = maxOf(minRows, maxOccupiedRow + if (isEditMode || draggingItemId != null) 1 else 0)
    val gridBodyHeight = (cellHeight * totalRows) + (cellGap * (totalRows - 1).coerceAtLeast(0))

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (nestedScrollConnection != null) {
                    Modifier.nestedScroll(nestedScrollConnection)
                } else {
                    Modifier
                }
            )
            .verticalScroll(rememberScrollState())
    ) {
        val gapPx = with(density) { cellGap.toPx() }
        val screenWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val insetPx = with(density) { horizontalContentInset.toPx() }
        val topPadPx = with(density) { contentTopPadding.toPx() }
        val contentWidthPx = (screenWidthPx - insetPx * 2f).coerceAtLeast(1f)
        val cellWidthPx = ((contentWidthPx - gapPx * (safeCols - 1)) / safeCols).coerceAtLeast(1f)
        val cellHeightPx = with(density) { cellHeight.toPx() }.coerceAtLeast(1f)
        val cellWidthDp = with(density) { cellWidthPx.toDp() }
        val minViewportHeight = if (maxHeight.value.isFinite()) maxHeight else 0.dp
        val canvasHeight = maxOf(minViewportHeight, contentTopPadding + gridBodyHeight + contentBottomPadding)
        val canvasHeightPx = with(density) { canvasHeight.toPx() }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(canvasHeight)
                .pointerInput(isEditMode) {
                    if (isEditMode) {
                        detectTapGestures(onTap = { selectedItemId = null })
                    }
                }
        ) {
            if (isEditMode || draggingItemId != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val dotColor = colors.inkSecondary.copy(alpha = 0.24f)
                    for (r in 0 until totalRows) {
                        for (c in 0 until safeCols) {
                            val cx = insetPx + c * (cellWidthPx + gapPx) + cellWidthPx / 2f
                            val cy = topPadPx + r * (cellHeightPx + gapPx) + cellHeightPx / 2f
                            drawCircle(color = dotColor, radius = 1.6.dp.toPx(), center = Offset(cx, cy))
                        }
                    }
                }
            }

            // Drop target rounded reticle while dragging an item
            val activeDragItem = items.firstOrNull { it.id == draggingItemId }
            if (activeDragItem != null) {
                val spanC = activeDragItem.spanColumns.coerceIn(1, safeCols)
                val spanR = activeDragItem.spanRows.coerceIn(1, 5)
                val targetX = (insetPx + previewCol * (cellWidthPx + gapPx)).roundToInt()
                val targetY = (topPadPx + previewRow * (cellHeightPx + gapPx)).roundToInt()
                val targetW = (cellWidthDp * spanC) + (cellGap * (spanC - 1))
                val targetH = (cellHeight * spanR) + (cellGap * (spanR - 1))

                Box(
                    modifier = Modifier
                        .zIndex(25f)
                        .offset { IntOffset(targetX, targetY) }
                        .size(width = targetW, height = targetH)
                        .background(colors.accent.copy(alpha = 0.14f), shape = RoundedCornerShape(14.dp))
                        .kansoMechaAccents(accentActive = true, showCornerBrackets = true, showLedPip = false)
                )
            }

            val selectionShape = RoundedCornerShape(14.dp)

            items.forEach { item ->
                val spanC = item.spanColumns.coerceIn(1, safeCols)
                val spanR = item.spanRows.coerceIn(1, 5)
                val clampedCol = item.gridCol.coerceIn(0, (safeCols - spanC).coerceAtLeast(0))
                val clampedRow = item.gridRow.coerceAtLeast(0)

                val gridBaseX = insetPx + clampedCol * (cellWidthPx + gapPx)
                val gridBaseY = topPadPx + clampedRow * (cellHeightPx + gapPx)
                val gridItemWPx = cellWidthPx * spanC + gapPx * (spanC - 1)
                val gridItemHPx = cellHeightPx * spanR + gapPx * (spanR - 1)

                val isDragging = draggingItemId == item.id
                val isSelected = isEditMode && selectedItemId == item.id
                val isAccentBlock = item.itemType == HomeItemType.ACCENT_BLOCK

                val shouldBleed = fullBleedAccentBlocks && isAccentBlock && !isDragging
                val bleedLeft = if (shouldBleed && clampedCol == 0) 0f else gridBaseX
                val bleedRight = if (shouldBleed && clampedCol + spanC >= safeCols) screenWidthPx else gridBaseX + gridItemWPx
                val bleedTop = if (shouldBleed && clampedRow == 0) 0f else gridBaseY
                val bleedBottom = if (shouldBleed && maxOccupiedRow > 0 && clampedRow + spanR >= maxOccupiedRow) {
                    canvasHeightPx
                } else {
                    gridBaseY + gridItemHPx
                }

                val itemW = with(density) { (bleedRight - bleedLeft).coerceAtLeast(1f).toDp() }
                val itemH = with(density) { (bleedBottom - bleedTop).coerceAtLeast(1f).toDp() }

                val renderX = (bleedLeft + if (isDragging) dragOffsetX else 0f).roundToInt()
                val renderY = (bleedTop + if (isDragging) dragOffsetY else 0f).roundToInt()

                val isConfigurableWidget = item.itemType != HomeItemType.APP &&
                    item.itemType != HomeItemType.FOLDER

                val itemZIndex = when {
                    isDragging -> 30f
                    isSelected -> if (isAccentBlock) 16f else 22f
                    isAccentBlock -> 0f
                    else -> 2f
                }

                Box(
                    modifier = Modifier
                        .testTag("grid_item_${item.id}")
                        .zIndex(itemZIndex)
                        .offset { IntOffset(renderX, renderY) }
                        .size(width = itemW, height = itemH)
                        .graphicsLayer {
                            if (isDragging) {
                                scaleX = 1.03f
                                scaleY = 1.03f
                                alpha = 0.94f
                            }
                        }
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    width = 1.8.dp,
                                    color = colors.accent,
                                    shape = selectionShape
                                )
                            } else if (isEditMode) {
                                Modifier.border(
                                    width = 0.8.dp,
                                    color = colors.inkSecondary.copy(alpha = 0.28f),
                                    shape = selectionShape
                                )
                            } else {
                                Modifier
                            }
                        )
                        .pointerInput(item.id, isEditMode, safeCols, clampedCol, clampedRow, spanC, spanR) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    KansoPerformanceGovernor.notifyAnimationBurst(900L)
                                    if (!isEditMode) onEnterEditMode()
                                    selectedItemId = item.id
                                    draggingItemId = item.id
                                    dragOffsetX = 0f
                                    dragOffsetY = 0f
                                    totalDragDistance = 0f
                                    previewCol = clampedCol
                                    previewRow = clampedRow
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    KansoPerformanceGovernor.notifyAnimationBurst(500L)
                                    dragOffsetX += dragAmount.x
                                    dragOffsetY += dragAmount.y
                                    totalDragDistance += hypot(dragAmount.x, dragAmount.y)
                                    val stepX = (cellWidthPx + gapPx).coerceAtLeast(1f)
                                    val stepY = (cellHeightPx + gapPx).coerceAtLeast(1f)
                                    previewCol = (((gridBaseX - insetPx) + dragOffsetX) / stepX)
                                        .roundToInt()
                                        .coerceIn(0, (safeCols - spanC).coerceAtLeast(0))
                                    previewRow = (((gridBaseY - topPadPx) + dragOffsetY) / stepY)
                                        .roundToInt()
                                        .coerceIn(0, (totalRows + 1).coerceAtLeast(0))
                                },
                                onDragEnd = {
                                    if (totalDragDistance >= 14f) {
                                        onMoveItemToCell(item, previewCol, previewRow)
                                    }
                                    selectedItemId = item.id
                                    draggingItemId = null
                                    dragOffsetX = 0f
                                    dragOffsetY = 0f
                                    totalDragDistance = 0f
                                },
                                onDragCancel = {
                                    draggingItemId = null
                                    dragOffsetX = 0f
                                    dragOffsetY = 0f
                                    totalDragDistance = 0f
                                }
                            )
                        }
                ) {
                    ModularGridItemContent(
                        item = item,
                        cellWidth = cellWidthDp,
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
                        onSelectInEditMode = { selectedItemId = item.id },
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
                        onRemoveItem = { onRemoveItem(item) }
                    )

                    // In Rearrange Mode, capture tap on the item surface to focus/select it cleanly
                    if (isEditMode && !isDragging) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(item.id) {
                                    detectTapGestures(onTap = { selectedItemId = item.id })
                                }
                        )
                    }

                    // Floating contextual action pill pops ONLY for the selected widget/item
                    if (isSelected && !isDragging) {
                        Row(
                            modifier = Modifier
                                .zIndex(28f)
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-14).dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isConfigurableWidget) {
                                val settingsPillShape = RoundedCornerShape(10.dp)
                                Row(
                                    modifier = Modifier
                                        .testTag("grid_config_${item.id}")
                                        .kansoIconNeumorphic(cornerRadius = 10.dp, shadowScale = 0.6f)
                                        .clip(settingsPillShape)
                                        .background(colors.darkModuleBg)
                                        .border(1.dp, colors.accent, settingsPillShape)
                                        .kansoTactileClickable { onConfigureItem(item) }
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = "Widget settings",
                                        tint = colors.accent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "SETTINGS",
                                        color = Color(0xFFF2F3F5),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        letterSpacing = 0.6.sp
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .testTag("grid_remove_${item.id}")
                                    .size(28.dp)
                                    .kansoIconNeumorphic(cornerRadius = 9.dp, shadowScale = 0.55f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(colors.accent)
                                    .kansoTactileClickable {
                                        selectedItemId = null
                                        onRemoveItem(item)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Remove item",
                                    tint = colors.onAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModularGridItemContent(
    item: HomeItemEntity,
    cellWidth: Dp,
    folders: Map<Long, FolderWithApps>,
    overrides: Map<String, IconOverrideEntity>,
    badges: Map<String, Int>,
    iconPackManager: IconPackManager,
    iconPackMode: IconPackMode,
    selectedIconPackPackage: String,
    telemetry: TelemetryState,
    media: MediaHudState,
    weather: CachedWeather,
    audioBands: List<Float>,
    notificationAccessGranted: Boolean,
    appWidgetHost: AppWidgetHost?,
    isEditMode: Boolean,
    onSelectInEditMode: () -> Unit,
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
    onRemoveItem: () -> Unit
) {
    val colors = LocalKansoColors.current
    val config = remember(item.configJson) { ModularWidgetConfig.decode(item.configJson) }

    when (item.itemType) {
        HomeItemType.ACCENT_BLOCK -> {
            AccentColorBlockWidget(
                style = config.accentBlockStyle,
                flipped = config.accentBlockFlipped,
                rotated = config.accentBlockRotated,
                modifier = Modifier.fillMaxSize()
            )
        }

        HomeItemType.EXTERNAL_WIDGET -> {
            val widgetId = item.appWidgetId ?: -1
            if (appWidgetHost != null && widgetId != -1) {
                ExternalAppWidgetCard(
                    appWidgetHost = appWidgetHost,
                    appWidgetId = widgetId,
                    isEditMode = false,
                    onRemove = onRemoveItem,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        HomeItemType.TELEMETRY_CAPSULE -> {
            NetworkTelemetryWidget(
                readout = telemetry.readoutFor(config.telemetryMetric),
                metric = config.telemetryMetric,
                mechaTheme = config.mechaTheme,
                audioBands = audioBands,
                isAudioActive = media.isPlaying,
                showAudioVisualizer = config.showAudioVisualizer,
                onLongClick = null,
                onClick = {
                    if (isEditMode) onSelectInEditMode()
                    else onExecuteAction(config.tapAction, config.targetPackage, config.targetActivity)
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        HomeItemType.ACTION_MATRIX -> {
            ActionMatrixWidget(
                rows = config.matrixRows,
                cols = config.matrixCols,
                tiles = config.matrixTiles,
                telemetry = telemetry,
                mechaTheme = config.mechaTheme,
                isMediaPlaying = media.isPlaying,
                onTileClick = { tile ->
                    if (isEditMode) onSelectInEditMode()
                    else onExecuteAction(tile.action, tile.packageName, tile.activityName)
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        HomeItemType.TOGGLE_SWITCH -> {
            ModularToggleSwitchWidget(
                action = config.toggleAction,
                customLabel = config.customLabel,
                telemetry = telemetry,
                mechaTheme = config.mechaTheme,
                isMediaPlaying = media.isPlaying,
                onLongClick = null,
                onToggle = {
                    if (isEditMode) onSelectInEditMode()
                    else onExecuteAction(config.toggleAction, config.targetPackage, config.targetActivity)
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        HomeItemType.BATTERY_CAPSULE -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                BatteryCapsuleWidget(
                    batteryPercent = telemetry.batteryPercent,
                    isCharging = telemetry.isCharging,
                    mechaTheme = config.mechaTheme,
                    onLongClick = null,
                    onClick = {
                        if (isEditMode) onSelectInEditMode()
                        else onExecuteAction(KansoActionId.BATTERY_SETTINGS, "", "")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        HomeItemType.MEDIA_HUD -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                MediaHudWidget(
                    media = media,
                    notificationAccessGranted = notificationAccessGranted,
                    mechaTheme = config.mechaTheme,
                    onLongClick = null,
                    onRequestMediaPermission = if (isEditMode) onSelectInEditMode else onRequestMediaPermission,
                    onOpenPlayerApp = { pkg ->
                        if (isEditMode) onSelectInEditMode() else onOpenPlayerApp(pkg)
                    },
                    onPlayPause = if (isEditMode) onSelectInEditMode else onMediaPlayPause,
                    onPrevious = if (isEditMode) onSelectInEditMode else onMediaPrev,
                    onNext = if (isEditMode) onSelectInEditMode else onMediaNext,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        HomeItemType.WEATHER_BADGE -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                WeatherBadgeWidget(
                    weather = weather,
                    mechaTheme = config.mechaTheme,
                    onLongClick = null,
                    onClick = if (isEditMode) onSelectInEditMode else onWeatherClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        HomeItemType.CALENDAR_RIBBON -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CalendarRibbonWidget(
                    days = telemetry.weekDays,
                    mechaTheme = config.mechaTheme,
                    onLongClick = null,
                    onClick = if (isEditMode) onSelectInEditMode else onCalendarClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        HomeItemType.QUICK_TOGGLE_CLUSTER -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                QuickToggleClusterWidget(
                    isMediaPlaying = media.isPlaying,
                    isWifiConnected = telemetry.isWifiConnected,
                    isMobileConnected = telemetry.isMobileConnected,
                    isBluetoothEnabled = telemetry.isBluetoothEnabled,
                    mechaTheme = config.mechaTheme,
                    onLongClick = null,
                    onTogglePlayPause = if (isEditMode) onSelectInEditMode else onMediaPlayPause,
                    onOpenWifiPanel = {
                        if (isEditMode) onSelectInEditMode() else onExecuteAction(KansoActionId.WIFI, "", "")
                    },
                    onOpenMobileDataPanel = {
                        if (isEditMode) onSelectInEditMode() else onExecuteAction(KansoActionId.MOBILE_DATA, "", "")
                    },
                    onOpenBluetooth = {
                        if (isEditMode) onSelectInEditMode() else onExecuteAction(KansoActionId.BLUETOOTH, "", "")
                    }
                )
            }
        }

        HomeItemType.FOLDER -> {
            val folderData = item.folderId?.let { folders[it] }
            val folderSize = (cellWidth * 0.86f).coerceIn(54.dp, 64.dp)
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularFolderIcon(
                    apps = folderData?.apps?.map { it.packageName to it.activityName }.orEmpty(),
                    iconPackManager = iconPackManager,
                    iconPackMode = iconPackMode,
                    selectedIconPackPackage = selectedIconPackPackage,
                    overrides = overrides,
                    containerSize = folderSize,
                    modifier = Modifier.kansoTactileClickable {
                        if (isEditMode) onSelectInEditMode()
                        else if (folderData != null) onOpenFolder(folderData)
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = folderData?.folder?.title ?: item.label,
                    color = colors.inkPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }

        else -> {
            val key = "${item.packageName}/${item.activityName}"
            val fullIconSize = (cellWidth * 0.86f).coerceIn(56.dp, 66.dp)
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                KansoAppIcon(
                    packageName = item.packageName,
                    activityName = item.activityName,
                    contentDescription = item.label,
                    iconPackManager = iconPackManager,
                    iconPackMode = iconPackMode,
                    selectedIconPackPackage = selectedIconPackPackage,
                    iconOverride = overrides[key],
                    badgeCount = badges[item.packageName] ?: 0,
                    iconSize = fullIconSize,
                    cornerRadius = 16.dp,
                    modifier = Modifier.kansoTactileClickable {
                        if (isEditMode) onSelectInEditMode()
                        else onLaunchApp(item.packageName, item.activityName)
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.label,
                    color = colors.inkPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }
    }
}
