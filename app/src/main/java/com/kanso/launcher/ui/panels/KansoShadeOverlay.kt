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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
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
import com.kanso.launcher.ui.widgets.MediaHudWidget
import com.kanso.launcher.ui.widgets.iconForAction
import com.kanso.launcher.ui.widgets.statusLabelForAction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ShadeStage {
    CLOSED,
    NOTIFICATIONS,
    CONTROLS
}

/**
 * Two-Stage Custom Mecha Drop-Down Shade:
 * - Stage 1 (First Swipe Down): Kanso Notification Panel with live notifications & media player.
 * - Stage 2 (Second Swipe Down): Customizable Control Panel where users can add unlimited buttons,
 *   remove tiles, and remap buttons to any system action or installed app shortcut.
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
    modifier: Modifier = Modifier
) {
    val colors = LocalKansoColors.current
    var dragAccum by remember { mutableFloatStateOf(0f) }
    var isEditingControls by remember { mutableStateOf(false) }

    BackHandler(enabled = stage != ShadeStage.CLOSED) {
        if (isEditingControls) {
            isEditingControls = false
        } else {
            when (stage) {
                ShadeStage.CONTROLS -> onStageChange(ShadeStage.NOTIFICATIONS)
                ShadeStage.NOTIFICATIONS -> onStageChange(ShadeStage.CLOSED)
                ShadeStage.CLOSED -> Unit
            }
        }
    }

    AnimatedVisibility(
        visible = stage != ShadeStage.CLOSED,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = spring(dampingRatio = 0.90f, stiffness = 460f)
        ) + fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .testTag("kanso_shade_overlay")
                .fillMaxSize()
                .background(colors.canvasBg.copy(alpha = 0.97f))
                .pointerInput(stage) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            dragAccum = 0f
                            KansoPerformanceGovernor.notifyAnimationBurst(600L)
                        },
                        onVerticalDrag = { _, dragAmount -> dragAccum += dragAmount },
                        onDragEnd = {
                            if (dragAccum > 95f && stage == ShadeStage.NOTIFICATIONS) {
                                onStageChange(ShadeStage.CONTROLS)
                            } else if (dragAccum < -95f) {
                                isEditingControls = false
                                if (stage == ShadeStage.CONTROLS) onStageChange(ShadeStage.NOTIFICATIONS)
                                else onStageChange(ShadeStage.CLOSED)
                            }
                            dragAccum = 0f
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Mecha Header + Stage Switcher + Control Panel Edit toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (stage == ShadeStage.CONTROLS) {
                                if (isEditingControls) "EDIT CONTROLS" else "CONTROL PANEL"
                            } else {
                                "NOTIFICATIONS"
                            },
                            color = colors.inkPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${telemetry.clock.hours24}:${telemetry.clock.minutes} // ${telemetry.batteryPercent}% BAT // ${telemetry.networkSpeedValue} ${telemetry.networkSpeedUnit}",
                            color = colors.accent,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (stage == ShadeStage.CONTROLS) {
                            Box(
                                modifier = Modifier
                                    .testTag("control_panel_edit_toggle")
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isEditingControls) colors.accent else colors.darkModuleBg)
                                    .kansoTactileClickable { isEditingControls = !isEditingControls },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isEditingControls) Icons.Rounded.Check else Icons.Rounded.Edit,
                                    contentDescription = if (isEditingControls) "Done editing controls" else "Customize control buttons",
                                    tint = if (isEditingControls) colors.onAccent else colors.accent,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .testTag("shade_toggle_stage_button")
                                .height(38.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(colors.darkModuleBg)
                                .kansoTactileClickable {
                                    isEditingControls = false
                                    onStageChange(
                                        if (stage == ShadeStage.NOTIFICATIONS) ShadeStage.CONTROLS
                                        else ShadeStage.NOTIFICATIONS
                                    )
                                }
                                .padding(horizontal = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = if (stage == ShadeStage.NOTIFICATIONS) Icons.Rounded.Tune else Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (stage == ShadeStage.NOTIFICATIONS) "CONTROLS" else "ALERTS",
                                    color = Color(0xFFF2F3F5),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .testTag("shade_close_button")
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.darkModuleBg)
                                .kansoTactileClickable {
                                    isEditingControls = false
                                    onStageChange(ShadeStage.CLOSED)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close shade",
                                tint = Color(0xFFF2F3F5),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedContent(
                    targetState = stage,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    modifier = Modifier.weight(1f),
                    label = "shadeStageContent"
                ) { activeStage ->
                    if (activeStage == ShadeStage.NOTIFICATIONS) {
                        NotificationPanelBody(
                            notifications = notifications,
                            notificationAccessGranted = notificationAccessGranted,
                            media = media,
                            onOpenNotificationAccessSettings = onOpenNotificationAccessSettings,
                            onDismissNotification = onDismissNotification,
                            onClearAllNotifications = onClearAllNotifications,
                            onPlayPause = onPlayPause,
                            onPrevTrack = onPrevTrack,
                            onNextTrack = onNextTrack,
                            onOpenPlayerApp = onOpenPlayerApp,
                            onCloseShade = { onStageChange(ShadeStage.CLOSED) }
                        )
                    } else {
                        ControlPanelBody(
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
                            onBrightnessChange = onBrightnessChange,
                            onVolumeChange = onVolumeChange
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .testTag("shade_stage_handle")
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .kansoNeumorphic(raised = false, cornerRadius = 18.dp)
                        .kansoTactileClickable {
                            isEditingControls = false
                            if (stage == ShadeStage.NOTIFICATIONS) onStageChange(ShadeStage.CONTROLS)
                            else onStageChange(ShadeStage.CLOSED)
                        }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (stage == ShadeStage.NOTIFICATIONS) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowUp,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (stage == ShadeStage.NOTIFICATIONS) {
                            "SWIPE DOWN AGAIN FOR CONTROL PANEL"
                        } else {
                            "SWIPE UP TO RETURN"
                        },
                        color = colors.inkSecondary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationPanelBody(
    notifications: List<KansoNotificationItem>,
    notificationAccessGranted: Boolean,
    media: MediaHudState,
    onOpenNotificationAccessSettings: () -> Unit,
    onDismissNotification: (String) -> Unit,
    onClearAllNotifications: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevTrack: () -> Unit,
    onNextTrack: () -> Unit,
    onOpenPlayerApp: (String) -> Unit,
    onCloseShade: () -> Unit
) {
    val colors = LocalKansoColors.current
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        MediaHudWidget(
            media = media,
            notificationAccessGranted = notificationAccessGranted,
            onRequestMediaPermission = onOpenNotificationAccessSettings,
            onOpenPlayerApp = { pkg ->
                onCloseShade()
                onOpenPlayerApp(pkg)
            },
            onPlayPause = onPlayPause,
            onPrevious = onPrevTrack,
            onNext = onNextTrack,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ACTIVE STREAM (${notifications.size})",
                color = colors.inkSecondary,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp,
                letterSpacing = 1.sp
            )
            if (notifications.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .testTag("notifications_clear_all")
                        .clip(CircleShape)
                        .background(colors.darkModuleBg)
                        .kansoTactileClickable(onClick = onClearAllNotifications)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
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
        }

        Spacer(modifier = Modifier.height(10.dp))

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
                    text = "Enable Kanso Media & Notifications to stream live alerts and sync Spotify, YouTube, Apple Music, and Prime Music here.",
                    color = colors.inkSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp
                )
                Box(
                    modifier = Modifier
                        .testTag("grant_notif_panel_button")
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.accent)
                        .kansoTactileClickable(onClick = onOpenNotificationAccessSettings)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "ENABLE ACCESS",
                        color = colors.onAccent,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        } else if (notifications.isEmpty()) {
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
                        text = "ALL CLEAR // NO ACTIVE ALERTS",
                        color = colors.inkSecondary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(notifications, key = { it.key }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .kansoNeumorphic(raised = true, cornerRadius = 18.dp)
                            .kansoMechaAccents(showCornerBrackets = true, showLedPip = false)
                            .kansoTactileClickable {
                                runCatching { item.contentIntent?.send() }
                                onCloseShade()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.appName.uppercase(),
                                    color = colors.accent,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.5.sp
                                )
                                Text(
                                    text = timeFmt.format(Date(item.postTimeMs)),
                                    color = colors.inkSecondary,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
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
                                    modifier = Modifier.size(15.dp)
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
private fun ControlPanelBody(
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
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    val colors = LocalKansoColors.current
    var addingNewTile by remember { mutableStateOf(false) }
    var editingTileIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Brightness & Media Volume Mecha Sliders
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .kansoNeumorphic(raised = true, cornerRadius = 22.dp)
                .kansoMechaAccents(showCornerBrackets = true, showLedPip = true)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Brightness6,
                    contentDescription = "Brightness",
                    tint = colors.accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Slider(
                    value = telemetry.brightnessFraction,
                    onValueChange = onBrightnessChange,
                    valueRange = 0.05f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accent,
                        activeTrackColor = colors.accent,
                        inactiveTrackColor = colors.trackMuted
                    ),
                    modifier = Modifier
                        .testTag("control_brightness_slider")
                        .weight(1f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = "Media Volume",
                    tint = colors.accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Slider(
                    value = telemetry.mediaVolumeFraction,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accent,
                        activeTrackColor = colors.accent,
                        inactiveTrackColor = colors.trackMuted
                    ),
                    modifier = Modifier
                        .testTag("control_volume_slider")
                        .weight(1f)
                )
            }
        }

        // Edit Mode helper bar
        if (isEditing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .kansoNeumorphic(raised = false, cornerRadius = 14.dp)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TAP TILE TO REMAP • + TO ADD",
                    color = colors.accent,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp
                )
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

        // Scrollable Customizable Control Button Grid (unlimited buttons!)
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 6.dp),
            modifier = Modifier
                .testTag("control_panel_tiles_grid")
                .fillMaxWidth()
                .weight(1f)
        ) {
            itemsIndexed(controlTiles) { index, tile ->
                val isAppTile = tile.action == KansoActionId.LAUNCH_APP && tile.packageName.isNotBlank()
                val active = !isAppTile && telemetry.isActionActive(tile.action, isMediaPlaying)
                val bg by animateColorAsState(
                    targetValue = if (active) colors.accent else colors.darkModuleBg,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "ctrlBg_$index"
                )
                val fg by animateColorAsState(
                    targetValue = if (active) colors.onAccent else Color(0xFFE5E8EF),
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "ctrlFg_$index"
                )
                val tileTitle = tile.customLabel.ifBlank { tile.action.label }
                val tileStatus = if (isAppTile) "APP" else statusLabelForAction(tile.action, telemetry, isMediaPlaying)

                Box(
                    modifier = Modifier
                        .testTag("control_tile_${index}_${tile.action.name.lowercase()}")
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
                                onLongClick = { editingTileIndex = index },
                                onClick = {
                                    if (isEditing) editingTileIndex = index
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
                                .testTag("control_tile_remove_$index")
                                .align(Alignment.TopEnd)
                                .offset(x = 3.dp, y = (-3).dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(colors.accent)
                                .kansoTactileClickable { onRemoveControlTile(index) },
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

            // Always show "+ Add Button" card in Edit mode (and at end of grid)
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
                        text = "ADD BUTTON",
                        color = colors.accent,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }
        }

        // System Telemetry Readout Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .kansoNeumorphic(raised = false, cornerRadius = 16.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "RAM ${(telemetry.ramUsageFraction * 100).toInt()}%",
                color = colors.inkPrimary,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
            Text(
                text = "SSD ${telemetry.storageUsedGb}/${telemetry.storageTotalGb}",
                color = colors.inkSecondary,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp
            )
            Text(
                text = "${telemetry.batteryTempC.toInt()}°C",
                color = colors.accent,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
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
