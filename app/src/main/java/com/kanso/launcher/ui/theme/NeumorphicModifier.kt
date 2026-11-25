package com.kanso.launcher.ui.theme

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.withSave
import androidx.core.graphics.withTranslation
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Enhanced 3D Mecha-Industrial Neumorphic modifier:
 * Combines dual-source cast shadows (Paint.setShadowLayer + soft ambient offset pass),
 * overhead directional 3D surface sheen gradient, and crisp specular bevel rim so widgets
 * and icons render with pronounced physical depth across all Android GPUs and emulators.
 */
fun Modifier.kansoNeumorphic(
    raised: Boolean = true,
    cornerRadius: Dp = 20.dp,
    surfaceColor: Color? = null,
    pressed: Boolean = false,
    customShape: Shape? = null
): Modifier = composed {
    val colors = LocalKansoColors.current
    val fillColor = surfaceColor ?: if (raised) colors.surfaceRaised else colors.surfaceInset
    val depthFactor = if (pressed) 0.42f else 1f

    this.drawBehind {
        val radiusPx = cornerRadius.toPx().coerceAtMost(minOf(size.width, size.height) / 2f)
        val rect = RectF(0f, 0f, size.width, size.height)
        val lightBlur = (colors.shadowLightBlur.toPx() * depthFactor).coerceAtLeast(1.5f)
        val darkBlur = (colors.shadowDarkBlur.toPx() * depthFactor).coerceAtLeast(2f)
        val lightOffset = colors.shadowLightOffset.toPx() * depthFactor
        val darkOffset = colors.shadowDarkOffset.toPx() * depthFactor

        // Subtle overhead 3D bevel gradient for raised hardware cards
        val topBevelColor = if (surfaceColor == null && raised) {
            colors.surfaceRaisedHighlight
        } else {
            fillColor.copy(alpha = 1f)
        }
        val bottomBevelColor = if (surfaceColor == null && raised) {
            if (colors.isDark) Color(0xFF18191E) else Color(0xFFE5E8EE)
        } else {
            fillColor
        }

        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas

            if (customShape != null) {
                val outline = customShape.createOutline(size, layoutDirection, this)
                val composePath = Path().apply {
                    when (outline) {
                        is androidx.compose.ui.graphics.Outline.Generic -> addPath(outline.path)
                        is androidx.compose.ui.graphics.Outline.Rounded -> addRoundRect(outline.roundRect)
                        is androidx.compose.ui.graphics.Outline.Rectangle -> addRect(outline.rect)
                    }
                }
                val androidPath = composePath.asAndroidPath()

                if (raised) {
                    // Ambient contact shadow pass (guarantees visible 3D lift on every renderer)
                    nativeCanvas.withTranslation(darkOffset * 0.65f, darkOffset * 0.85f) {
                        val contactPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.FILL
                            color = colors.shadowDark.copy(alpha = if (colors.isDark) 0.55f else 0.42f).toArgb()
                        }
                        drawPath(androidPath, contactPaint)
                    }

                    val darkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.FILL
                        color = fillColor.toArgb()
                        setShadowLayer(darkBlur, darkOffset, darkOffset, colors.shadowDark.toArgb())
                    }
                    nativeCanvas.drawPath(androidPath, darkPaint)

                    val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.FILL
                        color = fillColor.toArgb()
                        setShadowLayer(lightBlur, -lightOffset, -lightOffset, colors.shadowLight.toArgb())
                    }
                    nativeCanvas.drawPath(androidPath, lightPaint)
                }

                val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    shader = LinearGradient(
                        0f, 0f, size.width, size.height,
                        topBevelColor.toArgb(),
                        bottomBevelColor.toArgb(),
                        Shader.TileMode.CLAMP
                    )
                }
                nativeCanvas.drawPath(androidPath, fillPaint)

                val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 1.dp.toPx()
                    shader = LinearGradient(
                        0f, 0f, size.width, size.height,
                        colors.shadowLight.copy(alpha = if (colors.isDark) 0.35f else 0.92f).toArgb(),
                        colors.shadowDark.copy(alpha = if (colors.isDark) 0.55f else 0.35f).toArgb(),
                        Shader.TileMode.CLAMP
                    )
                }
                nativeCanvas.drawPath(androidPath, rimPaint)
                return@drawIntoCanvas
            }

            if (raised) {
                // Soft physical drop offset shadow + top-left specular glow
                val contactRect = RectF(
                    darkOffset * 0.35f,
                    darkOffset * 0.55f,
                    size.width + darkOffset * 0.65f,
                    size.height + darkOffset * 0.85f
                )
                val contactPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = colors.shadowDark.copy(alpha = if (colors.isDark) 0.45f else 0.36f).toArgb()
                }
                nativeCanvas.drawRoundRect(contactRect, radiusPx, radiusPx, contactPaint)

                val darkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = fillColor.toArgb()
                    setShadowLayer(darkBlur, darkOffset, darkOffset, colors.shadowDark.toArgb())
                }
                nativeCanvas.drawRoundRect(rect, radiusPx, radiusPx, darkPaint)

                val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = fillColor.toArgb()
                    setShadowLayer(lightBlur, -lightOffset, -lightOffset, colors.shadowLight.toArgb())
                }
                nativeCanvas.drawRoundRect(rect, radiusPx, radiusPx, lightPaint)

                // Directional 3D surface fill
                val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    if (surfaceColor == null) {
                        shader = LinearGradient(
                            0f, 0f, size.width, size.height,
                            topBevelColor.toArgb(),
                            bottomBevelColor.toArgb(),
                            Shader.TileMode.CLAMP
                        )
                    } else {
                        color = fillColor.toArgb()
                    }
                }
                nativeCanvas.drawRoundRect(rect, radiusPx, radiusPx, fillPaint)

                // Crisp 3D top-left light to bottom-right shadow bevel rim
                val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 1.dp.toPx()
                    shader = LinearGradient(
                        0f, 0f, size.width, size.height,
                        colors.shadowLight.copy(alpha = if (colors.isDark) 0.36f else 0.90f).toArgb(),
                        colors.shadowDark.copy(alpha = if (colors.isDark) 0.50f else 0.32f).toArgb(),
                        Shader.TileMode.CLAMP
                    )
                }
                nativeCanvas.drawRoundRect(rect, radiusPx, radiusPx, rimPaint)
            } else {
                // Recessed inset well with clipped dual inner bevel shadows
                val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = fillColor.toArgb()
                }
                nativeCanvas.drawRoundRect(rect, radiusPx, radiusPx, fillPaint)

                nativeCanvas.withSave {
                    val clipPath = android.graphics.Path().apply {
                        addRoundRect(rect, radiusPx, radiusPx, android.graphics.Path.Direction.CW)
                    }
                    clipPath(clipPath)

                    val strokeW = 3.5.dp.toPx()
                    val topLeftInner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = strokeW
                        color = colors.shadowDark.copy(alpha = if (colors.isDark) 0.80f else 0.72f).toArgb()
                        setShadowLayer(6.dp.toPx(), 2.5.dp.toPx(), 2.5.dp.toPx(), colors.shadowDark.toArgb())
                    }
                    drawRoundRect(
                        RectF(-strokeW / 2f, -strokeW / 2f, size.width + strokeW, size.height + strokeW),
                        radiusPx,
                        radiusPx,
                        topLeftInner
                    )

                    val bottomRightInner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = strokeW * 0.85f
                        color = colors.shadowLight.copy(alpha = if (colors.isDark) 0.45f else 0.90f).toArgb()
                        setShadowLayer(5.dp.toPx(), -2.dp.toPx(), -2.dp.toPx(), colors.shadowLight.toArgb())
                    }
                    drawRoundRect(
                        RectF(-strokeW, -strokeW, size.width + strokeW / 2f, size.height + strokeW / 2f),
                        radiusPx,
                        radiusPx,
                        bottomRightInner
                    )
                }
            }
        }
    }
}

