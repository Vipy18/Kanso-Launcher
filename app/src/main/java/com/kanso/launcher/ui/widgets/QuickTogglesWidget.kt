package com.kanso.launcher.ui.widgets

import android.media.AudioManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kanso.launcher.R
import com.kanso.launcher.data.model.ActionTileBinding
import com.kanso.launcher.data.model.KansoActionId
import com.kanso.launcher.data.model.MechaWidgetTheme
import com.kanso.launcher.data.telemetry.TelemetryState
import com.kanso.launcher.ui.theme.JetBrainsMonoFontFamily
import com.kanso.launcher.ui.theme.LocalKansoColors
import com.kanso.launcher.ui.theme.WithMechaWidgetTheme
import com.kanso.launcher.ui.theme.kansoIconNeumorphic
import com.kanso.launcher.ui.theme.kansoNeumorphic
import com.kanso.launcher.ui.theme.kansoTactileClickable
import com.kanso.launcher.ui.theme.mechaMetallicChassis

/**
 * Top-right sharp machined 3-stop Sound / System Toggle Switch (SILENT <-> VIBRATE <-> RING).
 * Strictly uses active accent + launcher default colors with cut-corner metallic actuator thumb.
 */
@Composable
fun StatusVolumeCapsule(
    telemetry: TelemetryState,
    action: KansoActionId = KansoActionId.SILENT_MODE,
    mechaTheme: MechaWidgetTheme = MechaWidgetTheme.DEFAULT,
    isMediaPlaying: Boolean = false,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onToggle: () -> Unit
) {
    WithMechaWidgetTheme(mechaTheme) {
        val colors = LocalKansoColors.current
        val isSoundMode = action == KansoActionId.SILENT_MODE

        val stopIndex = when {
            isSoundMode -> when (telemetry.ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> 0
                AudioManager.RINGER_MODE_VIBRATE -> 1
                else -> 2
            }
            telemetry.isActionActive(action, isMediaPlaying) -> 2
            else -> 0
        }

        val knobTargetX = when (stopIndex) {
            0 -> 0.dp
            1 -> 42.dp
            else -> 84.dp
        }

        val knobOffsetX by animateDpAsState(
            targetValue = knobTargetX,
            animationSpec = spring(
                dampingRatio = 0.66f,
                stiffness = 440f
            ),
            label = "volumeKnobSlide"
        )

        val knobBg by animateColorAsState(
            targetValue = when {
                isSoundMode && stopIndex == 0 -> colors.darkModuleElevated
                isSoundMode && stopIndex == 1 -> colors.accent.copy(alpha = 0.85f)
                stopIndex == 2 -> colors.accent
                else -> colors.darkModuleElevated
            },
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "volumeKnobColor"
        )
        val knobTint by animateColorAsState(
            targetValue = if (stopIndex > 0) colors.onAccent else Color(0xFFD0D5DE),
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "volumeKnobTint"
        )

        val icon = iconForAction(action, telemetry, isMediaPlaying)
        val statusText = statusLabelForAction(action, telemetry, isMediaPlaying)
        val isClassic = mechaTheme == MechaWidgetTheme.DEFAULT

        if (isClassic) {
            val classicTargetX = when (stopIndex) {
                0 -> 0.dp
                1 -> 44.dp
                else -> 88.dp
            }
            val classicKnobX by animateDpAsState(
                targetValue = classicTargetX,
                animationSpec = spring(dampingRatio = 0.66f, stiffness = 440f),
                label = "classicVolumeKnobSlide"
            )
            Box(
                modifier = modifier
                    .testTag("widget_volume_capsule")
                    .height(54.dp)
                    .width(148.dp)
                    .kansoNeumorphic(
                        raised = true,
                        cornerRadius = 27.dp,
                        surfaceColor = colors.darkModuleBg
                    )
                    .kansoTactileClickable(onLongClick = onLongClick, onClick = onToggle)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (stopIndex == 0) Color.Transparent else Color(0xFF5D626E))
                    )
                    Text(
                        text = statusText,
                        color = Color(0xFF8A909A),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 9.sp,
                        letterSpacing = 1.0.sp
                    )
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (stopIndex == 2) Color.Transparent else Color(0xFF5D626E))
                    )
                }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(classicKnobX.roundToPx(), 0) }
                        .size(44.dp)
                        .kansoIconNeumorphic(cornerRadius = 22.dp, shadowScale = 0.6f)
                        .clip(CircleShape)
                        .background(knobBg)
                        .border(
                            width = 0.9.dp,
                            color = if (stopIndex > 0) colors.onAccent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.10f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = Pair(stopIndex, icon),
                        transitionSpec = {
                            (fadeIn() + scaleIn(initialScale = 0.72f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.72f))
                        },
                        label = "classicVolumeIconMorph"
                    ) { (_, targetIcon) ->
                        Icon(
                            imageVector = targetIcon,
                            contentDescription = stringResource(R.string.cd_volume_toggle),
                            tint = knobTint,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
            return@WithMechaWidgetTheme
        }

        val thumbRadius = 7.dp
        val thumbShape = RoundedCornerShape(thumbRadius)

        Box(
            modifier = modifier
                .testTag("widget_volume_capsule")
                .height(54.dp)
                .width(142.dp)
                .mechaMetallicChassis(
                    theme = mechaTheme,
                    darkPlate = true,
                    accentActive = stopIndex > 0,
                    cutDp = 11.dp
                )
                .kansoTactileClickable(onLongClick = onLongClick, onClick = onToggle)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 14.dp)
                        .background(if (stopIndex == 0) colors.accent else Color(0xFF505662))
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isSoundMode) {
                        Box(
                            modifier = Modifier
                                .size(width = 10.dp, height = 3.dp)
                                .background(if (stopIndex == 1) colors.accent else Color(0xFF505662))
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                    Text(
                        text = statusText,
                        color = Color(0xFFBAC0CC),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp,
                        letterSpacing = 0.8.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 14.dp)
                        .background(if (stopIndex == 2) colors.accent else Color(0xFF505662))
                )
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(knobOffsetX.roundToPx(), 0) }
                    .size(width = 46.dp, height = 42.dp)
                    .kansoIconNeumorphic(cornerRadius = thumbRadius, shadowScale = 0.55f)
                    .clip(thumbShape)
                    .background(knobBg)
                    .border(
                        width = 1.dp,
                        color = if (stopIndex > 0) colors.onAccent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.22f),
                        shape = thumbShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = Pair(stopIndex, icon),
                    transitionSpec = {
                        (fadeIn() + scaleIn(initialScale = 0.72f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.72f))
                    },
                    label = "volumeIconMorph"
                ) { (_, targetIcon) ->
                    Icon(
                        imageVector = targetIcon,
                        contentDescription = stringResource(R.string.cd_volume_toggle),
                        tint = knobTint,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }
    }
}

