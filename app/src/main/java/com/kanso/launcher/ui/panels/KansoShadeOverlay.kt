package com.kanso.launcher.ui.panels

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kanso.launcher.data.apps.InstalledApp
import com.kanso.launcher.data.db.IconOverrideEntity
import com.kanso.launcher.data.iconpacks.IconPackManager
import com.kanso.launcher.data.model.ActionTileBinding
import com.kanso.launcher.data.model.KansoActionId
import com.kanso.launcher.data.prefs.IconPackMode
import com.kanso.launcher.data.telemetry.TelemetryState
import com.kanso.launcher.service.KansoNotificationItem
import com.kanso.launcher.service.KansoPerformanceGovernor
import com.kanso.launcher.service.MediaHudState
import com.kanso.launcher.ui.home.ActionTileBindingDialog
import com.kanso.launcher.ui.theme.JetBrainsMonoFontFamily
import com.kanso.launcher.ui.theme.LocalKansoColors
import com.kanso.launcher.ui.theme.SpaceGroteskFontFamily
import com.kanso.launcher.ui.theme.kansoMechaAccents
import com.kanso.launcher.ui.theme.kansoNeumorphic
import com.kanso.launcher.ui.theme.kansoTactileClickable
import com.kanso.launcher.ui.widgets.KansoAppIcon
import com.kanso.launcher.ui.widgets.iconForAction
import com.kanso.launcher.ui.widgets.statusLabelForAction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class ShadeStage {
    CLOSED,
    NOTIFICATIONS,
    CONTROLS
}

/**
 * Stitch Tactile Cyber-Mecha HUD Split Shade:
 * - Left Swipe-Down -> Notification Panel (Sweeping Amber canopy + brightness, 4-tile quick strip,
 *   rich Media HUD deck, color-coded active notification stack, filter dock).
 * - Right Swipe-Down -> Control Panel (Sweeping Amber canopy + brightness, Cyber Cyan Audio Bus
 *   slider + live output router, customizable 3-column Hardware Matrix with Edit Mode).
 */
