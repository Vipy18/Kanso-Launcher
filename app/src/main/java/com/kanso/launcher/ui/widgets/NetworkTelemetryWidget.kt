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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.kanso.launcher.ui.theme.SpaceGroteskFontFamily
import com.kanso.launcher.ui.theme.WithMechaWidgetTheme
import com.kanso.launcher.ui.theme.drawMachinedArcTicks
import com.kanso.launcher.ui.theme.kansoNeumorphic
import com.kanso.launcher.ui.theme.kansoTactileClickable
import com.kanso.launcher.ui.theme.mechaMetallicChassis

@Composable
fun AudioDotVisualizer(
    bands: List<Float>,
    isActive: Boolean,
    height: Dp = 84.dp,
    modifier: Modifier = Modifier
) {
    val colors = LocalKansoColors.current
    Canvas(
        modifier = modifier
            .testTag("audio_dot_visualizer")
            .size(width = 16.dp, height = height)
    ) {
        val rows = bands.size.coerceAtLeast(13)
        val spacingY = size.height / rows
        val maxCols = 3
        val dotR = 1.35.dp.toPx()
        val colStep = 4.4.dp.toPx()

        for (i in 0 until rows) {
            val level = bands.getOrElse(i) { 0.14f }.coerceIn(0.08f, 1f)
            val cy = i * spacingY + spacingY / 2f
            val activeCols = if (isActive) {
                (level * maxCols).toInt().coerceIn(1, maxCols)
            } else {
                when {
                    i < 3 -> 2
                    i < 8 -> 1
                    else -> 1
                }
            }

            for (c in 0 until maxCols) {
                val lit = c < activeCols
                val fade = (1f - (i.toFloat() / rows) * 0.55f).coerceIn(0.28f, 1f)
                val dotColor = when {
                    isActive && lit && (c == activeCols - 1 || level > 0.60f) -> colors.accent
                    lit -> colors.inkPrimary.copy(alpha = 0.88f * fade)
                    else -> colors.inkSecondary.copy(alpha = 0.24f * fade)
                }
                drawCircle(
                    color = dotColor,
                    radius = if (lit) dotR else dotR * 0.72f,
                    center = Offset(2.dp.toPx() + c * colStep, cy)
                )
            }
        }
    }
}

/**
 * Classic -45° tilted diagonal stadium Telemetry pill with upright readout and circular
 * radial progress ring (DEFAULT), or optional machined Mecha telemetry unit.
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
            val availW = if (maxWidth.value.isFinite() && maxWidth > 40.dp) maxWidth else 184.dp
            val availH = if (maxHeight.value.isFinite() && maxHeight > 40.dp) maxHeight else 160.dp
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
            } else if (mechaTheme == MechaWidgetTheme.DEFAULT) {
                val visSpace = if (showAudioVisualizer) 20.dp else 0.dp
                val diagBoxSize = minOf(
                    (availW - visSpace).coerceIn(138.dp, 162.dp),
                    availH.coerceIn(138.dp, 162.dp)
                )
                val scale = (diagBoxSize / 156.dp).coerceIn(0.84f, 1.12f)

                // Exact -45° rotated stadium math so pill stays strictly inside diagBoxSize
                // Rotated bbox = (pillW - pillH) * 0.70710677f + pillH == innerSpan <= diagBoxSize - 10.dp
                val pillH = diagBoxSize * 0.57f
                val innerSpan = (diagBoxSize - 10.dp).coerceAtLeast(pillH + 16.dp)
                val pillW = pillH + (innerSpan - pillH) / 0.70710677f
                // Exact offset from Box center to each semicircle cap center along -45°
                val capOffset = (pillW - pillH) * 0.5f * 0.70710677f
                val ringSize = (pillH - 16.dp * scale).coerceAtLeast(54.dp)

                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (showAudioVisualizer) {
                        AudioDotVisualizer(
                            bands = if (audioBands.isEmpty()) List(13) { 0.14f } else audioBands,
                            isActive = isAudioActive,
                            height = (diagBoxSize * 0.60f).coerceIn(72.dp, 102.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(diagBoxSize)
                            .kansoTactileClickable(onLongClick = onLongClick, onClick = onClick),
                        contentAlignment = Alignment.Center
                    ) {
                        // Tilted -45° extruded stadium capsule hull (requiredSize prevents parent clamping before rotation)
                        Box(
                            modifier = Modifier
                                .requiredSize(width = pillW, height = pillH)
                                .graphicsLayer { rotationZ = -45f }
                                .kansoNeumorphic(
                                    raised = true,
                                    cornerRadius = pillH / 2f
                                )
                        )

                        // Upper-right lobe: mathematically centered inside upper-right semicircle cap
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(x = capOffset, y = -capOffset)
                                .width((pillH - 12.dp * scale).coerceAtLeast(62.dp)),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = readout.primaryValue,
                                color = colors.inkPrimary,
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = (16f * scale).sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = readout.unitLabel,
                                color = colors.inkSecondary,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = (9.5f * scale).sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Lower-left lobe: concentric circular dial ring inside lower-left semicircle cap
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(x = -capOffset, y = capOffset)
                                .size(ringSize),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokePx = 4.6.dp.toPx() * scale
                                val inset = strokePx / 2f + 1.dp.toPx()
                                val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
                                val cx = size.width / 2f
                                val cy = size.height / 2f

                                // Subtle inner dial disc matching classic dark charcoal gauge face
                                drawCircle(
                                    color = if (colors.isDark) colors.darkModuleBg else colors.surfaceInset,
                                    radius = size.minDimension / 2f - strokePx,
                                    center = Offset(cx, cy)
                                )

                                // Base circular gauge track
                                drawArc(
                                    color = colors.trackMuted.copy(alpha = 0.72f),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = Offset(inset, inset),
                                    size = arcSize,
                                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                                )

                                // Active sweep arc
                                drawArc(
                                    color = colors.accent,
                                    startAngle = -90f,
                                    sweepAngle = (animatedGauge * 330f).coerceIn(22f, 350f),
                                    useCenter = false,
                                    topLeft = Offset(inset, inset),
                                    size = arcSize,
                                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                                )

                                // 4 subtle compass calibration micro-ticks inside dial
                                val tickOuter = size.minDimension / 2f - strokePx - 3.dp.toPx()
                                val tickInner = tickOuter - 3.dp.toPx()
                                val tickColor = colors.inkSecondary.copy(alpha = 0.38f)
                                val tickW = 1.dp.toPx()
                                drawLine(tickColor, Offset(cx, cy - tickOuter), Offset(cx, cy - tickInner), tickW)
                                drawLine(tickColor, Offset(cx, cy + tickOuter), Offset(cx, cy + tickInner), tickW)
                                drawLine(tickColor, Offset(cx - tickOuter, cy), Offset(cx - tickInner, cy), tickW)
                                drawLine(tickColor, Offset(cx + tickOuter, cy), Offset(cx + tickInner, cy), tickW)
                            }
                            Text(
                                text = metric.shortCode,
                                color = colors.inkSecondary,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = (9.5f * scale).sp,
                                letterSpacing = 0.6.sp,
                                maxLines = 1
                            )
                        }
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