/**
 * Sharp machined metallic Hardware Quick-Toggle Cluster strictly using active accent + defaults.
 */
@Composable
fun QuickToggleClusterWidget(
    isMediaPlaying: Boolean,
    isWifiConnected: Boolean,
    isMobileConnected: Boolean,
    isBluetoothEnabled: Boolean,
    mechaTheme: MechaWidgetTheme = MechaWidgetTheme.DEFAULT,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onTogglePlayPause: () -> Unit,
    onOpenWifiPanel: () -> Unit,
    onOpenMobileDataPanel: () -> Unit,
    onOpenBluetooth: () -> Unit
) {
    WithMechaWidgetTheme(mechaTheme) {
        val colors = LocalKansoColors.current

        val playBg by animateColorAsState(
            targetValue = if (isMediaPlaying) colors.accent else colors.darkModuleElevated,
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "playBg"
        )
        val playTint by animateColorAsState(
            targetValue = if (isMediaPlaying) colors.onAccent else colors.accent,
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "playTint"
        )
        val wifiBg by animateColorAsState(
            targetValue = if (isWifiConnected) colors.accent else colors.darkModuleElevated,
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "wifiBg"
        )
        val wifiTint by animateColorAsState(
            targetValue = if (isWifiConnected) colors.onAccent else Color(0xFF9EA5B2),
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "wifiTint"
        )
        val mobileBg by animateColorAsState(
            targetValue = if (isMobileConnected) colors.accent else colors.darkModuleBg,
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "mobileBg"
        )
        val mobileTint by animateColorAsState(
            targetValue = if (isMobileConnected) colors.onAccent else Color(0xFF9EA5B2),
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "mobileTint"
        )
        val btBg by animateColorAsState(
            targetValue = if (isBluetoothEnabled) colors.accent else colors.darkModuleBg,
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "btBg"
        )
        val btTint by animateColorAsState(
            targetValue = if (isBluetoothEnabled) colors.onAccent else Color(0xFF9EA5B2),
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "btTint"
        )
        val btScale by animateFloatAsState(
            targetValue = if (isBluetoothEnabled) 1.03f else 0.97f,
            animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
            label = "btScale"
        )

        val isClassic = mechaTheme == MechaWidgetTheme.DEFAULT
        val keyShape = RoundedCornerShape(7.dp)

        BoxWithConstraints(
            modifier = modifier.testTag("widget_quick_toggles"),
            contentAlignment = Alignment.Center
        ) {
            val plateW = if (maxWidth.value.isFinite() && maxWidth > 100.dp) maxWidth.coerceAtMost(176.dp) else 148.dp
            val topH = (plateW * 0.40f).coerceIn(52.dp, 62.dp)
            val keyW = (plateW - 18.dp) / 2f
            val keyH = (topH - 12.dp).coerceAtLeast(36.dp)
            val lowerSize = (plateW * 0.37f).coerceIn(50.dp, 60.dp)

            if (isClassic) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Upper classic dark stadium dual capsule (Play + elevated circular Wi-Fi disc)
                    Box(
                        modifier = Modifier
                            .width(plateW)
                            .height(topH)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .kansoNeumorphic(
                                    raised = true,
                                    cornerRadius = topH / 2f,
                                    surfaceColor = colors.darkModuleBg
                                )
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .testTag("quick_toggle_play_pause")
                                    .size(topH - 10.dp)
                                    .clip(CircleShape)
                                    .background(if (isMediaPlaying) colors.accent else Color.Transparent)
                                    .kansoTactileClickable(onLongClick = onLongClick, onClick = onTogglePlayPause),
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedContent(
                                    targetState = isMediaPlaying,
                                    transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.72f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.72f)) },
                                    label = "classicPlayPauseIcon"
                                ) { playing ->
                                    Icon(
                                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = stringResource(R.string.cd_media_play_pause),
                                        tint = playTint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .testTag("quick_toggle_wifi")
                                    .size(topH - 8.dp)
                                    .kansoIconNeumorphic(cornerRadius = (topH - 8.dp) / 2f, shadowScale = 0.65f)
                                    .clip(CircleShape)
                                    .background(wifiBg)
                                    .kansoTactileClickable(onLongClick = onLongClick, onClick = onOpenWifiPanel),
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedContent(
                                    targetState = isWifiConnected,
                                    transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.75f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.75f)) },
                                    label = "classicWifiIcon"
                                ) { connected ->
                                    Icon(
                                        imageVector = if (connected) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                                        contentDescription = stringResource(R.string.cd_wifi_toggle),
                                        tint = wifiTint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        if (isWifiConnected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 2.dp, end = 2.dp)
                                    .size(4.5.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent)
                            )
                        }
                    }

                    // Lower twin circular hardware discs (Mobile Data + Bluetooth)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .testTag("quick_toggle_mobile_data")
                                .size(lowerSize)
                                .kansoNeumorphic(
                                    raised = true,
                                    cornerRadius = lowerSize / 2f,
                                    surfaceColor = mobileBg
                                )
                                .clip(CircleShape)
                                .kansoTactileClickable(onLongClick = onLongClick, onClick = onOpenMobileDataPanel),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SwapVert,
                                contentDescription = stringResource(R.string.cd_mobile_data_toggle),
                                tint = mobileTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .testTag("quick_toggle_bluetooth")
                                .size(lowerSize)
                                .graphicsLayer {
                                    scaleX = btScale
                                    scaleY = btScale
                                }
                                .kansoNeumorphic(
                                    raised = true,
                                    cornerRadius = lowerSize / 2f,
                                    surfaceColor = btBg
                                )
                                .clip(CircleShape)
                                .kansoTactileClickable(onLongClick = onLongClick, onClick = onOpenBluetooth),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = isBluetoothEnabled,
                                transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.75f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.75f)) },
                                label = "classicBtIcon"
                            ) { enabled ->
                                Icon(
                                    imageVector = if (enabled) Icons.Rounded.Bluetooth else Icons.Rounded.BluetoothDisabled,
                                    contentDescription = stringResource(R.string.cd_bluetooth_toggle),
                                    tint = btTint,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
                return@BoxWithConstraints
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .height(topH)
                        .width(plateW)
                        .mechaMetallicChassis(
                            theme = mechaTheme,
                            darkPlate = true,
                            accentActive = isWifiConnected || isMediaPlaying,
                            cutDp = 10.dp
                        )
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .testTag("quick_toggle_play_pause")
                            .size(width = keyW, height = keyH)
                            .clip(keyShape)
                            .background(playBg)
                            .kansoTactileClickable(onLongClick = onLongClick, onClick = onTogglePlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = isMediaPlaying,
                            transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.72f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.72f)) },
                            label = "playPauseIcon"
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = stringResource(R.string.cd_media_play_pause),
                                tint = playTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .testTag("quick_toggle_wifi")
                            .size(width = keyW, height = keyH)
                            .clip(keyShape)
                            .background(wifiBg)
                            .kansoTactileClickable(onLongClick = onLongClick, onClick = onOpenWifiPanel),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = isWifiConnected,
                            transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.75f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.75f)) },
                            label = "wifiIcon"
                        ) { connected ->
                            Icon(
                                imageVector = if (connected) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                                contentDescription = stringResource(R.string.cd_wifi_toggle),
                                tint = wifiTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .testTag("quick_toggle_mobile_data")
                            .size(lowerSize)
                            .mechaMetallicChassis(
                                theme = mechaTheme,
                                darkPlate = !isMobileConnected,
                                accentActive = isMobileConnected,
                                cutDp = 9.dp
                            )
                            .background(if (isMobileConnected) mobileBg else Color.Transparent)
                            .kansoTactileClickable(onLongClick = onLongClick, onClick = onOpenMobileDataPanel),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SwapVert,
                            contentDescription = stringResource(R.string.cd_mobile_data_toggle),
                            tint = mobileTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .testTag("quick_toggle_bluetooth")
                            .size(lowerSize)
                            .graphicsLayer {
                                scaleX = btScale
                                scaleY = btScale
                            }
                            .mechaMetallicChassis(
                                theme = mechaTheme,
                                darkPlate = !isBluetoothEnabled,
                                accentActive = isBluetoothEnabled,
                                cutDp = 9.dp
                            )
                            .background(if (isBluetoothEnabled) btBg else Color.Transparent)
                            .kansoTactileClickable(onLongClick = onLongClick, onClick = onOpenBluetooth),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = isBluetoothEnabled,
                            transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.75f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.75f)) },
                            label = "btIcon"
                        ) { enabled ->
                            Icon(
                                imageVector = if (enabled) Icons.Rounded.Bluetooth else Icons.Rounded.BluetoothDisabled,
                                contentDescription = stringResource(R.string.cd_bluetooth_toggle),
                                tint = btTint,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sharp machined metallic X*Y Action Matrix Widget strictly using active accent + defaults.
 */
@Composable
fun ActionMatrixWidget(
    rows: Int,
    cols: Int,
    tiles: List<ActionTileBinding>,
    telemetry: TelemetryState,
    isMediaPlaying: Boolean,
    mechaTheme: MechaWidgetTheme = MechaWidgetTheme.DEFAULT,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onTileClick: (ActionTileBinding) -> Unit
) {
    WithMechaWidgetTheme(mechaTheme) {
        val colors = LocalKansoColors.current
        val safeRows = rows.coerceIn(1, 4)
        val safeCols = cols.coerceIn(1, 4)
        val tileRadius = if (mechaTheme == MechaWidgetTheme.DEFAULT) 12.dp else 6.dp
        val tileShape = RoundedCornerShape(tileRadius)

        BoxWithConstraints(
            modifier = modifier
                .testTag("widget_action_matrix_${safeRows}x${safeCols}")
                .mechaMetallicChassis(
                    theme = mechaTheme,
                    darkPlate = false,
                    accentActive = false,
                    cutDp = 12.dp
                )
                .padding(5.dp)
        ) {
            val compact = maxHeight < 92.dp || safeRows >= 3
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 5.dp)
            ) {
                for (r in 0 until safeRows) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 5.dp)
                    ) {
                        for (c in 0 until safeCols) {
                            val idx = r * safeCols + c
                            val tile = tiles.getOrElse(idx) {
                                ActionTileBinding(action = KansoActionId.entries[idx % KansoActionId.entries.size])
                            }
                            val active = telemetry.isActionActive(tile.action, isMediaPlaying)
                            val tileBg by animateColorAsState(
                                targetValue = if (active) colors.accent else colors.darkModuleBg,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                label = "matrixTileBg_$idx"
                            )
                            val tileInk by animateColorAsState(
                                targetValue = if (active) colors.onAccent else Color(0xFFE0E4EC),
                                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                label = "matrixTileInk_$idx"
                            )
                            val icon = iconForAction(tile.action, telemetry, isMediaPlaying)

                            Column(
                                modifier = Modifier
                                    .testTag("matrix_tile_${tile.action.name.lowercase()}_$idx")
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(tileShape)
                                    .background(tileBg)
                                    .border(
                                        width = 0.8.dp,
                                        color = if (active) colors.onAccent.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.10f),
                                        shape = tileShape
                                    )
                                    .kansoTactileClickable(
                                        onLongClick = onLongClick,
                                        onClick = { onTileClick(tile) }
                                    )
                                    .padding(3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                AnimatedContent(
                                    targetState = icon,
                                    transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.78f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.78f)) },
                                    label = "matrixIcon_$idx"
                                ) { img ->
                                    Icon(
                                        imageVector = img,
                                        contentDescription = tile.customLabel.ifBlank { tile.action.label },
                                        tint = tileInk,
                                        modifier = Modifier.size(if (compact) 17.dp else 21.dp)
                                    )
                                }
                                if (!compact) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = tile.customLabel.ifBlank { tile.action.shortLabel },
                                        color = tileInk.copy(alpha = 0.92f),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sharp machined Modular Toggle Switch Widget supporting 3-state Sound Mode & binary toggles.
 */
@Composable
fun ModularToggleSwitchWidget(
    action: KansoActionId,
    customLabel: String,
    telemetry: TelemetryState,
    isMediaPlaying: Boolean,
    mechaTheme: MechaWidgetTheme = MechaWidgetTheme.DEFAULT,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onToggle: () -> Unit
) {
    WithMechaWidgetTheme(mechaTheme) {
        val colors = LocalKansoColors.current
        val isSoundMode = action == KansoActionId.SILENT_MODE
        val stopIndex = when {
            isSoundMode -> when (telemetry.ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> 0
                AudioManager.RINGER_MODE_VIBRATE -> 1
                else -> 2
            }
            telemetry.isActionActive(action, isMediaPlaying) -> 2
            else -> 0
        }
        val active = stopIndex > 0
        val icon = iconForAction(action, telemetry, isMediaPlaying)
        val stateLabel = statusLabelForAction(action, telemetry, isMediaPlaying)

        val thumbBg by animateColorAsState(
            targetValue = when {
                isSoundMode && stopIndex == 1 -> colors.accent.copy(alpha = 0.85f)
                active -> colors.accent
                else -> colors.darkModuleElevated
            },
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "toggleThumbBg"
        )
        val thumbInk by animateColorAsState(
            targetValue = if (active) colors.onAccent else Color(0xFFD0D5DE),
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "toggleThumbInk"
        )
        val thumbRadius = if (mechaTheme == MechaWidgetTheme.DEFAULT) 13.dp else 7.dp
        val thumbShape = RoundedCornerShape(thumbRadius)

        BoxWithConstraints(
            modifier = modifier
                .testTag("widget_toggle_switch_${action.name.lowercase()}")
                .mechaMetallicChassis(
                    theme = mechaTheme,
                    darkPlate = active,
                    accentActive = active,
                    cutDp = 11.dp
                )
                .kansoTactileClickable(onLongClick = onLongClick, onClick = onToggle)
                .padding(horizontal = 6.dp, vertical = 5.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val thumbSize = (minOf(maxHeight, 44.dp)).coerceAtLeast(32.dp)
            val slideDistance = (maxWidth - thumbSize).coerceAtLeast(0.dp)
            val targetThumbX = when (stopIndex) {
                0 -> 0.dp
                1 -> slideDistance * 0.5f
                else -> slideDistance
            }
            val thumbX by animateDpAsState(
                targetValue = targetThumbX,
                animationSpec = spring(dampingRatio = 0.66f, stiffness = 440f),
                label = "modularToggleSlide"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = when (stopIndex) {
                    0 -> Arrangement.End
                    1 -> Arrangement.SpaceBetween
                    else -> Arrangement.Start
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (stopIndex == 1 && isSoundMode) {
                    Text(
                        text = customLabel.ifBlank { action.shortLabel },
                        color = Color(0xFFF2F3F5),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                    Text(
                        text = stateLabel,
                        color = colors.accent,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                } else {
                    Column(
                        horizontalAlignment = if (active) Alignment.Start else Alignment.End
                    ) {
                        Text(
                            text = customLabel.ifBlank { action.shortLabel },
                            color = if (active) Color(0xFFF2F3F5) else colors.inkPrimary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stateLabel,
                            color = if (active) colors.accent else colors.inkSecondary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(thumbX.roundToPx(), 0) }
                    .size(thumbSize)
                    .kansoIconNeumorphic(cornerRadius = thumbRadius, shadowScale = 0.52f)
                    .clip(thumbShape)
                    .background(thumbBg)
                    .border(
                        width = 0.9.dp,
                        color = if (active) colors.onAccent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.22f),
                        shape = thumbShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = Pair(stopIndex, icon),
                    transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.74f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.74f)) },
                    label = "modularToggleIcon"
                ) { (_, glyph) ->
                    Icon(
                        imageVector = glyph,
                        contentDescription = action.label,
                        tint = thumbInk,
                        modifier = Modifier.size(thumbSize * 0.50f)
                    )
                }
            }
        }
    }
}
