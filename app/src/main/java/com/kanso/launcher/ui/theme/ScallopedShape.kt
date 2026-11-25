package com.kanso.launcher.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Smooth 8-lobe radial scalloped medallion shape matching the Weather Badge in Image 1.
 */
class ScallopedShape(
    private val lobes: Int = 8,
    private val waveAmplitudeFraction: Float = 0.068f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = minOf(cx, cy)
        val baseR = maxR * (1f - waveAmplitudeFraction)
        val amp = maxR * waveAmplitudeFraction
        val steps = 160

        val path = Path().apply {
            for (i in 0..steps) {
                val theta = (2.0 * PI * i) / steps
                val r = baseR + amp * cos(lobes * theta).toFloat()
                val x = cx + r * cos(theta).toFloat()
                val y = cy + r * sin(theta).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        return Outline.Generic(path)
    }
}