@Composable
fun KansoShadeOverlay(
    stage: ShadeStage,
    notifications: List<KansoNotificationItem>,
    notificationAccessGranted: Boolean,
    telemetry: TelemetryState,
    media: MediaHudState,
    controlTiles: List<ActionTileBinding>,
    apps: List<InstalledApp> = emptyList(),
    iconPackManager: IconPackManager? = null,
    iconPackMode: IconPackMode = IconPackMode.KANSO_ACCENT,
    selectedIconPackPackage: String = "",
    overrides: Map<String, IconOverrideEntity> = emptyMap(),
    onStageChange: (ShadeStage) -> Unit,
    onOpenNotificationAccessSettings: () -> Unit,
    onDismissNotification: (String) -> Unit,
    onClearAllNotifications: () -> Unit,
    onExecuteTile: (ActionTileBinding) -> Unit,
    onAddControlTile: (ActionTileBinding) -> Unit,
    onUpdateControlTile: (Int, ActionTileBinding) -> Unit,
    onRemoveControlTile: (Int) -> Unit,
    onResetControlTiles: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onPlayPause: () -> Unit,
    onPrevTrack: () -> Unit,
    onNextTrack: () -> Unit,
    onOpenPlayerApp: (String) -> Unit,
    onOpenAudioOutputSettings: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalKansoColors.current
    var dragAccum by remember { mutableFloatStateOf(0f) }
    var isEditingControls by remember { mutableStateOf(false) }

    BackHandler(enabled = stage != ShadeStage.CLOSED) {
        if (isEditingControls) {
            isEditingControls = false
        } else {
            onStageChange(ShadeStage.CLOSED)
        }
    }

    AnimatedVisibility(
        visible = stage != ShadeStage.CLOSED,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(dampingRatio = 0.86f, stiffness = 480f)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = spring(dampingRatio = 0.92f, stiffness = 520f)
        ) + fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .testTag("kanso_shade_overlay")
                .fillMaxSize()
                .background(colors.canvasBg.copy(alpha = 0.98f))
                .pointerInput(stage) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            dragAccum = 0f
                            KansoPerformanceGovernor.notifyAnimationBurst(600L)
                        },
                        onVerticalDrag = { _, dragAmount -> dragAccum += dragAmount },
                        onDragEnd = {
                            if (dragAccum > 100f && stage == ShadeStage.NOTIFICATIONS) {
                                onStageChange(ShadeStage.CONTROLS)
                            } else if (dragAccum < -95f) {
                                isEditingControls = false
                                onStageChange(ShadeStage.CLOSED)
                            }
                            dragAccum = 0f
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                // Sweeping Signal Amber Rounded-Bottom Canopy with Embedded Luminance Slider
                StitchAmberCanopyHeader(
                    stage = stage,
                    telemetry = telemetry,
                    notificationCount = notifications.size,
                    isEditingControls = isEditingControls,
                    onToggleEditControls = { isEditingControls = !isEditingControls },
                    onSwitchStage = {
                        isEditingControls = false
                        onStageChange(
                            if (stage == ShadeStage.NOTIFICATIONS) ShadeStage.CONTROLS
                            else ShadeStage.NOTIFICATIONS
                        )
                    },
                    onClearNotifications = onClearAllNotifications,
                    onOpenSettings = {
                        isEditingControls = false
                        onStageChange(ShadeStage.CLOSED)
                        onOpenSettings()
                    },
                    onClose = {
                        isEditingControls = false
                        onStageChange(ShadeStage.CLOSED)
                    },
                    onBrightnessChange = onBrightnessChange
                )

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedContent(
                    targetState = stage,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    label = "stitchShadeBody"
                ) { activeStage ->
                    if (activeStage == ShadeStage.NOTIFICATIONS) {
                        StitchNotificationPanelBody(
                            notifications = notifications,
                            notificationAccessGranted = notificationAccessGranted,
                            telemetry = telemetry,
                            media = media,
                            onExecuteTile = onExecuteTile,
                            onOpenNotificationAccessSettings = onOpenNotificationAccessSettings,
                            onDismissNotification = onDismissNotification,
                            onOpenAudioOutputSettings = onOpenAudioOutputSettings,
                            onPlayPause = onPlayPause,
                            onPrevTrack = onPrevTrack,
                            onNextTrack = onNextTrack,
                            onOpenPlayerApp = onOpenPlayerApp,
                            onCloseShade = { onStageChange(ShadeStage.CLOSED) }
                        )
                    } else {
                        StitchControlPanelBody(
                            telemetry = telemetry,
                            isMediaPlaying = media.isPlaying,
                            controlTiles = controlTiles,
                            isEditing = isEditingControls,
                            apps = apps,
                            iconPackManager = iconPackManager,
                            iconPackMode = iconPackMode,
                            selectedIconPackPackage = selectedIconPackPackage,
                            overrides = overrides,
                            onExecuteTile = onExecuteTile,
                            onAddControlTile = onAddControlTile,
                            onUpdateControlTile = onUpdateControlTile,
                            onRemoveControlTile = onRemoveControlTile,
                            onResetControlTiles = onResetControlTiles,
                            onVolumeChange = onVolumeChange,
                            onOpenAudioOutputSettings = onOpenAudioOutputSettings
                        )
                    }
                }

                // Bottom Recessed Grab Handle
                Box(
                    modifier = Modifier
                        .testTag("shade_stage_handle")
                        .fillMaxWidth()
                        .kansoTactileClickable {
                            isEditingControls = false
                            onStageChange(ShadeStage.CLOSED)
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(colors.inkSecondary.copy(alpha = 0.45f))
                    )
                }
            }
        }
    }
}

