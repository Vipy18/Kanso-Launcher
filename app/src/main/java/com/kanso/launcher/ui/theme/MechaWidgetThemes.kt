package com.kanso.launcher.ui.theme

import android.graphics.Paint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanso.launcher.data.model.MechaWidgetTheme
import com.kanso.launcher.service.KansoPerformanceGovernor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Strictly preserves the active launcher palette (user Accent + Default Light/Dark surface colors).
 * DEFAULT renders Kanso's primary Neumorphism; optional Mecha themes add brushed-metal chassis,
 * slight softened bevel vertices, neumorphic outer shadows, and live mechanical animations.
 */
@Composable
fun WithMechaWidgetTheme(
    @Suppress("UNUSED_PARAMETER") theme: MechaWidgetTheme,
    content: @Composable () -> Unit
) {
    content()
}

/**
 * Returns the silhouette for each widget theme:
 * - DEFAULT: Classic rounded Neumorphic hardware radius.
 * - MACHINED_MECHA / REACTOR_CORE / TACTICAL_GAUGE / DATA_STREAM: Distinct mecha silhouettes with
 *   subtle softened corner transitions so every widget feels unified with the neumorphic launcher.
 */
@Composable
fun rememberMechaShape(
    theme: MechaWidgetTheme,
    cutDp: Dp = 12.dp
): Shape = remember(theme, cutDp) {
    when (theme) {
        MechaWidgetTheme.DEFAULT -> RoundedCornerShape((cutDp * 1.65f).coerceIn(14.dp, 24.dp))

        MechaWidgetTheme.MACHINED_MECHA -> GenericShape { size, _ ->
            val w = size.width
            val h = size.height
            val cut = (minOf(w, h) * 0.14f).coerceIn(6f, 22f)
            val sm = (cut * 0.42f).coerceAtLeast(4f)
            moveTo(cut, 0f)
            lineTo(w - sm, 0f)
            quadraticTo(w, 0f, w, sm)
            lineTo(w, h - cut)
            lineTo(w - cut, h)
            lineTo(sm, h)
            quadraticTo(0f, h, 0f, h - sm)
            lineTo(0f, cut)
            close()
        }

        MechaWidgetTheme.REACTOR_CORE -> GenericShape { size, _ ->
            val w = size.width
            val h = size.height
            val cut = (minOf(w, h) * 0.17f).coerceIn(8f, 26f)
            val r = (cut * 0.28f).coerceIn(2f, 6f)
            moveTo(cut + r, 0f)
            lineTo(w - cut - r, 0f)
            quadraticTo(w - cut, 0f, w - cut + r * 0.7f, r * 0.7f)
            lineTo(w - r * 0.7f, cut - r * 0.7f)
            quadraticTo(w, cut, w, cut + r)
            lineTo(w, h - cut - r)
            quadraticTo(w, h - cut, w - r * 0.7f, h - cut + r * 0.7f)
            lineTo(w - cut + r * 0.7f, h - r * 0.7f)
            quadraticTo(w - cut, h, w - cut - r, h)
            lineTo(cut + r, h)
            quadraticTo(cut, h, cut - r * 0.7f, h - r * 0.7f)
            lineTo(r * 0.7f, h - cut + r * 0.7f)
            quadraticTo(0f, h - cut, 0f, h - cut - r)
            lineTo(0f, cut + r)
            quadraticTo(0f, cut, r * 0.7f, cut - r * 0.7f)
            lineTo(cut - r * 0.7f, r * 0.7f)
            quadraticTo(cut, 0f, cut + r, 0f)
            close()
        }

        MechaWidgetTheme.TACTICAL_GAUGE -> GenericShape { size, _ ->
            val w = size.width
            val h = size.height
            val c = (minOf(w, h) * 0.13f).coerceIn(6f, 20f)
            val notchW = (w * 0.16f).coerceIn(10f, 30f)
            val notchD = (h * 0.052f).coerceIn(2.5f, 5.5f)
            val cx = w / 2f

            moveTo(c, 0f)
            lineTo(cx - notchW / 2f, 0f)
            lineTo(cx - notchW / 2f + notchD, notchD)
            lineTo(cx + notchW / 2f - notchD, notchD)
            lineTo(cx + notchW / 2f, 0f)
            lineTo(w - c, 0f)
            quadraticTo(w, 0f, w, c)
            lineTo(w, h - c)
            quadraticTo(w, h, w - c, h)
            lineTo(cx + notchW / 2f, h)
            lineTo(cx + notchW / 2f - notchD, h - notchD)
            lineTo(cx - notchW / 2f + notchD, h - notchD)
            lineTo(cx - notchW / 2f, h)
            lineTo(c, h)
            quadraticTo(0f, h, 0f, h - c)
            lineTo(0f, c)
            quadraticTo(0f, 0f, c, 0f)
            close()
        }

        MechaWidgetTheme.DATA_STREAM -> GenericShape { size, _ ->
            val w = size.width
            val h = size.height
            val bigCut = (minOf(w, h) * 0.18f).coerceIn(8f, 24f)
            val r = (minOf(w, h) * 0.08f).coerceIn(4f, 10f)
            moveTo(r, 0f)
            lineTo(w - bigCut, 0f)
            lineTo(w, bigCut)
            lineTo(w, h - r)
            quadraticTo(w, h, w - r, h)
            lineTo(bigCut, h)
            lineTo(0f, h - bigCut)
            lineTo(0f, r)
            quadraticTo(0f, 0f, r, 0f)
            close()
        }
    }
}

