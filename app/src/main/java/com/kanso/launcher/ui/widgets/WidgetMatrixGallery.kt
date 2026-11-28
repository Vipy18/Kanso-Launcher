package com.kanso.launcher.ui.widgets

import android.appwidget.AppWidgetProviderInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanso.launcher.data.db.HomeItemType
import com.kanso.launcher.data.db.SurfacePage
import com.kanso.launcher.data.model.AccentBlockStyle
import com.kanso.launcher.data.model.KansoActionId
import com.kanso.launcher.data.model.MechaWidgetTheme
import com.kanso.launcher.data.model.ModularWidgetConfig
import com.kanso.launcher.data.model.TelemetryMetric
import com.kanso.launcher.data.prefs.CachedWeather
import com.kanso.launcher.data.telemetry.TelemetryState
import com.kanso.launcher.service.MediaHudState
import com.kanso.launcher.ui.LauncherViewModel
import com.kanso.launcher.ui.theme.JetBrainsMonoFontFamily
import com.kanso.launcher.ui.theme.LocalKansoColors
import com.kanso.launcher.ui.theme.SpaceGroteskFontFamily
import com.kanso.launcher.ui.theme.kansoNeumorphic
import com.kanso.launcher.ui.theme.kansoTactileClickable
import kotlin.math.roundToInt

enum class StitchThemeFilter(
    val chipLabel: String,
    val profileTitle: String,
    val mappedTheme: MechaWidgetTheme?
) {
    ALL("ALL THEMES", "MULTI-THEME MATRIX", null),
    KANSO_HYBRID("[KANSO HYBRID]", "KANSO HYBRID // TACTILE", MechaWidgetTheme.DEFAULT),
    PURE_NEUMORPH("[PURE NEUMORPH]", "PURE NEUMORPH // SOFT", MechaWidgetTheme.TACTICAL_GAUGE),
    MECHA_UNIT01("[MECHA // UNIT-01]", "MECHA // UNIT-01 ARMOR", MechaWidgetTheme.MACHINED_MECHA),
    CYBER_HUD("[CYBER // HUD]", "CYBER // HUD SCANLINE", MechaWidgetTheme.DATA_STREAM)
}

/**
 * Full-Screen Stitch Multi-Theme Widget Matrix Gallery accessible from Settings.
 * Previews all built-in Kanso widgets live across the 4 Stitch themes, lets the user tune
 * bevel depth & HUD glow physics, apply a theme across workspace widgets, or drop any widget
 * directly onto Workspace 1.
 */
