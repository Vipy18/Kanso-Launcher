package com.kanso.launcher.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Pure-Kotlin 5x7 dot-matrix pixel bitmaps for digits 0-9.
 * Zero external font assets required; draws crisp uniform square pixels.
 */
object DotMatrixFont {
    private val DIGITS: Map<Char, Array<String>> = mapOf(
        '0' to arrayOf(
            ".###.",
            "#...#",
            "#...#",
            "#...#",
            "#...#",
            "#...#",
            ".###."
        ),
        '1' to arrayOf(
            "..#..",
            ".##..",
            "..#..",
            "..#..",
            "..#..",
            "..#..",
            ".###."
        ),
        '2' to arrayOf(
            ".###.",
            "#...#",
            "....#",
            "..##.",
            ".#...",
            "#....",
            "#####"
        ),
        '3' to arrayOf(
            ".###.",
            "#...#",
            "....#",
            "..##.",
            "....#",
            "#...#",
            ".###."
        ),
        '4' to arrayOf(
            "...#.",
            "..##.",
            ".#.#.",
            "#..#.",
            "#####",
            "...#.",
            "...#."
        ),
        '5' to arrayOf(
            "#####",
            "#....",
            "####.",
            "....#",
            "....#",
            "#...#",
            ".###."
        ),
        '6' to arrayOf(
            ".###.",
            "#....",
            "####.",
            "#...#",
            "#...#",
            "#...#",
            ".###."
        ),
        '7' to arrayOf(
            "#####",
            "....#",
            "...#.",
            "..#..",
            ".#...",
            ".#...",
            ".#..."
        ),
        '8' to arrayOf(
            ".###.",
            "#...#",
            "#...#",
            ".###.",
            "#...#",
            "#...#",
            ".###."
        ),
        '9' to arrayOf(
            ".###.",
            "#...#",
            "#...#",
            ".####",
            "....#",
            "#...#",
            ".###."
        )
    )

    // 3x3 5-dot diamond/cross '+' glyph used as the twin divider between HH and MM
    private val CROSS: Array<String> = arrayOf(
        ".#.",
        "###",
        ".#."
    )

    fun bitmapFor(ch: Char): Array<String> = DIGITS[ch] ?: DIGITS.getValue('0')
    fun crossBitmap(): Array<String> = CROSS
}

/**
 * Stacked Dot-Matrix Hero Clock matching Image 1 & Image 3:
 * Draws HH stacked over MM with two centered 5-dot '+' diamond/cross markers in between.
 */
@Composable
fun DotMatrixClock(
    hours: String,
    minutes: String,
    modifier: Modifier = Modifier,
    pixelSize: Dp = 5.5.dp,
    pixelGap: Dp = 2.2.dp,
    digitColor: Color = LocalKansoColors.current.onAccent,
    dividerColor: Color = digitColor
) {
    val hSafe = hours.padStart(2, '0').takeLast(2)
    val mSafe = minutes.padStart(2, '0').takeLast(2)

    val pitch = pixelSize + pixelGap
    val digitCols = 5
    val digitRows = 7
    val interDigitCols = 2
    val totalCols = digitCols * 2 + interDigitCols // 12 columns
    val dividerRows = 3
    val rowGapUnits = 1.6f
    val totalHeightUnits = digitRows * 2 + dividerRows + rowGapUnits * 2

    val totalWidth = pitch * totalCols - pixelGap
    val totalHeight = pitch * totalHeightUnits

    Canvas(modifier = modifier.size(width = totalWidth, height = totalHeight)) {
        val pxSize = pixelSize.toPx()
        val pxPitch = pitch.toPx()

        fun drawGlyph(grid: Array<String>, originX: Float, originY: Float, color: Color) {
            for (r in grid.indices) {
                val row = grid[r]
                for (c in row.indices) {
                    if (row[c] == '#') {
                        drawRect(
                            color = color,
                            topLeft = Offset(originX + c * pxPitch, originY + r * pxPitch),
                            size = Size(pxSize, pxSize)
                        )
                    }
                }
            }
        }

        // Top row: HH
        drawGlyph(DotMatrixFont.bitmapFor(hSafe[0]), 0f, 0f, digitColor)
        drawGlyph(
            DotMatrixFont.bitmapFor(hSafe[1]),
            (digitCols + interDigitCols) * pxPitch,
            0f,
            digitColor
        )

        // Middle divider: two centered 5-dot '+' glyphs
        val dividerY = (digitRows + rowGapUnits * 0.55f) * pxPitch
        val crossSize = pxSize * 0.78f
        val crossPitch = pxPitch * 0.80f
        val leftCrossX = 3.2f * pxPitch
        val rightCrossX = 6.5f * pxPitch
        val cross = DotMatrixFont.crossBitmap()
        for (originX in listOf(leftCrossX, rightCrossX)) {
            for (r in cross.indices) {
                for (c in cross[r].indices) {
                    if (cross[r][c] == '#') {
                        drawRect(
                            color = dividerColor,
                            topLeft = Offset(originX + c * crossPitch, dividerY + r * crossPitch),
                            size = Size(crossSize, crossSize)
                        )
                    }
                }
            }
        }

        // Bottom row: MM
        val minutesY = (digitRows + dividerRows + rowGapUnits * 1.15f) * pxPitch
        drawGlyph(DotMatrixFont.bitmapFor(mSafe[0]), 0f, minutesY, digitColor)
        drawGlyph(
            DotMatrixFont.bitmapFor(mSafe[1]),
            (digitCols + interDigitCols) * pxPitch,
            minutesY,
            digitColor
        )
    }
}