/**
 * Unified Kanso Widget Surface Modifier:
 * - DEFAULT: Restores classic raised Neumorphism (soft dual cast shadows, directional 3D bevel,
 *   rounded geometry, and clean optional accent highlight rim).
 * - Optional Mecha Themes (MACHINED_MECHA, REACTOR_CORE, TACTICAL_GAUGE, DATA_STREAM):
 *   Renders outer dual neumorphic shadows & subtle beveled border FIRST (unclipped), then clips
 *   the brushed machined alloy fill, micro-grain, and live accent-only mechanical animations.
 */
fun Modifier.mechaMetallicChassis(
    theme: MechaWidgetTheme,
    darkPlate: Boolean = false,
    accentActive: Boolean = false,
    cutDp: Dp = 12.dp,
    customShape: Shape? = null
): Modifier = composed {
    val colors = LocalKansoColors.current

    // Classic Neumorphism is the PRIMARY DEFAULT style for Kanso Launcher
    if (theme == MechaWidgetTheme.DEFAULT) {
        val corner = (cutDp * 1.65f).coerceIn(14.dp, 24.dp)
        val surface = if (darkPlate) colors.darkModuleBg else null
        return@composed this
            .kansoNeumorphic(
                raised = true,
                cornerRadius = corner,
                surfaceColor = surface,
                customShape = customShape
            )
            .drawWithContent {
                drawContent()
                if (accentActive) {
                    val shape = customShape ?: RoundedCornerShape(corner)
                    val outline = shape.createOutline(size, layoutDirection, this)
                    drawOutline(
                        outline = outline,
                        color = colors.accent.copy(alpha = 0.28f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
    }

    val shape = customShape ?: rememberMechaShape(theme, cutDp)
    val perfState by KansoPerformanceGovernor.state.collectAsStateWithLifecycle()
    val highFidelity = perfState.highFidelityEffectsEnabled

    val phase = if (highFidelity && theme != MechaWidgetTheme.MACHINED_MECHA) {
        val transition = rememberInfiniteTransition(label = "mechaAnim_${theme.name}")
        val duration = when (theme) {
            MechaWidgetTheme.REACTOR_CORE -> 5200
            MechaWidgetTheme.TACTICAL_GAUGE -> 3800
            MechaWidgetTheme.DATA_STREAM -> 3000
            else -> 4000
        }
        val anim by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = duration, easing = LinearEasing),
                repeatMode = if (theme == MechaWidgetTheme.TACTICAL_GAUGE) RepeatMode.Reverse else RepeatMode.Restart
            ),
            label = "mechaPhase_${theme.name}"
        )
        anim
    } else {
        0.35f
    }

    val plateTop: Color
    val plateMid: Color
    val plateBottom: Color
    val rimLight: Color
    val rimDark: Color

    if (darkPlate || colors.isDark) {
        plateTop = colors.darkModuleElevated
        plateMid = colors.darkModuleBg
        plateBottom = Color(0xFF0E1014)
        rimLight = Color.White.copy(alpha = if (accentActive) 0.26f else 0.16f)
        rimDark = Color.Black.copy(alpha = 0.72f)
    } else {
        plateTop = colors.surfaceRaisedHighlight
        plateMid = colors.surfaceRaised
        plateBottom = colors.surfaceInset
        rimLight = Color.White.copy(alpha = 0.88f)
        rimDark = colors.shadowDark.copy(alpha = 0.52f)
    }

    val accent = colors.accent
    val inkSec = colors.inkSecondary

    this
        // Outer unclipped neumorphic dual-source cast shadows (unifies Mecha widgets with Neumorphism)
        .drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            val composePath = Path().apply {
                when (outline) {
                    is Outline.Generic -> addPath(outline.path)
                    is Outline.Rounded -> addRoundRect(outline.roundRect)
                    is Outline.Rectangle -> addRect(outline.rect)
                }
            }
            val androidPath = composePath.asAndroidPath()
            val darkOffset = colors.shadowDarkOffset.toPx() * 0.78f
            val lightOffset = colors.shadowLightOffset.toPx() * 0.72f
            val darkBlur = colors.shadowDarkBlur.toPx().coerceAtLeast(3f)
            val lightBlur = colors.shadowLightBlur.toPx().coerceAtLeast(2.5f)

            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                val darkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = plateMid.toArgb()
                    setShadowLayer(darkBlur, darkOffset, darkOffset, colors.shadowDark.toArgb())
                }
                native.drawPath(androidPath, darkPaint)

                val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = plateMid.toArgb()
                    setShadowLayer(
                        lightBlur,
                        -lightOffset,
                        -lightOffset,
                        colors.shadowLight.copy(alpha = if (colors.isDark) 0.34f else 0.90f).toArgb()
                    )
                }
                native.drawPath(androidPath, lightPaint)
            }
        }
        .clip(shape)
        .drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            // Brushed machined alloy fill
            drawOutline(
                outline = outline,
                brush = Brush.linearGradient(
                    colors = listOf(plateTop, plateMid, plateBottom, plateMid),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )
            )

            // Subtle machined diagonal metallic sheen band
            drawOutline(
                outline = outline,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        rimLight.copy(alpha = rimLight.alpha * 0.35f),
                        Color.Transparent
                    ),
                    start = Offset(size.width * 0.12f, 0f),
                    end = Offset(size.width * 0.78f, size.height)
                )
            )

            // Micro brushed-metal horizontal grain lines
            val grainStep = 4.dp.toPx()
            val grainColor = if (darkPlate || colors.isDark) Color.White.copy(alpha = 0.024f)
            else Color.Black.copy(alpha = 0.022f)
            var gy = grainStep
            while (gy < size.height) {
                drawLine(
                    color = grainColor,
                    start = Offset(0f, gy),
                    end = Offset(size.width, gy),
                    strokeWidth = 0.6.dp.toPx()
                )
                gy += grainStep
            }

            // Neumorphic + metallic outer bevel stroke
            drawOutline(
                outline = outline,
                brush = Brush.linearGradient(
                    colors = listOf(
                        if (accentActive) accent.copy(alpha = 0.80f) else rimLight,
                        inkSec.copy(alpha = 0.32f),
                        rimDark
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                ),
                style = Stroke(width = 1.4.dp.toPx())
            )
        }
        .drawWithContent {
            drawContent()
            drawMechaThemeDetails(
                theme = theme,
                phase = phase,
                accent = accent,
                neutralInk = inkSec,
                accentActive = accentActive,
                highFidelity = highFidelity
            )
        }
}