@Composable
fun WidgetMatrixScreen(
    viewModel: LauncherViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val colors = LocalKansoColors.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val media by viewModel.mediaState.collectAsStateWithLifecycle()
    val audioBands by viewModel.audioBands.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf(StitchThemeFilter.KANSO_HYBRID) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    val activeTheme = selectedFilter.mappedTheme ?: MechaWidgetTheme.DEFAULT

    Column(
        modifier = Modifier
            .testTag("widget_matrix_screen")
            .fillMaxSize()
            .background(colors.canvasBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Command Telemetry Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .testTag("widget_matrix_back")
                        .size(44.dp)
                        .kansoNeumorphic(raised = true, cornerRadius = 22.dp)
                        .kansoTactileClickable(onClick = onNavigateBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.inkPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "SYS.MODULE // v4.8 CATALOG",
                        color = colors.accent,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "WIDGET MATRIX",
                        color = colors.inkPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = (-0.4).sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.surfaceInset)
                    .border(1.dp, colors.accent.copy(alpha = 0.45f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "${telemetry.batteryPercent}% // LIVE",
                    color = colors.accent,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            }
        }

        // Theme Filter Ribbon
        StitchThemeFilterRibbon(
            selected = selectedFilter,
            onSelect = { selectedFilter = it },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Active Profile Status Pill + Toast Feedback
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .kansoNeumorphic(raised = false, cornerRadius = 16.dp)
                .padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (selectedFilter == StitchThemeFilter.CYBER_HUD) colors.secondaryTelemetry
                            else colors.accent
                        )
                )
                Text(
                    text = statusBanner ?: "ACTIVE PROFILE: ${selectedFilter.profileTitle}",
                    color = colors.inkPrimary,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "11 FAMILIES",
                color = colors.inkSecondary,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Main Matrix Catalog + Physics Controls
        WidgetMatrixCatalogContent(
            telemetry = telemetry,
            media = media,
            weather = settings.weather,
            audioBands = audioBands,
            use24HourClock = settings.use24HourClock,
            selectedFilter = selectedFilter,
            bevelDepth = settings.widgetBevelDepth,
            hudGlow = settings.widgetHudGlow,
            showPhysicsControls = true,
            onBevelDepthChange = viewModel::setWidgetBevelDepth,
            onHudGlowChange = viewModel::setWidgetHudGlow,
            onApplyThemeToAll = {
                viewModel.applyThemeToAllWorkspaceWidgets(activeTheme)
                statusBanner = "APPLIED ${selectedFilter.chipLabel} TO WORKSPACE"
            },
            onSelectRingerMode = viewModel::setRingerMode,
            onSetBrightness = { viewModel.telemetryRepository.setBrightnessFraction(it) },
            onSetVolume = { viewModel.telemetryRepository.setMediaVolumeFraction(it) },
            onAddWidget = { tmpl ->
                viewModel.addModularWidget(
                    itemType = tmpl.itemType,
                    label = tmpl.title,
                    spanCols = tmpl.spanCols,
                    spanRows = tmpl.spanRows,
                    config = tmpl.config,
                    pageIndex = SurfacePage.WORKSPACE
                )
                statusBanner = "PLACED ${tmpl.title.uppercase()} ON WORKSPACE"
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}

@Composable
fun StitchThemeFilterRibbon(
    selected: StitchThemeFilter,
    onSelect: (StitchThemeFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalKansoColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StitchThemeFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            val chipAccent = if (filter == StitchThemeFilter.CYBER_HUD) colors.secondaryTelemetry else colors.accent
            val chipOnAccent = if (filter == StitchThemeFilter.CYBER_HUD) Color(0xFF00222A) else colors.onAccent
            Box(
                modifier = Modifier
                    .testTag("matrix_filter_${filter.name.lowercase()}")
                    .clip(CircleShape)
                    .background(if (isSelected) chipAccent else colors.surfaceRaised)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) chipAccent else colors.trackMuted.copy(alpha = 0.45f),
                        shape = CircleShape
                    )
                    .kansoTactileClickable { onSelect(filter) }
                    .padding(horizontal = 13.dp, vertical = 8.dp)
            ) {
                Text(
                    text = filter.chipLabel,
                    color = if (isSelected) chipOnAccent else colors.inkPrimary,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp
                )
            }
        }
    }
}

fun buildStitchWidgetTemplates(activeTheme: MechaWidgetTheme): List<KansoWidgetTemplate> {
    val themeTag = when (activeTheme) {
        MechaWidgetTheme.DEFAULT -> "KANSO HYBRID"
        MechaWidgetTheme.TACTICAL_GAUGE -> "PURE NEUMORPH"
        MechaWidgetTheme.MACHINED_MECHA -> "MECHA // UNIT-01"
        MechaWidgetTheme.REACTOR_CORE -> "REACTOR CORE"
        MechaWidgetTheme.DATA_STREAM -> "CYBER // HUD"
    }
    return listOf(
        KansoWidgetTemplate(
            id = "stitch_clock_${activeTheme.name}",
            title = "Chrono Clock & Date Unit",
            subtitle = "4×1 • Live clock, date & timezone badge ($themeTag)",
            badge = "01 // CLOCK & DATE • $themeTag",
            itemType = HomeItemType.STITCH_CLOCK,
            spanCols = 4,
            spanRows = 1,
            config = ModularWidgetConfig(mechaTheme = activeTheme)
        ),
        KansoWidgetTemplate(
            id = "tri_state_ringer_${activeTheme.name}",
            title = "3-Position Ringer Switch",
            subtitle = "4×1 • Direct Silent / Vibrate / Ring selector",
            badge = "02 // TACTILE SWITCH • $themeTag",
            itemType = HomeItemType.TRI_STATE_SWITCH,
            spanCols = 4,
            spanRows = 1,
            config = ModularWidgetConfig(mechaTheme = activeTheme)
        ),
        KansoWidgetTemplate(
            id = "dual_slider_capsule_${activeTheme.name}",
            title = "Analog Luminance & Volume Deck",
            subtitle = "4×1 • Live brightness & media audio bus sliders",
            badge = "03 // ANALOG SLIDERS • $themeTag",
            itemType = HomeItemType.DUAL_SLIDER_CAPSULE,
            spanCols = 4,
            spanRows = 1,
            config = ModularWidgetConfig(mechaTheme = activeTheme)
        ),
        KansoWidgetTemplate(
            id = "telemetry_net_${activeTheme.name}",
            title = "Network & Audio Telemetry Pod",
            subtitle = "2×2 • Tilted live speed gauge + dot visualizer",
            badge = "04 // VITALS TELEMETRY • $themeTag",
            itemType = HomeItemType.TELEMETRY_CAPSULE,
            spanCols = 2,
            spanRows = 2,
            config = ModularWidgetConfig(
                mechaTheme = activeTheme,
                telemetryMetric = TelemetryMetric.NET_SPEED,
                tapAction = KansoActionId.WIFI,
                showAudioVisualizer = true
            )
        ),
        KansoWidgetTemplate(
            id = "telemetry_ram_${activeTheme.name}",
            title = "RAM Pressure Telemetry Pod",
            subtitle = "2×2 • Live memory allocation ring",
            badge = "04 // RAM VITALS • $themeTag",
            itemType = HomeItemType.TELEMETRY_CAPSULE,
            spanCols = 2,
            spanRows = 2,
            config = ModularWidgetConfig(
                mechaTheme = activeTheme,
                telemetryMetric = TelemetryMetric.RAM_USAGE,
                tapAction = KansoActionId.OPEN_SETTINGS,
                showAudioVisualizer = false
            )
        ),
        KansoWidgetTemplate(
            id = "telemetry_cpu_${activeTheme.name}",
            title = "System Load Reactor Pod",
            subtitle = "2×2 • Live CPU/system load gauge",
            badge = "04 // CPU VITALS • $themeTag",
            itemType = HomeItemType.TELEMETRY_CAPSULE,
            spanCols = 2,
            spanRows = 2,
            config = ModularWidgetConfig(
                mechaTheme = activeTheme,
                telemetryMetric = TelemetryMetric.CPU_LOAD,
                tapAction = KansoActionId.BATTERY_SETTINGS,
                showAudioVisualizer = true
            )
        ),
        KansoWidgetTemplate(
            id = "matrix_4x2_${activeTheme.name}",
            title = "4×2 Command Button Deck",
            subtitle = "4×2 • 8 configurable hardware & app triggers",
            badge = "05 // BUTTON MATRIX • $themeTag",
            itemType = HomeItemType.ACTION_MATRIX,
            spanCols = 4,
            spanRows = 2,
            config = ModularWidgetConfig(
                mechaTheme = activeTheme,
                matrixRows = 2,
                matrixCols = 4,
                matrixTiles = ModularWidgetConfig.defaultMatrixTiles(8)
            )
        ),
        KansoWidgetTemplate(
            id = "matrix_2x2_${activeTheme.name}",
            title = "2×2 Radio Action Matrix",
            subtitle = "2×2 • Wi-Fi, Data, Bluetooth, Hotspot",
            badge = "05 // RADIO MATRIX • $themeTag",
            itemType = HomeItemType.ACTION_MATRIX,
            spanCols = 2,
            spanRows = 2,
            config = ModularWidgetConfig(
                mechaTheme = activeTheme,
                matrixRows = 2,
                matrixCols = 2,
                matrixTiles = ModularWidgetConfig.defaultMatrixTiles(4)
            )
        ),
        KansoWidgetTemplate(
            id = "toggle_torch_${activeTheme.name}",
            title = "2-Position Tactile Switch (Torch)",
            subtitle = "2×1 • Spring-loaded hardware flashlight toggle",
            badge = "02 // 2-POS SWITCH • $themeTag",
            itemType = HomeItemType.TOGGLE_SWITCH,
            spanCols = 2,
            spanRows = 1,
            config = ModularWidgetConfig(
                mechaTheme = activeTheme,
                toggleAction = KansoActionId.TORCH,
                customLabel = "TORCH"
            )
        ),
        KansoWidgetTemplate(
            id = "battery_capsule_${activeTheme.name}",
            title = "Dot-Matrix Battery Capsule",
            subtitle = "2×1 • Live charge percentage & dot gauge",
            badge = "04 // POWER CELL • $themeTag",
            itemType = HomeItemType.BATTERY_CAPSULE,
            spanCols = 2,
            spanRows = 1,
            config = ModularWidgetConfig(mechaTheme = activeTheme)
        ),
        KansoWidgetTemplate(
            id = "media_hud_${activeTheme.name}",
            title = "Media HUD Player Deck",
            subtitle = "4×1 • Live Spotify, YT, Apple & Prime Music",
            badge = "06 // MEDIA DECK • $themeTag",
            itemType = HomeItemType.MEDIA_HUD,
            spanCols = 4,
            spanRows = 1,
            config = ModularWidgetConfig(mechaTheme = activeTheme)
        ),
        KansoWidgetTemplate(
            id = "weather_badge_${activeTheme.name}",
            title = "Atmospheric Weather Medallion",
            subtitle = "2×2 • Location-synced Open-Meteo telemetry",
            badge = "07 // WEATHER • $themeTag",
            itemType = HomeItemType.WEATHER_BADGE,
            spanCols = 2,
            spanRows = 2,
            config = ModularWidgetConfig(mechaTheme = activeTheme)
        ),
        KansoWidgetTemplate(
            id = "calendar_ribbon_${activeTheme.name}",
            title = "7-Day Calendar Ribbon",
            subtitle = "4×1 • Full-width tactile week strip",
            badge = "08 // CALENDAR • $themeTag",
            itemType = HomeItemType.CALENDAR_RIBBON,
            spanCols = 4,
            spanRows = 1,
            config = ModularWidgetConfig(mechaTheme = activeTheme)
        )
    )
}

@Composable
fun WidgetMatrixCatalogContent(
    telemetry: TelemetryState,
    media: MediaHudState,
    weather: CachedWeather,
    audioBands: List<Float>,
    use24HourClock: Boolean,
    selectedFilter: StitchThemeFilter,
    bevelDepth: Float = 1.0f,
    hudGlow: Float = 0.65f,
    showPhysicsControls: Boolean = false,
    onBevelDepthChange: (Float) -> Unit = {},
    onHudGlowChange: (Float) -> Unit = {},
    onApplyThemeToAll: () -> Unit = {},
    onSelectRingerMode: (Int) -> Unit = {},
    onSetBrightness: (Float) -> Unit = {},
    onSetVolume: (Float) -> Unit = {},
    onAddWidget: (KansoWidgetTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalKansoColors.current
    val effectiveTheme = selectedFilter.mappedTheme ?: MechaWidgetTheme.DEFAULT
    val templates = remember(selectedFilter) {
        if (selectedFilter == StitchThemeFilter.ALL) {
            val themesCycle = listOf(
                MechaWidgetTheme.DEFAULT,
                MechaWidgetTheme.MACHINED_MECHA,
                MechaWidgetTheme.DATA_STREAM,
                MechaWidgetTheme.TACTICAL_GAUGE
            )
            buildStitchWidgetTemplates(MechaWidgetTheme.DEFAULT).mapIndexed { index, tmpl ->
                val t = themesCycle[index % themesCycle.size]
                tmpl.copy(
                    id = "${tmpl.id}_all_$index",
                    config = tmpl.config.copy(mechaTheme = t)
                )
            }
        } else {
            buildStitchWidgetTemplates(effectiveTheme)
        }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(templates, key = { it.id }) { tmpl ->
            Column(
                modifier = Modifier
                    .testTag("matrix_card_${tmpl.id}")
                    .fillMaxWidth()
                    .kansoNeumorphic(raised = false, cornerRadius = 22.dp)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tmpl.badge.ifBlank { "WIDGET MODULE" },
                        color = if (tmpl.config.mechaTheme == MechaWidgetTheme.DATA_STREAM) {
                            colors.secondaryTelemetry
                        } else {
                            colors.accent
                        },
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "${tmpl.spanCols}×${tmpl.spanRows}",
                        color = colors.inkSecondary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    when (tmpl.itemType) {
                        HomeItemType.STITCH_CLOCK -> StitchClockWidget(
                            clock = telemetry.clock,
                            use24Hour = use24HourClock,
                            mechaTheme = tmpl.config.mechaTheme
                        )
                        HomeItemType.TRI_STATE_SWITCH -> TriStateRingerWidget(
                            ringerMode = telemetry.ringerMode,
                            mechaTheme = tmpl.config.mechaTheme,
                            onSelectRingerMode = onSelectRingerMode
                        )
                        HomeItemType.DUAL_SLIDER_CAPSULE -> DualSliderCapsuleWidget(
                            brightnessFraction = telemetry.brightnessFraction,
                            volumeFraction = telemetry.mediaVolumeFraction,
                            mechaTheme = tmpl.config.mechaTheme,
                            onBrightnessChange = onSetBrightness,
                            onVolumeChange = onSetVolume
                        )
                        HomeItemType.TELEMETRY_CAPSULE -> NetworkTelemetryWidget(
                            readout = telemetry.readoutFor(tmpl.config.telemetryMetric),
                            metric = tmpl.config.telemetryMetric,
                            mechaTheme = tmpl.config.mechaTheme,
                            audioBands = audioBands,
                            isAudioActive = media.isPlaying,
                            showAudioVisualizer = tmpl.config.showAudioVisualizer,
                            modifier = Modifier.size(width = 166.dp, height = 144.dp)
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
                                .height(if (tmpl.config.matrixRows >= 3) 136.dp else 112.dp)
                        )
                        HomeItemType.TOGGLE_SWITCH -> ModularToggleSwitchWidget(
                            action = tmpl.config.toggleAction,
                            customLabel = tmpl.config.customLabel,
                            telemetry = telemetry,
                            isMediaPlaying = media.isPlaying,
                            mechaTheme = tmpl.config.mechaTheme,
                            onToggle = {},
                            modifier = Modifier
                                .width(196.dp)
                                .height(54.dp)
                        )
                        HomeItemType.BATTERY_CAPSULE -> BatteryCapsuleWidget(
                            batteryPercent = telemetry.batteryPercent,
                            isCharging = telemetry.isCharging,
                            mechaTheme = tmpl.config.mechaTheme,
                            modifier = Modifier.width(210.dp)
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
                        else -> AccentColorBlockWidget(
                            style = tmpl.config.accentBlockStyle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(88.dp)
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
                            fontSize = 13.5.sp,
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
                    Row(
                        modifier = Modifier
                            .testTag("place_widget_${tmpl.id}")
                            .clip(CircleShape)
                            .background(colors.accent)
                            .kansoTactileClickable { onAddWidget(tmpl) }
                            .padding(horizontal = 13.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Place ${tmpl.title}",
                            tint = colors.onAccent,
                            modifier = Modifier.size(14.dp)
                        )
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

        if (showPhysicsControls) {
            item(key = "physics_tuner") {
                Column(
                    modifier = Modifier
                        .testTag("widget_physics_tuner")
                        .fillMaxWidth()
                        .kansoNeumorphic(raised = true, cornerRadius = 24.dp)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "HAPTIC & BEVEL PHYSICS",
                            color = colors.inkPrimary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Extrusion Bevel Depth",
                                color = colors.inkPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${(bevelDepth * 100f).roundToInt()}%",
                                color = colors.accent,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Slider(
                            value = bevelDepth,
                            onValueChange = onBevelDepthChange,
                            valueRange = 0.4f..1.8f,
                            colors = SliderDefaults.colors(
                                thumbColor = colors.accent,
                                activeTrackColor = colors.accent,
                                inactiveTrackColor = colors.surfaceInset
                            )
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "HUD Telemetry Glow",
                                color = colors.inkPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${(hudGlow * 100f).roundToInt()}%",
                                color = colors.secondaryTelemetry,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Slider(
                            value = hudGlow,
                            onValueChange = onHudGlowChange,
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = colors.secondaryTelemetry,
                                activeTrackColor = colors.secondaryTelemetry,
                                inactiveTrackColor = colors.surfaceInset
                            )
                        )
                    }

                    Row(
                        modifier = Modifier
                            .testTag("apply_theme_all_widgets_btn")
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(colors.accent)
                            .kansoTactileClickable(onClick = onApplyThemeToAll),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = colors.onAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "APPLY ${selectedFilter.chipLabel} TO ALL WIDGETS",
                            color = colors.onAccent,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.6.sp
                        )
                    }
                }
            }
        }
    }
}

fun buildAccentBlockTemplates(): List<KansoWidgetTemplate> =
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
