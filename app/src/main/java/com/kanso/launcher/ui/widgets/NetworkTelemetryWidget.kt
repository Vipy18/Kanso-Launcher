package com.kanso.launcher.ui.widgets

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kanso.launcher.data.model.MechaWidgetTheme
import com.kanso.launcher.data.model.TelemetryMetric
import com.kanso.launcher.data.telemetry.MetricReadout
import com.kanso.launcher.ui.theme.JetBrainsMonoFontFamily
import com.kanso.launcher.ui.theme.LocalKansoColors
import com.kanso.launcher.ui.theme.WithMechaWidgetTheme
import com.kanso.launcher.ui.theme.drawMachinedArcTicks
import com.kanso.launcher.ui.theme.kansoTactileClickable
import com.kanso.launcher.ui.theme.mechaMetallicChassis

@Composable
fun AudioDotVisualizer(
    bands: List<Float>,
    isActive: Boolean,
    height: Dp = 74.dp,
    modifier: Modifier = Modifier
) {
    val colors = LocalKansoColors.current
    Canvas(
        modifier = modifier
            .testTag("audio_dot_visualizer")
            .size(width = 16.dp, height = height)
    ) {
        val rows = bands.size.coerceAtLeast(12)
        val spacingY = size.height / rows
        val maxCols = 3
        val barW = 3.2.dp.toPx()
        val barH = (spacingY * 0.56f).coerceAtLeast(2.dp.toPx())
        val colStep = 4.8.dp.toPx()

        for (i in 0 until rows) {
            val level = bands.getOrElse(i) { 0.14f }.coerceIn(0.08f, 1f)
            val y = i * spacingY + (spacingY - barH) / 2f
            val activeCols = if (isActive) {
                (level * maxCols).toInt().coerceIn(1, maxCols)
            } else {
                if (i % 3 == 0) 2 else 1
            }

            for (c in 0 until maxCols) {
                val lit = c < activeCols
                val barColor = when {
                    isActive && lit && (c == activeCols - 1 || level > 0.62f) -> colors.accent
                    lit -> colors.inkPrimary.copy(alpha = 0.82f)
                    else -> colors.inkSecondary.copy(alpha = 0.16f)
                }
                drawRect(
                    color = barColor,
                    topLeft = Offset(1.dp.toPx() + c * colStep, y),
                    size = Size(barW, barH)
                )
            }
        }
    }
}

/**
 * Sharp machined metallic Telemetry Unit with calibrated radial ticks and accent-only indicators.
 */
@Composable
fun NetworkTelemetryWidget(
    readout: MetricReadout,
    metric: TelemetryMetric = TelemetryMetric.NET_SPEED,
    mechaTheme: MechaWidgetTheme = MechaWidgetTheme.DEFAULT,
    audioBands: List<Float> = emptyList(),
    isAudioActive: Boolean = false,
    showAudioVisualizer: Boolean = true,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    WithMechaWidgetTheme(mechaTheme) {
        val colors = LocalKansoColors.current
        val animatedGauge by animateFloatAsState(
            targetValue = readout.gaugeFraction.coerceIn(0.08f, 1f),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "telemetryGaugeSweep"
        )

        BoxWithConstraints(
            modifier = modifier.testTag("widget_telemetry_${metric.name.lowercase()}")
        ) {
            val availW = if (maxWidth.value.isFinite() && maxWidth > 40.dp) maxWidth else 182.dp
            val availH = if (maxHeight.value.isFinite() && maxHeight > 40.dp) maxHeight else 148.dp
            val compact = availW < 146.dp || availH < 120.dp

            if (compact) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .mechaMetallicChassis(
                            theme = mechaTheme,
                            darkPlate = false,
                            accentActive = animatedGauge > 0.68f,
                            cutDp = 12.dp
                        )
                        .kansoTactileClickable(onLongClick = onLongClick, onClick = onClick)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(minOf(availW, availH) * 0.80f)) {
                        drawMachinedArcTicks(
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.minDimension * 0.48f,
                            tickCount = 18,
                            activeFraction = animatedGauge,
                            accent = colors.accent,
                            inactiveColor = colors.inkSecondary.copy(alpha = 0.24f)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = readout.primaryValue,
                            color = colors.inkPrimary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                        Text(
                            text = metric.shortCode,
                            color = colors.accent,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            letterSpacing = 0.8.sp,
                            maxLines = 1
                        )
                    }
                }
            } else {
                val cardW = (availW - if (showAudioVisualizer) 20.dp else 0.dp).coerceIn(136.dp, 240.dp)
                val cardH = availH.coerceIn(124.dp, 220.dp)
                val scale = (minOf(cardW, cardH) / 146.dp).coerceIn(0.80f, 1.35f)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (showAudioVisualizer) {
                        AudioDotVisualizer(
                            bands = if (audioBands.isEmpty()) List(12) { 0.14f } else audioBands,
                            isActive = isAudioActive,
                            height = (cardH * 0.72f).coerceIn(72.dp, 130.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .size(width = cardW, height = cardH)
                            .mechaMetallicChassis(
                                theme = mechaTheme,
                                darkPlate = false,
                                accentActive = animatedGauge > 0.65f,
                                cutDp = (14f * scale).dp
                            )
                            .kansoTactileClickable(onLongClick = onLongClick, onClick = onClick)
                            .padding((11f * scale).dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top telemetry header strip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SYS // ${metric.shortCode}",
                                color = colors.accent,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = (8.5f * scale).sp,
                                letterSpacing = 0.9.sp,
                                maxLines = 1
                            )
                            Text(
                                text = mechaTheme.shortCode,
                                color = colors.inkSecondary,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = (7.5f * scale).sp,
                                letterSpacing = 0.7.sp
                            )
                        }

                        // Center dial gauge + digital readout
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier.size((66f * scale).dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val c = Offset(size.width / 2f, size.height / 2f)
                                    val r = size.minDimension * 0.46f
                                    drawMachinedArcTicks(
                                        center = c,
                                        radius = r,
                                        tickCount = 20,
                                        activeFraction = animatedGauge,
                                        accent = colors.accent,
                                        inactiveColor = colors.inkSecondary.copy(alpha = 0.25f)
                                    )
                                    drawArc(
                                        color = colors.accent,
                                        startAngle = -135f,
                                        sweepAngle = animatedGauge * 270f,
                                        useCenter = false,
                                        topLeft = Offset(c.x - r * 0.68f, c.y - r * 0.68f),
                                        size = Size(r * 1.36f, r * 1.36f),
                                        style = Stroke(width = 2.6.dp.toPx() * scale, cap = StrokeCap.Butt)
                                    )
                                }
                                Text(
                                    text = "${(animatedGauge * 100).toInt()}%",
                                    color = colors.inkPrimary,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (9.5f * scale).sp
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = readout.primaryValue,
                                    color = colors.inkPrimary,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (16f * scale).sp,
                                    textAlign = TextAlign.End,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = readout.unitLabel.uppercase(),
                                    color = colors.inkSecondary,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (8.5f * scale).sp,
                                    textAlign = TextAlign.End,
                                    maxLines = 1
                                )
                            }
                        }

                        // Bottom machined linear progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(colors.inkSecondary.copy(alpha = 0.20f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedGauge)
                                    .height(4.dp)
                                    .background(colors.accent)
                            )
                        }
                    }
                }
            }
        }
    }
}