private fun DrawScope.drawMechaThemeDetails(
    theme: MechaWidgetTheme,
    phase: Float,
    accent: Color,
    neutralInk: Color,
    accentActive: Boolean,
    highFidelity: Boolean
) {
    if (theme == MechaWidgetTheme.DEFAULT) return

    val w = size.width
    val h = size.height
    val pad = 4.dp.toPx()
    val stroke = 1.15.dp.toPx()

    val rivetR = 1.1.dp.toPx()
    val rivetColor = neutralInk.copy(alpha = 0.45f)
    drawCircle(rivetColor, rivetR, Offset(pad + 5.dp.toPx(), pad + 3.dp.toPx()))
    drawCircle(rivetColor, rivetR, Offset(w - pad - 5.dp.toPx(), h - pad - 3.dp.toPx()))

    when (theme) {
        MechaWidgetTheme.DEFAULT -> Unit

        MechaWidgetTheme.MACHINED_MECHA -> {
            val arm = 8.dp.toPx()
            val c = if (accentActive) accent.copy(alpha = 0.85f) else neutralInk.copy(alpha = 0.42f)
            drawLine(c, Offset(w - pad - arm, pad), Offset(w - pad, pad), stroke)
            drawLine(c, Offset(w - pad, pad), Offset(w - pad, pad + arm), stroke)
            drawLine(c, Offset(pad, h - pad - arm), Offset(pad, h - pad), stroke)
            drawLine(c, Offset(pad, h - pad), Offset(pad + arm, h - pad), stroke)
            drawLine(
                color = if (accentActive) accent else accent.copy(alpha = 0.55f),
                start = Offset(w - pad - 12.dp.toPx(), pad + 2.5.dp.toPx()),
                end = Offset(w - pad - 3.dp.toPx(), pad + 2.5.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Butt
            )
        }

        MechaWidgetTheme.REACTOR_CORE -> {
            val cx = w / 2f
            val cy = h / 2f
            val radius = (minOf(w, h) * 0.44f).coerceAtLeast(10.dp.toPx())
            val sweepStart = phase * 360f
            val ringAlpha = if (accentActive) 0.55f else 0.30f

            for (seg in 0 until 3) {
                drawArc(
                    color = accent.copy(alpha = ringAlpha),
                    startAngle = sweepStart + seg * 120f,
                    sweepAngle = 52f,
                    useCenter = false,
                    topLeft = Offset(cx - radius, cy - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Butt)
                )
            }

            val cut = 11.dp.toPx()
            drawLine(accent.copy(alpha = 0.78f), Offset(pad, cut), Offset(cut, pad), 1.6.dp.toPx())
            drawLine(accent.copy(alpha = 0.78f), Offset(w - cut, h - pad), Offset(w - pad, h - cut), 1.6.dp.toPx())
            drawLine(neutralInk.copy(alpha = 0.42f), Offset(w - cut, pad), Offset(w - pad, cut), stroke)
            drawLine(neutralInk.copy(alpha = 0.42f), Offset(pad, h - cut), Offset(cut, h - pad), stroke)
        }

        MechaWidgetTheme.TACTICAL_GAUGE -> {
            val tickCount = ((w / 10.dp.toPx()).toInt()).coerceIn(6, 18)
            val usableW = (w - 24.dp.toPx()).coerceAtLeast(12f)
            val startX = 12.dp.toPx()
            val step = usableW / tickCount
            for (i in 0..tickCount) {
                val x = startX + i * step
                val major = i % 3 == 0
                val len = if (major) 4.5.dp.toPx() else 2.2.dp.toPx()
                drawLine(
                    color = if (major) accent.copy(alpha = 0.65f) else neutralInk.copy(alpha = 0.35f),
                    start = Offset(x, pad),
                    end = Offset(x, pad + len),
                    strokeWidth = if (major) 1.1.dp.toPx() else 0.75.dp.toPx()
                )
            }

            val cursorX = startX + usableW * phase
            drawLine(
                color = accent.copy(alpha = if (accentActive) 0.95f else 0.72f),
                start = Offset(cursorX - 4.dp.toPx(), h - pad - 1.5.dp.toPx()),
                end = Offset(cursorX + 4.dp.toPx(), h - pad - 1.5.dp.toPx()),
                strokeWidth = 2.2.dp.toPx()
            )

            val cy = h / 2f
            drawLine(accent.copy(alpha = 0.65f), Offset(pad, cy), Offset(pad + 5.dp.toPx(), cy), stroke)
            drawLine(accent.copy(alpha = 0.65f), Offset(w - pad - 5.dp.toPx(), cy), Offset(w - pad, cy), stroke)
        }

        MechaWidgetTheme.DATA_STREAM -> {
            if (highFidelity) {
                val beamX = w * phase
                val beamW = (w * 0.22f).coerceIn(14.dp.toPx(), 42.dp.toPx())
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            accent.copy(alpha = if (accentActive) 0.14f else 0.08f),
                            Color.Transparent
                        ),
                        startX = beamX - beamW / 2f,
                        endX = beamX + beamW / 2f
                    ),
                    topLeft = Offset(beamX - beamW / 2f, 0f),
                    size = Size(beamW, h)
                )
            }

            val cut = minOf(w, h) * 0.18f
            drawLine(
                color = accent.copy(alpha = 0.88f),
                start = Offset(w - cut, 1.dp.toPx()),
                end = Offset(w - 1.dp.toPx(), cut),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = accent.copy(alpha = 0.60f),
                start = Offset(1.dp.toPx(), h - cut),
                end = Offset(cut, h - 1.dp.toPx()),
                strokeWidth = 1.6.dp.toPx()
            )

            for (i in 0 until 4) {
                val lit = ((phase * 4f).toInt() % 4) == i
                drawLine(
                    color = if (lit) accent else neutralInk.copy(alpha = 0.32f),
                    start = Offset(pad + 6.dp.toPx() + i * 5.dp.toPx(), pad + 2.dp.toPx()),
                    end = Offset(pad + 9.dp.toPx() + i * 5.dp.toPx(), pad + 2.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}

fun Modifier.mechaWidgetThemeOverlay(theme: MechaWidgetTheme): Modifier = composed {
    if (theme == MechaWidgetTheme.DEFAULT) return@composed this
    val colors = LocalKansoColors.current
    val perfState by KansoPerformanceGovernor.state.collectAsStateWithLifecycle()
    val highFidelity = perfState.highFidelityEffectsEnabled
    this.drawWithContent {
        drawContent()
        drawMechaThemeDetails(
            theme = theme,
            phase = 0.35f,
            accent = colors.accent,
            neutralInk = colors.inkSecondary,
            accentActive = false,
            highFidelity = highFidelity
        )
    }
}

/**
 * Helper to draw a calibrated radial tick ring (used in Telemetry & Weather widgets).
 */
fun DrawScope.drawMachinedArcTicks(
    center: Offset,
    radius: Float,
    tickCount: Int,
    activeFraction: Float,
    accent: Color,
    inactiveColor: Color,
    startAngleDeg: Float = -135f,
    sweepAngleDeg: Float = 270f
) {
    val activeTicks = (tickCount * activeFraction.coerceIn(0f, 1f)).toInt()
    val innerR = radius - 4.5.dp.toPx()
    val outerR = radius
    for (i in 0 until tickCount) {
        val frac = i.toFloat() / (tickCount - 1).coerceAtLeast(1)
        val angleRad = (startAngleDeg + sweepAngleDeg * frac) * (PI / 180.0)
        val cosA = cos(angleRad).toFloat()
        val sinA = sin(angleRad).toFloat()
        val lit = i <= activeTicks
        drawLine(
            color = if (lit) accent else inactiveColor,
            start = Offset(center.x + innerR * cosA, center.y + innerR * sinA),
            end = Offset(center.x + outerR * cosA, center.y + outerR * sinA),
            strokeWidth = if (lit) 2.2.dp.toPx() else 1.2.dp.toPx(),
            cap = StrokeCap.Butt
        )
    }
}
