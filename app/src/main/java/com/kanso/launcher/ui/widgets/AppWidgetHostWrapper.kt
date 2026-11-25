package com.kanso.launcher.ui.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.kanso.launcher.data.db.HomeItemType
import com.kanso.launcher.data.model.AccentBlockStyle
import com.kanso.launcher.data.model.KansoActionId
import com.kanso.launcher.data.model.MechaWidgetTheme
import com.kanso.launcher.data.model.ModularWidgetConfig
import com.kanso.launcher.data.model.TelemetryMetric
import com.kanso.launcher.data.prefs.CachedWeather
import com.kanso.launcher.data.telemetry.TelemetryState
import com.kanso.launcher.service.MediaHudState
import com.kanso.launcher.ui.theme.JetBrainsMonoFontFamily
import com.kanso.launcher.ui.theme.LocalKansoColors
import com.kanso.launcher.ui.theme.SpaceGroteskFontFamily
import com.kanso.launcher.ui.theme.kansoIconNeumorphic
import com.kanso.launcher.ui.theme.kansoNeumorphic
import com.kanso.launcher.ui.theme.kansoTactileClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class KansoWidgetTemplate(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String = "",
    val itemType: String,
    val spanCols: Int,
    val spanRows: Int,
    val config: ModularWidgetConfig = ModularWidgetConfig()
)

/**
 * Hosts a bound third-party Android AppWidget edge-to-edge (ZERO padding) and calls
 * updateAppWidgetSize dynamically whenever its measured cell bounds change.
 */
@Composable
fun ExternalAppWidgetCard(
    appWidgetHost: AppWidgetHost,
    appWidgetId: Int,
    isEditMode: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalKansoColors.current
    val manager = remember(context) { AppWidgetManager.getInstance(context) }
    val info: AppWidgetProviderInfo? = remember(appWidgetId) { manager.getAppWidgetInfo(appWidgetId) }
    var hostViewRef by remember(appWidgetId) { mutableStateOf<AppWidgetHostView?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .testTag("external_widget_$appWidgetId")
            .fillMaxSize()
            .kansoIconNeumorphic(cornerRadius = 18.dp, shadowScale = 0.72f)
            .clip(RoundedCornerShape(18.dp))
    ) {
        val wDp = maxWidth.value.toInt().coerceAtLeast(48)
        val hDp = maxHeight.value.toInt().coerceAtLeast(48)

        LaunchedEffect(hostViewRef, wDp, hDp) {
            val view = hostViewRef ?: return@LaunchedEffect
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    view.updateAppWidgetSize(Bundle.EMPTY, listOf(SizeF(wDp.toFloat(), hDp.toFloat())))
                } else {
                    @Suppress("DEPRECATION")
                    view.updateAppWidgetSize(Bundle.EMPTY, wDp, hDp, wDp, hDp)
                }
            }
        }

        if (info != null) {
            AndroidView(
                factory = { ctx ->
                    appWidgetHost.createView(ctx.applicationContext, appWidgetId, info).apply {
                        setAppWidget(appWidgetId, info)
                        setPadding(0, 0, 0, 0)
                        hostViewRef = this
                    }
                },
                update = { view ->
                    view.setPadding(0, 0, 0, 0)
                    hostViewRef = view
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.darkModuleBg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Widgets,
                    contentDescription = null,
                    tint = colors.inkSecondary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Widget unavailable",
                    color = colors.inkSecondary,
                    fontSize = 12.sp
                )
            }
        }

        if (isEditMode) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(30.dp)
                    .clip(CircleShape)
                    .kansoNeumorphic(raised = true, cornerRadius = 15.dp, surfaceColor = colors.accent)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Remove widget",
                    tint = colors.onAccent,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

/**
 * Live-Preview Widget Add Dialog featuring:
 * - Pre-configured Mecha-Industrial Themes: Reactor Core, Tactical Gauge, and Data Stream
 * - Full modular Kanso widgets & third-party Android AppWidgets with dynamic real-size previews
 */