/**
 * Lightweight raised neumorphic outer shadow & micro-bevel halo for app icons, rounded-square
 * folders, and compact buttons without shrinking their inner artwork.
 */
fun Modifier.kansoIconNeumorphic(
    cornerRadius: Dp = 14.dp,
    shadowScale: Float = 0.65f
): Modifier = composed {
    val colors = LocalKansoColors.current
    this.drawBehind {
        val radiusPx = cornerRadius.toPx().coerceAtMost(minOf(size.width, size.height) / 2f)
        val rect = RectF(0f, 0f, size.width, size.height)
        val darkOffset = (colors.shadowDarkOffset.toPx() * shadowScale).coerceAtLeast(1.5f)
        val lightOffset = (colors.shadowLightOffset.toPx() * shadowScale).coerceAtLeast(1f)
        val darkBlur = (colors.shadowDarkBlur.toPx() * shadowScale).coerceAtLeast(2.5f)
        val lightBlur = (colors.shadowLightBlur.toPx() * shadowScale).coerceAtLeast(2f)

        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            val darkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = colors.surfaceRaised.toArgb()
                setShadowLayer(
                    darkBlur,
                    darkOffset,
                    darkOffset,
                    colors.shadowDark.copy(alpha = if (colors.isDark) 0.72f else 0.55f).toArgb()
                )
            }
            nativeCanvas.drawRoundRect(rect, radiusPx, radiusPx, darkPaint)

            val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = colors.surfaceRaised.toArgb()
                setShadowLayer(
                    lightBlur,
                    -lightOffset,
                    -lightOffset,
                    colors.shadowLight.copy(alpha = if (colors.isDark) 0.28f else 0.85f).toArgb()
                )
            }
            nativeCanvas.drawRoundRect(rect, radiusPx, radiusPx, lightPaint)
        }
    }
}

