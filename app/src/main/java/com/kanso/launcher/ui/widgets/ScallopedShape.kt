package com.kanso.launcher.ui.widgets

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
 * Multi-lobed tactile scalloped medallion silhouette (12 smooth cusps by default)
 * matching the classic Kanso / Tactile Cyber-Mecha HUD weather badge.
 */
class ScallopedShape(
    private val lobes: Int = 12,
    private val depthRatio: Float = 0.068f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = minOf(size.width, size.height) / 2f
        val baseR = maxR * (1f - depthRatio)
        val amp = maxR * depthRatio
        val steps = 180

        val path = Path()
        for (i in 0..steps) {
            val theta = (2.0 * PI * i) / steps
            val r = baseR + amp * cos(lobes * theta).toFloat()
            val x = cx + r * cos(theta).toFloat()
            val y = cy + r * sin(theta).toFloat()
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        return Outline.Generic(path)
    }
}