@Composable
fun ModularWidgetAddDialog(
    telemetry: TelemetryState,
    media: MediaHudState,
    weather: CachedWeather,
    audioBands: List<Float>,
    providers: List<AppWidgetProviderInfo>,
    allowExternalWidgets: Boolean = true,
    onAddKansoWidget: (KansoWidgetTemplate) -> Unit,
    onSelectExternalProvider: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalKansoColors.current
    var activeTab by remember { mutableIntStateOf(0) }

    val templates = remember {
        listOf(
            KansoWidgetTemplate(
                id = "theme_neumorphic_telemetry",
                title = "Kanso Neumorphic — Telemetry Capsule",
                subtitle = "2×2 • Classic soft raised neumorphic shell (Default style)",
                badge = "DEFAULT // NEUMORPHIC",
                itemType = HomeItemType.TELEMETRY_CAPSULE,
                spanCols = 2,
                spanRows = 2,
                config = ModularWidgetConfig(
                    mechaTheme = MechaWidgetTheme.DEFAULT,
                    telemetryMetric = TelemetryMetric.NET_SPEED,
                    tapAction = KansoActionId.WIFI,
                    showAudioVisualizer = true
                )
            ),
            KansoWidgetTemplate(
                id = "theme_machined_mecha",
                title = "Machined Mecha — Telemetry Pod",
                subtitle = "2×2 • Brushed metallic chassis with neumorphic bevel shadows",
                badge = "OPTION // MACHINED MECHA",
                itemType = HomeItemType.TELEMETRY_CAPSULE,
                spanCols = 2,
                spanRows = 2,
                config = ModularWidgetConfig(
                    mechaTheme = MechaWidgetTheme.MACHINED_MECHA,
                    telemetryMetric = TelemetryMetric.CPU_LOAD,
                    tapAction = KansoActionId.BATTERY_SETTINGS,
                    showAudioVisualizer = true
                )
            ),
            KansoWidgetTemplate(
                id = "theme_reactor_core",
                title = "Reactor Core — Telemetry Capsule",
                subtitle = "2×2 • Heavy octagon alloy plate, rotating accent stator & live dots",
                badge = "OPTION // REACTOR CORE",
                itemType = HomeItemType.TELEMETRY_CAPSULE,
                spanCols = 2,
                spanRows = 2,
                config = ModularWidgetConfig(
                    mechaTheme = MechaWidgetTheme.REACTOR_CORE,
                    telemetryMetric = TelemetryMetric.CPU_LOAD,
                    tapAction = KansoActionId.BATTERY_SETTINGS,
                    showAudioVisualizer = true
                )
            ),
            KansoWidgetTemplate(
                id = "theme_tactical_gauge",
                title = "Tactical Gauge — 4×2 Command Matrix",
                subtitle = "4×2 • Asymmetric armor chassis, machined HUD crosshairs & ticks",
                badge = "THEME // TACTICAL GAUGE",
                itemType = HomeItemType.ACTION_MATRIX,
                spanCols = 4,
                spanRows = 2,
                config = ModularWidgetConfig(
                    mechaTheme = MechaWidgetTheme.TACTICAL_GAUGE,
                    matrixRows = 2,
                    matrixCols = 4,
                    matrixTiles = ModularWidgetConfig.defaultMatrixTiles(8)
                )
            ),
            KansoWidgetTemplate(
                id = "theme_data_stream",
                title = "Data Stream — Network & RAM Monitor",
                subtitle = "2×2 • Razor bevel frame, scanning laser sweep & live telemetry",
                badge = "THEME // DATA STREAM",
                itemType = HomeItemType.TELEMETRY_CAPSULE,
                spanCols = 2,
                spanRows = 2,
                config = ModularWidgetConfig(
                    mechaTheme = MechaWidgetTheme.DATA_STREAM,
                    telemetryMetric = TelemetryMetric.NET_SPEED,
                    tapAction = KansoActionId.WIFI,
                    showAudioVisualizer = true
                )
            ),
            KansoWidgetTemplate(
                id = "telemetry_ram",
                title = "Telemetry Capsule — RAM Pressure",
                subtitle = "2×2 • Live Memory Pressure Ring (Add multiple!)",
                itemType = HomeItemType.TELEMETRY_CAPSULE,
                spanCols = 2,
                spanRows = 2,
                config = ModularWidgetConfig(
                    telemetryMetric = TelemetryMetric.RAM_USAGE,
                    tapAction = KansoActionId.OPEN_SETTINGS,
                    showAudioVisualizer = false
                )
            ),
            KansoWidgetTemplate(
                id = "telemetry_ssd",
                title = "Telemetry Capsule — Internal Storage",
                subtitle = "2×2 • Live Storage Capacity Ring",
                itemType = HomeItemType.TELEMETRY_CAPSULE,
                spanCols = 2,
                spanRows = 2,
                config = ModularWidgetConfig(
                    telemetryMetric = TelemetryMetric.STORAGE_USAGE,
                    tapAction = KansoActionId.OPEN_SETTINGS,
                    showAudioVisualizer = false
                )
            ),
            KansoWidgetTemplate(
                id = "matrix_2x2",
                title = "2×2 Radio Action Matrix",
                subtitle = "2×2 • Wi-Fi, Data, Bluetooth, Hotspot",
                itemType = HomeItemType.ACTION_MATRIX,
                spanCols = 2,
                spanRows = 2,
                config = ModularWidgetConfig(
                    matrixRows = 2,
                    matrixCols = 2,
                    matrixTiles = ModularWidgetConfig.defaultMatrixTiles(4)
                )
            ),
            KansoWidgetTemplate(
                id = "matrix_4x4",
                title = "4×4 Full Mecha Control Grid",
                subtitle = "4×3 • 16 configurable hardware & app triggers",
                itemType = HomeItemType.ACTION_MATRIX,
                spanCols = 4,
                spanRows = 3,
                config = ModularWidgetConfig(
                    matrixRows = 4,
                    matrixCols = 4,
                    matrixTiles = ModularWidgetConfig.defaultMatrixTiles(16)
                )
            ),
            KansoWidgetTemplate(
                id = "toggle_silent",
                title = "3-State Sound Switch",
                subtitle = "2×1 • Spring animated Ring / Vibrate / Silent",
                itemType = HomeItemType.TOGGLE_SWITCH,
                spanCols = 2,
                spanRows = 1,
                config = ModularWidgetConfig(
                    mechaTheme = MechaWidgetTheme.DEFAULT,
                    toggleAction = KansoActionId.SILENT_MODE,
                    customLabel = "SOUND"
                )
            ),
            KansoWidgetTemplate(
                id = "toggle_torch",
                title = "Animated Switch — Flashlight",
                subtitle = "2×1 • Spring sliding mecha toggle",
                itemType = HomeItemType.TOGGLE_SWITCH,
                spanCols = 2,
                spanRows = 1,
                config = ModularWidgetConfig(toggleAction = KansoActionId.TORCH, customLabel = "TORCH")
            ),
            KansoWidgetTemplate(
                id = "toggle_rotate",
                title = "Animated Switch — Auto-Rotate",
                subtitle = "2×1 • Orientation lock toggle",
                itemType = HomeItemType.TOGGLE_SWITCH,
                spanCols = 2,
                spanRows = 1,
                config = ModularWidgetConfig(toggleAction = KansoActionId.AUTO_ROTATE, customLabel = "ROTATE")
            ),
            KansoWidgetTemplate(
                id = "media_hud",
                title = "Media HUD Card",
                subtitle = "4×1 • Live Spotify, YT, Apple & Prime Music",
                itemType = HomeItemType.MEDIA_HUD,
                spanCols = 4,
                spanRows = 1
            ),
            KansoWidgetTemplate(
                id = "weather_badge",
                title = "Scalloped Weather Medallion",
                subtitle = "2×2 • Location-synced Open-Meteo",
                itemType = HomeItemType.WEATHER_BADGE,
                spanCols = 2,
                spanRows = 2
            ),
            KansoWidgetTemplate(
                id = "calendar_ribbon",
                title = "7-Day Calendar Ribbon",
                subtitle = "4×1 • Full-width tactile week strip",
                itemType = HomeItemType.CALENDAR_RIBBON,
                spanCols = 4,
                spanRows = 1
            )
        )
    }

    val accentBlockTemplates = remember {
        AccentBlockStyle.entries.map { style ->
            val (cols, rows) = when (style) {
                AccentBlockStyle.S_CURVE_SHELF -> 4 to 2
                AccentBlockStyle.CHAMFERED_PLATE -> 2 to 2
                AccentBlockStyle.DIAGONAL_SLAB -> 4 to 1
                AccentBlockStyle.CORNER_ARMOR -> 2 to 2
                AccentBlockStyle.STEPPED_MONOLITH -> 3 to 2
                AccentBlockStyle.SPLIT_CHEVRON -> 4 to 1
                AccentBlockStyle.PILL_BAR -> 4 to 1
            }
            KansoWidgetTemplate(
                id = "accent_block_${style.name.lowercase()}",
                title = style.title,
                subtitle = "${cols}×${rows} • ${style.shortCode} architectural plate (Accent color only)",
                badge = "ACCENT BLOCK // LAYER 0",
                itemType = HomeItemType.ACCENT_BLOCK,
                spanCols = cols,
                spanRows = rows,
                config = ModularWidgetConfig(accentBlockStyle = style)
            )
        }
    }

    val tabs = remember(allowExternalWidgets) {
        if (allowExternalWidgets) {
            listOf("Widgets", "Accent Blocks", "Android")
        } else {
            listOf("Widgets", "Accent Blocks")
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .testTag("widget_add_preview_dialog")
                .fillMaxWidth()
                .kansoNeumorphic(raised = true, cornerRadius = 28.dp)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Widget & Block Catalog",
                        color = colors.inkPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "NEUMORPHIC & MECHA MODULES • ACCENT BLOCKS",
                        color = colors.accent,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp,
                        letterSpacing = 1.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = colors.inkSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .kansoNeumorphic(raised = false, cornerRadius = 16.dp)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEachIndexed { idx, label ->
                    val selected = activeTab == idx
                    Box(
                        modifier = Modifier
                            .testTag("widget_picker_tab_$idx")
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) colors.accent else Color.Transparent)
                            .kansoTactileClickable { activeTab = idx },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) colors.onAccent else colors.inkPrimary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (activeTab == 0 || activeTab == 1) {
                val activeList = if (activeTab == 0) templates else accentBlockTemplates
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 430.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(activeList, key = { it.id }) { tmpl ->
                        Column(
                            modifier = Modifier
                                .testTag("widget_template_${tmpl.id}")
                                .fillMaxWidth()
                                .kansoNeumorphic(raised = false, cornerRadius = 20.dp)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (tmpl.badge.isNotBlank()) {
                                Text(
                                    text = tmpl.badge,
                                    color = colors.accent,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.8.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 66.dp, max = 144.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when (tmpl.itemType) {
                                    HomeItemType.ACCENT_BLOCK -> AccentColorBlockWidget(
                                        style = tmpl.config.accentBlockStyle,
                                        flipped = tmpl.config.accentBlockFlipped,
                                        rotated = tmpl.config.accentBlockRotated,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(92.dp)
                                    )
                                    HomeItemType.TELEMETRY_CAPSULE -> NetworkTelemetryWidget(
                                        readout = telemetry.readoutFor(tmpl.config.telemetryMetric),
                                        metric = tmpl.config.telemetryMetric,
                                        mechaTheme = tmpl.config.mechaTheme,
                                        audioBands = audioBands,
                                        isAudioActive = media.isPlaying,
                                        showAudioVisualizer = tmpl.config.showAudioVisualizer,
                                        modifier = Modifier.size(width = 162.dp, height = 140.dp)
                                    )
                                    HomeItemType.ACTION_MATRIX -> ActionMatrixWidget(
                                        rows = tmpl.config.matrixRows,
                                        cols = tmpl.config.matrixCols,
                                        tiles = tmpl.config.matrixTiles,
                                        telemetry = telemetry,
                                        isMediaPlaying = media.isPlaying,
                                        mechaTheme = tmpl.config.mechaTheme,
                                        onTileClick = {},
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(if (tmpl.config.matrixRows >= 3) 136.dp else 108.dp)
                                    )
                                    HomeItemType.TOGGLE_SWITCH -> ModularToggleSwitchWidget(
                                        action = tmpl.config.toggleAction,
                                        customLabel = tmpl.config.customLabel,
                                        telemetry = telemetry,
                                        isMediaPlaying = media.isPlaying,
                                        mechaTheme = tmpl.config.mechaTheme,
                                        onToggle = {},
                                        modifier = Modifier
                                            .width(184.dp)
                                            .height(54.dp)
                                    )
                                    HomeItemType.MEDIA_HUD -> MediaHudWidget(
                                        media = media,
                                        mechaTheme = tmpl.config.mechaTheme,
                                        onPlayPause = {},
                                        onPrevious = {},
                                        onNext = {},
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    HomeItemType.WEATHER_BADGE -> WeatherBadgeWidget(
                                        weather = weather,
                                        mechaTheme = tmpl.config.mechaTheme,
                                        onClick = {}
                                    )
                                    HomeItemType.CALENDAR_RIBBON -> CalendarRibbonWidget(
                                        days = telemetry.weekDays,
                                        mechaTheme = tmpl.config.mechaTheme,
                                        onClick = {}
                                    )
                                    else -> BatteryCapsuleWidget(
                                        batteryPercent = telemetry.batteryPercent,
                                        isCharging = telemetry.isCharging,
                                        mechaTheme = tmpl.config.mechaTheme
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tmpl.title,
                                        color = colors.inkPrimary,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = tmpl.subtitle,
                                        color = colors.inkSecondary,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 9.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(colors.accent)
                                        .kansoTactileClickable {
                                            onAddKansoWidget(tmpl)
                                            onDismiss()
                                        }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = "PLACE",
                                        color = colors.onAccent,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                ExternalWidgetPickerList(
                    providers = providers,
                    onSelectProvider = {
                        onSelectExternalProvider(it)
                        onDismiss()
                    },
                    modifier = Modifier.heightIn(max = 430.dp)
                )
            }
        }
    }
}

@Composable
fun ExternalWidgetPickerList(
    providers: List<AppWidgetProviderInfo>,
    onSelectProvider: (AppWidgetProviderInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalKansoColors.current
    val pm = context.packageManager
    val densityDpi = context.resources.displayMetrics.densityDpi

    if (providers.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No external Android widgets installed on this device.",
                color = colors.inkSecondary,
                fontSize = 13.sp
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(providers, key = { it.provider.flattenToShortString() }) { info ->
            val label = remember(info) { info.loadLabel(pm) ?: info.provider.shortClassName }
            val pkgLabel = remember(info) {
                runCatching {
                    pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString()
                }.getOrDefault(info.provider.packageName)
            }
            val sizeTag = remember(info) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.targetCellWidth > 0 && info.targetCellHeight > 0) {
                    "${info.targetCellWidth}×${info.targetCellHeight} cells (${info.minWidth}×${info.minHeight}dp)"
                } else {
                    "${info.minWidth}×${info.minHeight}dp"
                }
            }
            var previewBitmap by remember(info.provider) { mutableStateOf<ImageBitmap?>(null) }

            LaunchedEffect(info.provider) {
                previewBitmap = withContext(Dispatchers.IO) {
                    runCatching {
                        val drawable = info.loadPreviewImage(context, densityDpi)
                            ?: info.loadIcon(context, densityDpi)
                        drawable?.toBitmap(
                            width = drawable.intrinsicWidth.coerceIn(96, 480),
                            height = drawable.intrinsicHeight.coerceIn(96, 320)
                        )?.asImageBitmap()
                    }.getOrNull()
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .kansoNeumorphic(raised = false, cornerRadius = 18.dp)
                    .kansoTactileClickable { onSelectProvider(info) }
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.canvasBg.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = previewBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = label,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Widgets,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
                            color = colors.inkPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$pkgLabel • $sizeTag",
                            color = colors.inkSecondary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add $label",
                            tint = colors.onAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