@Composable
private fun StitchAmberCanopyHeader(
    stage: ShadeStage,
    telemetry: TelemetryState,
    notificationCount: Int,
    isEditingControls: Boolean,
    onToggleEditControls: () -> Unit,
    onSwitchStage: () -> Unit,
    onClearNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    onBrightnessChange: (Float) -> Unit
) {
    val colors = LocalKansoColors.current
    val canopyShape = RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp)
    val dateShort = remember(telemetry.clock.formattedDate) {
        telemetry.clock.formattedDate.removePrefix("Today ").uppercase()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(canopyShape)
            .background(colors.accent)
    ) {
        // Subtle telemetry watermark lines in amber canopy
        Canvas(modifier = Modifier.matchParentSize()) {
            val lineColor = colors.onAccent.copy(alpha = 0.06f)
            val step = 18.dp.toPx()
            var x = -size.height
            while (x < size.width + size.height) {
                drawLine(
                    color = lineColor,
                    start = Offset(x, size.height),
                    end = Offset(x + size.height * 0.6f, 0f),
                    strokeWidth = 1.dp.toPx()
                )
                x += step
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(colors.onAccent)
                        )
                        Text(
                            text = "${telemetry.clock.hours24}:${telemetry.clock.minutes} // $dateShort",
                            color = colors.onAccent.copy(alpha = 0.82f),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = if (stage == ShadeStage.CONTROLS) {
                            if (isEditingControls) "EDIT MATRIX" else "CONTROL PANEL"
                        } else {
                            "NOTIFICATIONS"
                        },
                        color = colors.onAccent,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = (-0.4).sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (stage == ShadeStage.NOTIFICATIONS && notificationCount > 0) {
                        Row(
                            modifier = Modifier
                                .testTag("notifications_clear_all")
                                .height(36.dp)
                                .clip(CircleShape)
                                .background(colors.darkModuleBg)
                                .kansoTactileClickable(onClick = onClearNotifications)
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = "Clear all",
                                tint = colors.accent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "CLEAR",
                                color = Color(0xFFF2F3F5),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp
                            )
                        }
                    }

                    if (stage == ShadeStage.CONTROLS) {
                        Box(
                            modifier = Modifier
                                .testTag("control_panel_edit_toggle")
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isEditingControls) colors.onAccent else colors.darkModuleBg)
                                .kansoTactileClickable(onClick = onToggleEditControls),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isEditingControls) Icons.Rounded.Check else Icons.Rounded.Edit,
                                contentDescription = if (isEditingControls) "Done editing" else "Edit matrix",
                                tint = if (isEditingControls) colors.accent else Color(0xFFF2F3F5),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .testTag("shade_toggle_stage_button")
                            .height(36.dp)
                            .clip(CircleShape)
                            .background(colors.darkModuleBg)
                            .kansoTactileClickable(onClick = onSwitchStage)
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (stage == ShadeStage.NOTIFICATIONS) Icons.Rounded.Tune else Icons.Rounded.NotificationsActive,
                            contentDescription = "Switch shade panel",
                            tint = colors.accent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (stage == ShadeStage.NOTIFICATIONS) "CONTROLS" else "ALERTS",
                            color = Color(0xFFF2F3F5),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .testTag("shade_open_settings_button")
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.darkModuleBg)
                            .kansoTactileClickable(onClick = onOpenSettings),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFFF2F3F5),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .testTag("shade_close_button")
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.darkModuleBg.copy(alpha = 0.82f))
                            .kansoTactileClickable(onClick = onClose),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close shade",
                            tint = Color(0xFFF2F3F5),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Embedded Recessed Display Luminance Slider Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.darkModuleBg.copy(alpha = 0.92f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.WbSunny,
                    contentDescription = "Display Luminance",
                    tint = colors.accent,
                    modifier = Modifier.size(18.dp)
                )
                Slider(
                    value = telemetry.brightnessFraction.coerceIn(0.05f, 1f),
                    onValueChange = onBrightnessChange,
                    valueRange = 0.05f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accent,
                        activeTrackColor = colors.accent,
                        inactiveTrackColor = Color(0xFF343537)
                    ),
                    modifier = Modifier
                        .testTag("control_brightness_slider")
                        .weight(1f)
                        .height(34.dp)
                )
                Text(
                    text = "${(telemetry.brightnessFraction * 100f).roundToInt()}%",
                    color = colors.accent,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun StitchNotificationPanelBody(
    notifications: List<KansoNotificationItem>,
    notificationAccessGranted: Boolean,
    telemetry: TelemetryState,
    media: MediaHudState,
    onExecuteTile: (ActionTileBinding) -> Unit,
    onOpenNotificationAccessSettings: () -> Unit,
    onDismissNotification: (String) -> Unit,
    onOpenAudioOutputSettings: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevTrack: () -> Unit,
    onNextTrack: () -> Unit,
    onOpenPlayerApp: (String) -> Unit,
    onCloseShade: () -> Unit
) {
    val colors = LocalKansoColors.current
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var filterQuery by remember { mutableStateOf("") }

    val quickStripActions = remember {
        listOf(
            ActionTileBinding(action = KansoActionId.WIFI, customLabel = "WI-FI"),
            ActionTileBinding(action = KansoActionId.BLUETOOTH, customLabel = "BT"),
            ActionTileBinding(action = KansoActionId.SILENT_MODE, customLabel = "SOUND"),
            ActionTileBinding(action = KansoActionId.TORCH, customLabel = "TORCH")
        )
    }

    val filteredNotifications = remember(notifications, filterQuery) {
        if (filterQuery.isBlank()) notifications
        else {
            val q = filterQuery.trim().lowercase()
            notifications.filter {
                it.appName.lowercase().contains(q) ||
                    it.title.lowercase().contains(q) ||
                    it.text.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 4-Tile Hardware Quick Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickStripActions.forEach { binding ->
                val active = telemetry.isActionActive(binding.action, media.isPlaying)
                val icon = when (binding.action) {
                    KansoActionId.WIFI -> Icons.Rounded.Wifi
                    KansoActionId.BLUETOOTH -> Icons.Rounded.Bluetooth
                    KansoActionId.TORCH -> Icons.Rounded.FlashlightOn
                    else -> iconForAction(binding.action, telemetry, media.isPlaying)
                }
                Row(
                    modifier = Modifier
                        .testTag("notif_quick_${binding.action.name.lowercase()}")
                        .weight(1f)
                        .height(46.dp)
                        .kansoNeumorphic(
                            raised = true,
                            cornerRadius = 15.dp,
                            surfaceColor = if (active) colors.accent else colors.surfaceRaised
                        )
                        .kansoTactileClickable { onExecuteTile(binding) }
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = binding.customLabel,
                        tint = if (active) colors.onAccent else colors.inkPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = binding.customLabel,
                        color = if (active) colors.onAccent else colors.inkPrimary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Rich Stitch Media HUD Deck with Corner Brackets & Live Output Pill
        StitchShadeMediaCard(
            media = media,
            audioOutputLabel = telemetry.audioOutputLabel,
            notificationAccessGranted = notificationAccessGranted,
            onRequestMediaPermission = onOpenNotificationAccessSettings,
            onOpenAudioOutputSettings = onOpenAudioOutputSettings,
            onOpenPlayerApp = { pkg ->
                onCloseShade()
                onOpenPlayerApp(pkg)
            },
            onPlayPause = onPlayPause,
            onPrevTrack = onPrevTrack,
            onNextTrack = onNextTrack
        )

        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ACTIVE STREAM // ${filteredNotifications.size} ALERTS",
                color = colors.inkSecondary,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.sp
            )
            Text(
                text = "${telemetry.batteryPercent}% BAT",
                color = colors.accent,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }

        if (!notificationAccessGranted) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .kansoNeumorphic(raised = true, cornerRadius = 22.dp)
                    .kansoMechaAccents(accentActive = true, showCornerBrackets = true, showLedPip = true)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Grant Notification Access",
                    color = colors.inkPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Enable Kanso Notification Access to stream live alerts and sync Spotify, YouTube, Apple Music, and Prime Music here.",
                    color = colors.inkSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp
                )
                Box(
                    modifier = Modifier
                        .testTag("grant_notif_panel_button")
                        .clip(CircleShape)
                        .background(colors.accent)
                        .kansoTactileClickable(onClick = onOpenNotificationAccessSettings)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "ENABLE ACCESS",
                        color = colors.onAccent,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        } else if (filteredNotifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsOff,
                        contentDescription = null,
                        tint = colors.inkSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (filterQuery.isBlank()) "ALL CLEAR // NO ACTIVE ALERTS" else "NO MATCHING ALERTS",
                        color = colors.inkSecondary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.5.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 4.dp),
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(filteredNotifications, key = { _, it -> it.key }) { idx, item ->
                    val stripColor = when {
                        idx == 0 -> colors.accent
                        idx == 1 -> colors.secondaryTelemetry
                        else -> colors.inkSecondary.copy(alpha = 0.45f)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .kansoNeumorphic(raised = true, cornerRadius = 18.dp)
                            .kansoTactileClickable {
                                runCatching { item.contentIntent?.send() }
                                onCloseShade()
                            }
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Color-coded left accent strip
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .height(42.dp)
                                .clip(CircleShape)
                                .background(stripColor)
                        )
                        Spacer(modifier = Modifier.width(10.dp))

                        // Inset app initial badge well
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .kansoNeumorphic(raised = false, cornerRadius = 12.dp, surfaceColor = colors.surfaceInset),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.appName.take(2).uppercase(),
                                color = stripColor,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.appName.uppercase(),
                                    color = stripColor,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = timeFmt.format(Date(item.postTimeMs)),
                                    color = colors.inkSecondary,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.title,
                                color = colors.inkPrimary,
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (item.text.isNotBlank()) {
                                Text(
                                    text = item.text,
                                    color = colors.inkSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (item.isClearable) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceInset)
                                    .kansoTactileClickable { onDismissNotification(item.key) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Dismiss notification",
                                    tint = colors.inkSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recessed Notification Filter Bar
        StitchShadeFilterBar(
            value = filterQuery,
            placeholder = "FILTER ACTIVE ALERTS...",
            onValueChange = { filterQuery = it }
        )
    }
}

@Composable
private fun StitchShadeMediaCard(
    media: MediaHudState,
    audioOutputLabel: String,
    notificationAccessGranted: Boolean,
    onRequestMediaPermission: () -> Unit,
    onOpenAudioOutputSettings: () -> Unit,
    onOpenPlayerApp: (String) -> Unit,
    onPlayPause: () -> Unit,
    onPrevTrack: () -> Unit,
    onNextTrack: () -> Unit
) {
    val colors = LocalKansoColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .kansoNeumorphic(raised = true, cornerRadius = 22.dp)
            .kansoMechaAccents(accentActive = media.isPlaying, showCornerBrackets = true, showLedPip = true)
            .kansoTactileClickable {
                if (!notificationAccessGranted) onRequestMediaPermission()
                else if (media.packageName.isNotBlank()) onOpenPlayerApp(media.packageName)
            }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Inset Album Art Well
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .kansoNeumorphic(raised = false, cornerRadius = 15.dp, surfaceColor = colors.surfaceInset)
                    .clip(RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) {
                val art = media.albumArt
                if (art != null) {
                    Image(
                        bitmap = art.asImageBitmap(),
                        contentDescription = media.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (media.isPlaying) "PLAYING" else "MEDIA HUD",
                        color = colors.accent,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.5.sp,
                        letterSpacing = 0.8.sp
                    )
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.surfaceInset)
                            .kansoTactileClickable(onClick = onOpenAudioOutputSettings)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Headphones,
                            contentDescription = "Audio output",
                            tint = colors.secondaryTelemetry,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = audioOutputLabel,
                            color = colors.secondaryTelemetry,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = media.title.ifBlank { "No Active Media" },
                    color = colors.inkPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = media.artist.ifBlank {
                        if (notificationAccessGranted) "Start Spotify, YouTube or Music" else "Tap to enable media sync"
                    },
                    color = colors.inkSecondary,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Transport buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .kansoNeumorphic(raised = true, cornerRadius = 17.dp)
                        .kansoTactileClickable(onClick = onPrevTrack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = colors.inkPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                        .kansoTactileClickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (media.isPlaying) "Pause" else "Play",
                        tint = colors.onAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .kansoNeumorphic(raised = true, cornerRadius = 17.dp)
                        .kansoTactileClickable(onClick = onNextTrack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = colors.inkPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StitchControlPanelBody(
    telemetry: TelemetryState,
    isMediaPlaying: Boolean,
    controlTiles: List<ActionTileBinding>,
    isEditing: Boolean,
    apps: List<InstalledApp>,
    iconPackManager: IconPackManager?,
    iconPackMode: IconPackMode,
    selectedIconPackPackage: String,
    overrides: Map<String, IconOverrideEntity>,
    onExecuteTile: (ActionTileBinding) -> Unit,
    onAddControlTile: (ActionTileBinding) -> Unit,
    onUpdateControlTile: (Int, ActionTileBinding) -> Unit,
    onRemoveControlTile: (Int) -> Unit,
    onResetControlTiles: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onOpenAudioOutputSettings: () -> Unit
) {
    val colors = LocalKansoColors.current
    var addingNewTile by remember { mutableStateOf(false) }
    var editingTileIndex by remember { mutableStateOf<Int?>(null) }
    var tileFilter by remember { mutableStateOf("") }

    val indexedTiles = remember(controlTiles, tileFilter) {
        val all = controlTiles.mapIndexed { idx, tile -> idx to tile }
        if (tileFilter.isBlank()) all
        else {
            val q = tileFilter.trim().lowercase()
            all.filter { (_, tile) ->
                tile.customLabel.lowercase().contains(q) ||
                    tile.action.label.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        // Raised Audio Bus Volume Card (Cyber Cyan Track) + Live Audio Output Routing Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .kansoNeumorphic(raised = true, cornerRadius = 22.dp)
                .kansoMechaAccents(showCornerBrackets = true, showLedPip = true)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = "Audio Bus Volume",
                    tint = colors.secondaryTelemetry,
                    modifier = Modifier.size(19.dp)
                )
                Slider(
                    value = telemetry.mediaVolumeFraction.coerceIn(0f, 1f),
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.secondaryTelemetry,
                        activeTrackColor = colors.secondaryTelemetry,
                        inactiveTrackColor = colors.surfaceInset
                    ),
                    modifier = Modifier
                        .testTag("control_volume_slider")
                        .weight(1f)
                        .height(32.dp)
                )
                Text(
                    text = "${(telemetry.mediaVolumeFraction * 100f).roundToInt()}%",
                    color = colors.secondaryTelemetry,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }

            // Live Audio Output Router Pill
            Row(
                modifier = Modifier
                    .testTag("control_audio_output_bar")
                    .fillMaxWidth()
                    .kansoNeumorphic(raised = false, cornerRadius = 14.dp, surfaceColor = colors.surfaceInset)
                    .kansoTactileClickable(onClick = onOpenAudioOutputSettings)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Headphones,
                        contentDescription = "Audio Output Device",
                        tint = colors.secondaryTelemetry,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "OUTPUT // ${telemetry.audioOutputLabel.uppercase()}",
                        color = colors.inkPrimary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "ROUTE",
                    color = colors.secondaryTelemetry,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            }
        }

        // Hardware Matrix Header / Edit Mode Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEditing) {
                    "TAP TILE TO REMAP • LONG-PRESS ANYTIME"
                } else {
                    "HARDWARE MATRIX // ${controlTiles.size} SUBSYSTEMS"
                },
                color = if (isEditing) colors.accent else colors.inkSecondary,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 9.5.sp,
                letterSpacing = 0.8.sp
            )
            if (isEditing) {
                Row(
                    modifier = Modifier
                        .testTag("control_panel_reset_tiles")
                        .clip(CircleShape)
                        .background(colors.darkModuleBg)
                        .kansoTactileClickable(onClick = onResetControlTiles)
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = "Reset default control buttons",
                        tint = colors.accent,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "RESET",
                        color = Color(0xFFF2F3F5),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }
        }

        // 3-Column Tactile Hardware Matrix Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 2.dp, bottom = 6.dp),
            modifier = Modifier
                .testTag("control_panel_tiles_grid")
                .fillMaxWidth()
                .weight(1f)
        ) {
            itemsIndexed(indexedTiles, key = { _, pair -> "${pair.first}_${pair.second.action}_${pair.second.packageName}" }) { _, (origIdx, tile) ->
                val isAppTile = tile.action == KansoActionId.LAUNCH_APP && tile.packageName.isNotBlank()
                val active = !isAppTile && telemetry.isActionActive(tile.action, isMediaPlaying)
                val bg by animateColorAsState(
                    targetValue = if (active) colors.accent else colors.darkModuleBg,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "ctrlBg_$origIdx"
                )
                val fg by animateColorAsState(
                    targetValue = if (active) colors.onAccent else Color(0xFFE5E8EF),
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "ctrlFg_$origIdx"
                )
                val tileTitle = tile.customLabel.ifBlank { tile.action.label }
                val tileStatus = if (isAppTile) "APP" else statusLabelForAction(tile.action, telemetry, isMediaPlaying)

                Box(
                    modifier = Modifier
                        .testTag("control_tile_${origIdx}_${tile.action.name.lowercase()}")
                        .height(86.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .kansoNeumorphic(raised = true, cornerRadius = 20.dp, surfaceColor = bg)
                            .then(
                                if (isEditing) Modifier.border(1.5.dp, colors.accent, RoundedCornerShape(20.dp))
                                else Modifier
                            )
                            .kansoMechaAccents(accentActive = active, showCornerBrackets = true, showLedPip = true)
                            .kansoTactileClickable(
                                onLongClick = { editingTileIndex = origIdx },
                                onClick = {
                                    if (isEditing) editingTileIndex = origIdx
                                    else onExecuteTile(tile)
                                }
                            )
                            .padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start
                    ) {
                        if (isAppTile && iconPackManager != null) {
                            val key = "${tile.packageName}/${tile.activityName}"
                            KansoAppIcon(
                                packageName = tile.packageName,
                                activityName = tile.activityName,
                                contentDescription = tileTitle,
                                iconPackManager = iconPackManager,
                                iconPackMode = iconPackMode,
                                selectedIconPackPackage = selectedIconPackPackage,
                                iconOverride = overrides[key],
                                iconSize = 26.dp,
                                cornerRadius = 7.dp
                            )
                        } else {
                            Icon(
                                imageVector = iconForAction(tile.action, telemetry, isMediaPlaying),
                                contentDescription = tileTitle,
                                tint = fg,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = tileTitle,
                                color = fg,
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isEditing) "REMAP" else tileStatus,
                                color = fg.copy(alpha = 0.72f),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp
                            )
                        }
                    }

                    if (isEditing && controlTiles.size > 1) {
                        Box(
                            modifier = Modifier
                                .testTag("control_tile_remove_$origIdx")
                                .align(Alignment.TopEnd)
                                .offset(x = 3.dp, y = (-3).dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(colors.accent)
                                .kansoTactileClickable { onRemoveControlTile(origIdx) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Remove button",
                                tint = colors.onAccent,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            item(key = "add_control_tile_card") {
                Column(
                    modifier = Modifier
                        .testTag("control_panel_add_tile_btn")
                        .height(86.dp)
                        .fillMaxWidth()
                        .kansoNeumorphic(raised = false, cornerRadius = 20.dp)
                        .border(1.2.dp, colors.accent.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                        .kansoTactileClickable { addingNewTile = true }
                        .padding(10.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add control button",
                        tint = colors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ADD TILE",
                        color = colors.accent,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }
        }

        // Recessed Filter Bar + System Telemetry Readout
        StitchShadeFilterBar(
            value = tileFilter,
            placeholder = "FILTER SUBSYSTEM TILES...",
            onValueChange = { tileFilter = it }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .kansoNeumorphic(raised = false, cornerRadius = 15.dp)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "RAM ${telemetry.ramUsedGb}/${telemetry.ramTotalGb}G",
                color = colors.inkPrimary,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 9.5.sp
            )
            Text(
                text = "SSD ${telemetry.storageUsedGb}/${telemetry.storageTotalGb}",
                color = colors.inkSecondary,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.5.sp
            )
            Text(
                text = "${telemetry.batteryTempC.toInt()}°C // ${telemetry.networkSpeedValue}${telemetry.networkSpeedUnit}",
                color = colors.accent,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 9.5.sp
            )
        }
    }

    if (addingNewTile) {
        ActionTileBindingDialog(
            title = "Add Control Panel Button",
            current = null,
            apps = apps,
            iconPackManager = iconPackManager,
            iconPackMode = iconPackMode,
            selectedIconPackPackage = selectedIconPackPackage,
            overrides = overrides,
            onSelect = { newTile ->
                onAddControlTile(newTile)
                addingNewTile = false
            },
            onDismiss = { addingNewTile = false }
        )
    }

    editingTileIndex?.let { idx ->
        ActionTileBindingDialog(
            title = "Remap Button ${idx + 1}",
            current = controlTiles.getOrNull(idx),
            apps = apps,
            iconPackManager = iconPackManager,
            iconPackMode = iconPackMode,
            selectedIconPackPackage = selectedIconPackPackage,
            overrides = overrides,
            onSelect = { updated ->
                onUpdateControlTile(idx, updated)
                editingTileIndex = null
            },
            onDismiss = { editingTileIndex = null }
        )
    }
}

@Composable
private fun StitchShadeFilterBar(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    val colors = LocalKansoColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .kansoNeumorphic(raised = false, cornerRadius = 15.dp, surfaceColor = colors.surfaceInset)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = colors.inkSecondary,
            modifier = Modifier.size(16.dp)
        )
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = colors.inkSecondary.copy(alpha = 0.7f),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = colors.inkPrimary,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp
                ),
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Clear filter",
                tint = colors.inkSecondary,
                modifier = Modifier
                    .size(16.dp)
                    .kansoTactileClickable { onValueChange("") }
            )
        }
    }
}