/**
 * Tactile spring press scale (0.94f) + haptic feedback supporting BOTH tap and long-press
 * so any widget, icon, dock slot, or control can smoothly trigger Edit Mode on long-press.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.kansoTactileClickable(
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "kansoPressScale"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onLongClick = onLongClick?.let { longAction ->
                {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    longAction()
                }
            },
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
        )
}

/**
 * Overlays subtle sharp industrial/mecha/cyberpunk micro-details (hairline 90-degree corner
 * brackets, micro calibration ticks, and optional active accent LED pip) onto rounded
 * neumorphic modules without breaking their soft ergonomic silhouette.
 */
fun Modifier.kansoMechaAccents(
    accentActive: Boolean = false,
    showCornerBrackets: Boolean = true,
    showLedPip: Boolean = false
): Modifier = composed {
    val colors = LocalKansoColors.current
    this.drawBehind {
        val stroke = 1.dp.toPx()
        val inset = 5.dp.toPx()
        val arm = 6.dp.toPx()
        val bracketColor = if (accentActive) {
            colors.accent.copy(alpha = 0.78f)
        } else {
            colors.inkSecondary.copy(alpha = if (colors.isDark) 0.34f else 0.28f)
        }

        if (showCornerBrackets && size.width > 36.dp.toPx() && size.height > 36.dp.toPx()) {
            // Top-left sharp L-bracket
            drawLine(
                color = bracketColor,
                start = androidx.compose.ui.geometry.Offset(inset, inset + arm),
                end = androidx.compose.ui.geometry.Offset(inset, inset),
                strokeWidth = stroke
            )
            drawLine(
                color = bracketColor,
                start = androidx.compose.ui.geometry.Offset(inset, inset),
                end = androidx.compose.ui.geometry.Offset(inset + arm, inset),
                strokeWidth = stroke
            )
            // Bottom-right sharp L-bracket
            drawLine(
                color = bracketColor,
                start = androidx.compose.ui.geometry.Offset(size.width - inset - arm, size.height - inset),
                end = androidx.compose.ui.geometry.Offset(size.width - inset, size.height - inset),
                strokeWidth = stroke
            )
            drawLine(
                color = bracketColor,
                start = androidx.compose.ui.geometry.Offset(size.width - inset, size.height - inset - arm),
                end = androidx.compose.ui.geometry.Offset(size.width - inset, size.height - inset),
                strokeWidth = stroke
            )
        }

        if (showLedPip) {
            val pipR = 2.dp.toPx()
            drawCircle(
                color = if (accentActive) colors.accent else colors.inkSecondary.copy(alpha = 0.3f),
                radius = pipR,
                center = androidx.compose.ui.geometry.Offset(size.width - inset - pipR, inset + pipR)
            )
        }
    }
}

