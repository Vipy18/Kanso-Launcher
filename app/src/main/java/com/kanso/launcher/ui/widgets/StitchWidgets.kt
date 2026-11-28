package com.kanso.launcher.ui.widgets

import android.media.AudioManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kanso.launcher.data.model.MechaWidgetTheme
import com.kanso.launcher.data.telemetry.ClockTelemetry
import com.kanso.launcher.ui.theme.JetBrainsMonoFontFamily
import com.kanso.launcher.ui.theme.LocalKansoColors
import com.kanso.launcher.ui.theme.SpaceGroteskFontFamily
import com.kanso.launcher.ui.theme.kansoNeumorphic
import com.kanso.launcher.ui.theme.kansoTactileClickable
import com.kanso.launcher.ui.theme.mechaMetallicChassis
import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * Interactive 3-Position Tri-State Ringer Selector (SILENT / VIBRATE / RING).
 * Renders across all 4 Stitch design languages while honoring active launcher colors.
 */
@Composable
fun TriStateRingerWidget(
    ringerMode: Int,
    mechaTheme: MechaWidgetTheme = MechaWidgetTheme.DEFAULT,
    onSelectRingerMode: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalKansoColors.current
    val modes = remember {
        listOf(
            Triple(AudioManager.RINGER_MODE_SILENT, "SILENT", Icons.Rounded.NotificationsOff),
            Triple(AudioManager.RINGER_MODE_VIBRATE, "VIBRATE", Icons.Rounded.Vibration),
            Triple(AudioManager.RINGER_MODE_NORMAL, "RING", Icons.Rounded.NotificationsActive)
        )
    }

    val isCyber = mechaTheme == MechaWidgetTheme.DATA_STREAM
    val isMecha = mechaTheme == MechaWidgetTheme.MACHINED_MECHA || mechaTheme == MechaWidgetTheme.REACTOR_CORE
    val isPureNeumorph = mechaTheme == MechaWidgetTheme.TACTICAL_GAUGE
    val activeAccent = if (isCyber) colors.secondaryTelemetry else colors.accent
    val activeOnAccent = if (isCyber) Color(0xFF00222A) else colors.onAccent

    val outerModifier = when {
        isMecha -> modifier
            .mechaMetallicChassis(theme = mechaTheme, darkPlate = colors.isDark, accentActive = true, cutDp = 10.dp)
            .padding(6.dp)
        isCyber -> modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceInset)
            .border(1.dp, colors.secondaryTelemetry.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .padding(6.dp)
        isPureNeumorph -> modifier
            .kansoNeumorphic(raised = true, cornerRadius = 22.dp)
            .padding(6.dp)
        else -> modifier
            .kansoNeumorphic(raised = false, cornerRadius = 26.dp, surfaceColor = colors.surfaceInset)
            .border(1.dp, colors.trackMuted.copy(alpha = 0.55f), RoundedCornerShape(26.dp))
            .padding(5.dp)
    }

    Row(
        modifier = outerModifier
            .testTag("tri_state_ringer_widget")
            .fillMaxWidth()
            .height(52.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        modes.forEach { (mode, label, icon) ->
            val selected = ringerMode == mode
            val itemShape = when {
                isCyber -> RoundedCornerShape(6.dp)
                isMecha -> RoundedCornerShape(8.dp)
                isPureNeumorph -> RoundedCornerShape(16.dp)
                else -> CircleShape
            }
            val bgColor = when {
                selected && isPureNeumorph -> colors.surfaceRaisedHighlight
                selected -> activeAccent
                isPureNeumorph -> Color.Transparent
                else -> Color.Transparent
            }
            val fgColor = when {
                selected && isPureNeumorph -> colors.accent
                selected -> activeOnAccent
                else -> colors.inkSecondary
            }

            Row(
                modifier = Modifier
                    .testTag("tri_state_btn_${label.lowercase()}")
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (selected && isPureNeumorph) {
                            Modifier.kansoNeumorphic(raised = false, cornerRadius = 16.dp)
                        } else {
                            Modifier
                                .clip(itemShape)
                                .background(bgColor)
                        }
                    )
                    .kansoTactileClickable { onSelectRingerMode(mode) }
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = fgColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    color = fgColor,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Dual Analog Control Capsule: Display Luminance (Signal Amber) + Audio Bus Volume (Cyber Cyan).
 */
@Composable
fun DualSliderCapsuleWidget(
    brightnessFraction: Float,
    volumeFraction: Float,
    mechaTheme: MechaWidgetTheme = MechaWidgetTheme.DEFAULT,
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalKansoColors.current
    val isCyber = mechaTheme == MechaWidgetTheme.DATA_STREAM
    val isMecha = mechaTheme == MechaWidgetTheme.MACHINED_MECHA || mechaTheme == MechaWidgetTheme.REACTOR_CORE

    val containerMod = when {
        isMecha -> modifier.mechaMetallicChassis(theme = mechaTheme, darkPlate = colors.isDark, cutDp = 12.dp)
        isCyber -> modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceInset)
            .border(1.dp, colors.secondaryTelemetry.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
        else -> modifier
            .kansoNeumorphic(raised = true, cornerRadius = 22.dp)
            .border(1.dp, colors.trackMuted.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
    }

    Column(
        modifier = containerMod
            .testTag("dual_slider_capsule_widget")
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Luminance Track
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.WbSunny,
                contentDescription = "Brightness",
                tint = colors.accent,
                modifier = Modifier.size(16.dp)
            )
            Slider(
                value = brightnessFraction.coerceIn(0.05f, 1f),
                onValueChange = onBrightnessChange,
                valueRange = 0.05f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent,
                    inactiveTrackColor = colors.surfaceInset
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(26.dp)
            )
            Text(
                text = "${(brightnessFraction * 100f).roundToInt()}%",
                color = colors.accent,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                modifier = Modifier.width(34.dp)
            )
        }

        // Audio Bus Volume Track
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.VolumeUp,
                contentDescription = "Media Volume",
                tint = colors.secondaryTelemetry,
                modifier = Modifier.size(16.dp)
            )
            Slider(
                value = volumeFraction.coerceIn(0f, 1f),
                onValueChange = onVolumeChange,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = colors.secondaryTelemetry,
                    activeTrackColor = colors.secondaryTelemetry,
                    inactiveTrackColor = colors.surfaceInset
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(26.dp)
            )
            Text(
                text = "${(volumeFraction * 100f).roundToInt()}%",
                color = colors.secondaryTelemetry,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                modifier = Modifier.width(34.dp)
            )
        }
    }
}

/**
 * Multi-Theme Clock & Date Unit inspired by the Stitch Multi-Theme Widget Matrix.
 */
@Composable
fun StitchClockWidget(
    clock: ClockTelemetry,
    use24Hour: Boolean = true,
    mechaTheme: MechaWidgetTheme = MechaWidgetTheme.DEFAULT,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalKansoColors.current
    val hoursText = if (use24Hour) clock.hours24 else clock.hours12
    val tzBadge = remember {
        val offsetHours = TimeZone.getDefault().rawOffset / (1000 * 60 * 60)
        if (offsetHours >= 0) "GMT+$offsetHours" else "GMT$offsetHours"
    }

    when (mechaTheme) {
        MechaWidgetTheme.MACHINED_MECHA,
        MechaWidgetTheme.REACTOR_CORE -> {
            // Mecha // Unit-01: Chamfered Armored Clock with diagonal hazard caution stripes
            Box(
                modifier = modifier
                    .testTag("stitch_clock_mecha")
                    .fillMaxWidth()
                    .mechaMetallicChassis(theme = mechaTheme, darkPlate = true, accentActive = true, cutDp = 14.dp)
                    .kansoTactileClickable(onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(5.dp)
                ) {
                    val stripeW = 8.dp.toPx()
                    var x = -size.height
                    while (x < size.width + size.height) {
                        drawLine(
                            color = colors.accent.copy(alpha = 0.72f),
                            start = Offset(x, size.height),
                            end = Offset(x + size.height, 0f),
                            strokeWidth = stripeW * 0.45f,
                            cap = StrokeCap.Butt
                        )
                        x += stripeW
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "UNIT-01 // CHRONO",
                            color = colors.accent,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = tzBadge,
                            color = Color(0xFFD4C4AC),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 8.5.sp
                        )
                    }
                    Text(
                        text = "[ $hoursText:${clock.minutes} ]",
                        color = Color(0xFFF2F3F5),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = clock.formattedDate.uppercase(),
                        color = colors.accent.copy(alpha = 0.88f),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        MechaWidgetTheme.DATA_STREAM -> {
            // Cyber // HUD: Cyan monospace chronometer with scanline backdrop
            Box(
                modifier = modifier
                    .testTag("stitch_clock_cyber")
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surfaceInset)
                    .border(1.2.dp, colors.secondaryTelemetry.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                    .kansoTactileClickable(onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    var y = 0f
                    val step = 4.dp.toPx()
                    while (y < size.height) {
                        drawLine(
                            color = colors.secondaryTelemetry.copy(alpha = 0.06f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                        y += step
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SYS.CHRONO // LIVE",
                            color = colors.secondaryTelemetry,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(colors.secondaryTelemetry)
                        )
                    }
                    Text(
                        text = "$hoursText:${clock.minutes}",
                        color = colors.secondaryTelemetry,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "${clock.formattedDate} // $tzBadge",
                        color = colors.inkSecondary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        MechaWidgetTheme.TACTICAL_GAUGE -> {
            // Pure Neumorph: Soft recessed capsule dial
            Box(
                modifier = modifier
                    .testTag("stitch_clock_neumorph")
                    .fillMaxWidth()
                    .kansoNeumorphic(raised = true, cornerRadius = 28.dp)
                    .kansoTactileClickable(onClick = onClick)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .kansoNeumorphic(raised = false, cornerRadius = 20.dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$hoursText:${clock.minutes}",
                            color = colors.inkPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            letterSpacing = (-1).sp
                        )
                        Text(
                            text = clock.formattedDate,
                            color = colors.inkSecondary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .kansoNeumorphic(raised = true, cornerRadius = 14.dp)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = tzBadge,
                            color = colors.accent,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        MechaWidgetTheme.DEFAULT -> {
            // Kanso Hybrid: Tactile carbon chassis with Signal Amber highlight bar & GMT pill
            Row(
                modifier = modifier
                    .testTag("stitch_clock_hybrid")
                    .fillMaxWidth()
                    .kansoNeumorphic(raised = true, cornerRadius = 24.dp)
                    .kansoTactileClickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(colors.accent, colors.accent.copy(alpha = 0.35f))
                                )
                            )
                    )
                    Column {
                        Text(
                            text = "$hoursText:${clock.minutes}",
                            color = colors.inkPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 34.sp,
                            letterSpacing = (-1.2).sp
                        )
                        Text(
                            text = clock.formattedDate,
                            color = colors.inkSecondary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.accent)
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = tzBadge,
                            color = colors.onAccent,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                    Text(
                        text = if (use24Hour) "24H // SYNC" else "12H // SYNC",
                        color = colors.inkSecondary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 8.5.sp
                    )
                }
            }
        }
    }
}
