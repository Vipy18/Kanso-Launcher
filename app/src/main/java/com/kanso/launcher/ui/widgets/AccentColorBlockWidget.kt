package com.kanso.launcher.ui.widgets

import android.graphics.CornerPathEffect
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kanso.launcher.data.model.AccentBlockStyle
import com.kanso.launcher.ui.theme.LocalKansoColors
import com.kanso.launcher.ui.theme.buildColorBlockPath

@Composable
fun AccentColorBlockWidget(
    style: AccentBlockStyle,
    flipped: Boolean = false,
    rotated: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = LocalKansoColors.current
    val accent = colors.accent

    Box(
        modifier = modifier
            .testTag("accent_block_${style.name.lowercase()}")
            .fillMaxSize()
            .graphicsLayer {
                scaleX = if (flipped) -1f else 1f
                rotationZ = if (rotated) 180f else 0f
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val path = buildAccentBlockPath(style = style, w = w, h = h)
            val androidPath = path.asAndroidPath()
            val roundCorners = style != AccentBlockStyle.S_CURVE_SHELF && style != AccentBlockStyle.PILL_BAR
            val cornerEffect = if (roundCorners) CornerPathEffect(6.dp.toPx()) else null

            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                val darkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.FILL
                    color = accent.toArgb()
                    pathEffect = cornerEffect
                    setShadowLayer(
                        11.dp.toPx(),
                        3.dp.toPx(),
                        5.dp.toPx(),
                        colors.shadowDark.copy(alpha = if (colors.isDark) 0.70f else 0.36f).toArgb()
                    )
                }
                native.drawPath(androidPath, darkPaint)

                val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.FILL
                    color = accent.toArgb()
                    pathEffect = cornerEffect
                    setShadowLayer(
                        6.dp.toPx(),
                        -2.dp.toPx(),
                        -2.dp.toPx(),
                        colors.shadowLight.copy(alpha = if (colors.isDark) 0.22f else 0.72f).toArgb()
                    )
                }
                native.drawPath(androidPath, lightPaint)
            }
        }
    }
}

fun buildAccentBlockPath(style: AccentBlockStyle, w: Float, h: Float): Path {
    val minD = minOf(w, h)
    return when (style) {
        AccentBlockStyle.S_CURVE_SHELF -> buildColorBlockPath(
            width = w,
            height = h,
            leftDropFraction = 0.98f,
            rightShelfFraction = 0.68f,
            curveCenterXFraction = 0.50f
        )

        AccentBlockStyle.CHAMFERED_PLATE -> Path().apply {
            val cut = (minD * 0.22f).coerceIn(10f, 44f)
            val sm = cut * 0.35f
            moveTo(cut, 0f)
            lineTo(w - sm, 0f)
            lineTo(w, sm)
            lineTo(w, h - cut)
            lineTo(w - cut, h)
            lineTo(sm, h)
            lineTo(0f, h - sm)
            lineTo(0f, cut)
            close()
        }

        AccentBlockStyle.DIAGONAL_SLAB -> Path().apply {
            val slant = (w * 0.16f).coerceIn(14f, 64f)
            moveTo(slant, 0f)
            lineTo(w, 0f)
            lineTo(w - slant, h)
            lineTo(0f, h)
            close()
        }

        AccentBlockStyle.CORNER_ARMOR -> Path().apply {
            val armW = (w * 0.36f).coerceAtLeast(24f)
            val armH = (h * 0.36f).coerceAtLeast(24f)
            val cut = (minD * 0.14f).coerceIn(8f, 28f)
            moveTo(cut, 0f)
            lineTo(w, 0f)
            lineTo(w, armH - cut * 0.6f)
            lineTo(w - cut * 0.6f, armH)
            lineTo(armW + cut * 0.7f, armH)
            lineTo(armW, armH + cut * 0.7f)
            lineTo(armW, h - cut * 0.6f)
            lineTo(armW - cut * 0.6f, h)
            lineTo(0f, h)
            lineTo(0f, cut)
            close()
        }

        AccentBlockStyle.STEPPED_MONOLITH -> Path().apply {
            val cut = (minD * 0.12f).coerceIn(6f, 22f)
            val stepX = w * 0.62f
            val stepY = h * 0.24f
            moveTo(cut, 0f)
            lineTo(stepX - cut, 0f)
            lineTo(stepX, stepY)
            lineTo(w - cut, stepY)
            lineTo(w, stepY + cut)
            lineTo(w, h - cut)
            lineTo(w - cut, h)
            lineTo(0f, h)
            lineTo(0f, cut)
            close()
        }

        AccentBlockStyle.SPLIT_CHEVRON -> Path().apply {
            val tip = (w * 0.18f).coerceIn(14f, 52f)
            moveTo(0f, 0f)
            lineTo(w - tip, 0f)
            lineTo(w, h * 0.5f)
            lineTo(w - tip, h)
            lineTo(0f, h)
            lineTo(tip, h * 0.5f)
            close()
        }

        AccentBlockStyle.PILL_BAR -> Path().apply {
            val r = (minD * 0.48f).coerceAtLeast(12f)
            moveTo(r, 0f)
            lineTo(w - r, 0f)
            cubicTo(w - r * 0.35f, 0f, w, r * 0.35f, w, r)
            lineTo(w, h - r)
            cubicTo(w, h - r * 0.35f, w - r * 0.35f, h, w - r, h)
            lineTo(r, h)
            cubicTo(r * 0.35f, h, 0f, h - r * 0.35f, 0f, h - r)
            lineTo(0f, r)
            cubicTo(0f, r * 0.35f, r * 0.35f, 0f, r, 0f)
            close()
        }
    }
}
