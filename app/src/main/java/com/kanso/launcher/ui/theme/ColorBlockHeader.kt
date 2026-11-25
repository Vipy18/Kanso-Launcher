package com.kanso.launcher.ui.theme

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Custom cubic Bézier S-curve top color-block matching the Kanso-Launcher Light/Dark mockups:
 * frames the left hero Dot-Matrix Clock + Date lower (~100% of header height on left) and
 * sweeps smoothly upward around the recessed Battery pill to hug the right dark Volume capsule (~72%).
 */
class ColorBlockHeaderShape(
    private val leftDropFraction: Float = 0.98f,
    private val rightShelfFraction: Float = 0.72f,
    private val curveCenterXFraction: Float = 0.50f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = buildColorBlockPath(
            width = size.width,
            height = size.height,
            leftDropFraction = leftDropFraction,
            rightShelfFraction = rightShelfFraction,
            curveCenterXFraction = curveCenterXFraction
        )
        return Outline.Generic(path)
    }
}

fun buildColorBlockPath(
    width: Float,
    height: Float,
    leftDropFraction: Float = 0.98f,
    rightShelfFraction: Float = 0.72f,
    curveCenterXFraction: Float = 0.50f
): Path {
    val yLeft = height * leftDropFraction
    val yRight = height * rightShelfFraction
    val leftCornerR = (width * 0.065f).coerceAtMost(height * 0.16f)
    val curveStartX = width * (curveCenterXFraction - 0.08f)
    val curveEndX = width * (curveCenterXFraction + 0.11f)
    val rightFlareX = width * 0.87f
    val rightEdgeY = (yRight - height * 0.08f).coerceAtLeast(height * 0.45f)

    return Path().apply {
        moveTo(0f, 0f)
        lineTo(width, 0f)
        lineTo(width, rightEdgeY)
        cubicTo(
            width * 0.96f, yRight,
            rightFlareX, yRight,
            curveEndX, yRight
        )
        // Smooth S-curve sweep from right shelf down to left clock shelf
        cubicTo(
            width * (curveCenterXFraction + 0.01f), yRight,
            width * (curveCenterXFraction + 0.01f), yLeft,
            curveStartX, yLeft
        )
        lineTo(leftCornerR, yLeft)
        // Rounded bottom-left chamfer
        cubicTo(
            leftCornerR * 0.35f, yLeft,
            0f, yLeft - leftCornerR * 0.35f,
            0f, yLeft - leftCornerR
        )
        close()
    }
}

@Composable
fun ColorBlockHeaderBackdrop(
    modifier: Modifier = Modifier,
    height: Dp = 290.dp,
    leftDropFraction: Float = 0.98f,
    rightShelfFraction: Float = 0.72f,
    curveCenterXFraction: Float = 0.50f,
    accentColor: Color = LocalKansoColors.current.accent
) {
    val colors = LocalKansoColors.current
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val path = buildColorBlockPath(
            width = size.width,
            height = size.height,
            leftDropFraction = leftDropFraction,
            rightShelfFraction = rightShelfFraction,
            curveCenterXFraction = curveCenterXFraction
        )
        drawIntoCanvas { canvas ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = accentColor.toArgb()
                setShadowLayer(
                    12.dp.toPx(),
                    0f,
                    5.dp.toPx(),
                    colors.shadowDark.copy(alpha = if (colors.isDark) 0.75f else 0.45f).toArgb()
                )
            }
            canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
        }
    }
}
