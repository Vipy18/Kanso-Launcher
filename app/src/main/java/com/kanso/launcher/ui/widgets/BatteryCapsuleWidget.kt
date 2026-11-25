package com.kanso.launcher.ui.widgets

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kanso.launcher.R
import com.kanso.launcher.data.model.MechaWidgetTheme
import com.kanso.launcher.ui.theme.GeistFontFamily
import com.kanso.launcher.ui.theme.JetBrainsMonoFontFamily
import com.kanso.launcher.ui.theme.LocalKansoColors
import com.kanso.launcher.ui.theme.WithMechaWidgetTheme
import com.kanso.launcher.ui.theme.kansoNeumorphic
import com.kanso.launcher.ui.theme.kansoTactileClickable
import com.kanso.launcher.ui.theme.mechaMetallicChassis
import kotlin.math.hypot

/**
 * Classic tactile stadium Battery pill with circular dot-matrix halftone cell (DEFAULT)
 * or optional machined Mecha power cell when a Mecha theme is selected.
 */
@Composable
fun BatteryCapsuleWidget(
    batteryPercent: Int,
    isCharging: Boolean,
    mechaTheme: MechaWidgetTheme = MechaWidgetTheme.DEFAULT,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    WithMechaWidgetTheme(mechaTheme) {
        val colors = LocalKansoColors.current
        val fillRatio by animateFloatAsState(
            targetValue = (batteryPercent / 100f).coerceIn(0.05f, 1f),
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "batteryCellFill"
        )

        if (mechaTheme == MechaWidgetTheme.DEFAULT) {
            Row(
                modifier = modifier
                    .testTag("widget_battery_capsule")
                    .height(48.dp)
                    .kansoNeumorphic(
                        raised = true,
                        cornerRadius = 24.dp,
                        surfaceColor = colors.surfaceRaised
                    )
                    .kansoTactileClickable(onLongClick = onLongClick, onClick = onClick)
                    .padding(start = 10.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Circular dot-matrix halftone battery indicator
                Canvas(modifier = Modifier.size(30.dp)) {
                    val grid = 7
                    val step = size.minDimension / grid
                    val center = (grid - 1) / 2f
                    val maxDist = center + 0.35f
                    val dotRadius = (step * 0.27f).coerceAtLeast(1.dp.toPx())
                    val litThresholdRow = grid - (fillRatio * grid)

                    for (r in 0 until grid) {
                        for (c in 0 until grid) {
                            val dist = hypot(c - center, r - center)
                            if (dist <= maxDist) {
                                val edgeFactor = (1f - (dist / (maxDist + 0.6f)) * 0.45f).coerceIn(0.45f, 1f)
                                val isLit = r >= litThresholdRow - 0.35f
                                val dotColor = when {
                                    isLit && isCharging -> colors.accent
                                    isLit -> colors.inkPrimary.copy(alpha = 0.90f)
                                    else -> colors.inkSecondary.copy(alpha = 0.28f)
                                }
                                drawCircle(
                                    color = dotColor,
                                    radius = dotRadius * edgeFactor,
                                    center = Offset(
                                        x = (c + 0.5f) * step,
                                        y = (r + 0.5f) * step
                                    )
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCharging) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    Text(
                        text = "$batteryPercent% Battery",
                        color = colors.inkPrimary,
                        fontFamily = GeistFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.5.sp,
                        maxLines = 1
                    )
                }
            }
            return@WithMechaWidgetTheme
        }

        Row(
            modifier = modifier
                .testTag("widget_battery_capsule")
                .height(48.dp)
                .mechaMetallicChassis(
                    theme = mechaTheme,
                    darkPlate = false,
                    accentActive = isCharging || batteryPercent <= 20,
                    cutDp = 10.dp
                )
                .kansoTactileClickable(onLongClick = onLongClick, onClick = onClick)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Canvas(modifier = Modifier.size(width = 34.dp, height = 18.dp)) {
                val segs = 6
                val gap = 1.8.dp.toPx()
                val segW = (size.width - gap * (segs - 1)) / segs
                val litSegs = (fillRatio * segs).toInt().coerceIn(1, segs)
                for (i in 0 until segs) {
                    val lit = i < litSegs
                    val x = i * (segW + gap)
                    drawRect(
                        color = when {
                            lit && isCharging -> colors.accent
                            lit && i == litSegs - 1 -> colors.accent
                            lit -> colors.inkPrimary
                            else -> colors.inkSecondary.copy(alpha = 0.22f)
                        },
                        topLeft = Offset(x, 2.dp.toPx()),
                        size = Size(segW, size.height - 4.dp.toPx())
                    )
                }
            }

            Column(verticalArrangement = Arrangement.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCharging) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    Text(
                        text = "$batteryPercent%",
                        color = colors.inkPrimary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
                Text(
                    text = if (isCharging) "CHG // CELL" else stringResource(R.string.battery_unit_tag),
                    color = if (isCharging) colors.accent else colors.inkSecondary,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 7.5.sp,
                    letterSpacing = 0.7.sp,
                    maxLines = 1
                )
            }
        }
    }
}
