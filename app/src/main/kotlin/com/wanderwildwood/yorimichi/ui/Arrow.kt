package com.wanderwildwood.yorimichi.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The needle.
 *
 * Solid black, filled rather than outlined, and large: it is the one thing on the
 * screen that is looked at while walking, often at arm's length in the sun. A hollow
 * arrowhead at this size on sixteen greys reads as a smudge.
 *
 * With no compass to ask, the ring is drawn as a map is — north up — and the needle
 * points at the bearing from north. That is still a usable instruction, and it is
 * honest about what the phone can and cannot tell.
 */
@Composable
fun Arrow(
    bearingDegrees: Double,
    headingDegrees: Float?,
    modifier: Modifier = Modifier,
) {
    val northAt = -(headingDegrees ?: 0f).toDouble()
    val needleAt = bearingDegrees - (headingDegrees ?: 0f).toDouble()

    Canvas(modifier = modifier) {
        val radius = min(size.width, size.height) / 2f - 6f
        val middle = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            color = Color.Black,
            radius = radius,
            center = middle,
            style = Stroke(width = 3f),
        )

        // Where north has got to. Two strokes rather than a letter: a glyph small enough
        // to sit on the rim is a smudge on this panel, and the ring only has to say
        // which way it is turned.
        val tickBase = point(middle, radius * 0.80f, northAt)
        val tickTip = point(middle, radius, northAt)
        drawLine(Color.Black, tickBase, tickTip, strokeWidth = 7f)

        drawPath(needle(middle, radius, needleAt), Color.Black)
    }
}

/** The point [distance] from [middle] at [degrees] clockwise from straight up. */
private fun point(middle: Offset, distance: Float, degrees: Double): Offset {
    val radians = degrees * PI / 180.0
    return Offset(
        x = middle.x + distance * sin(radians).toFloat(),
        y = middle.y - distance * cos(radians).toFloat(),
    )
}

/**
 * A kite: a long point at the far end, shoulders behind the middle, and a shallow notch
 * between them. The notch is what makes it read as an arrow rather than a triangle at a
 * glance.
 *
 * Laid out along and across the needle rather than as angles from the centre, because
 * the thing being tuned is how slim it is, and a half-width in [across] is a number that
 * says that. Four and a half times as long as it is wide.
 */
private fun needle(middle: Offset, radius: Float, degrees: Double): Path = Path().apply {
    fun at(along: Float, across: Float) = vertex(middle, degrees, radius * along, radius * across)

    val tip = at(0.80f, 0f)
    val left = at(-0.16f, -0.105f)
    val tail = at(-0.05f, 0f)
    val right = at(-0.16f, 0.105f)

    moveTo(tip.x, tip.y)
    lineTo(left.x, left.y)
    lineTo(tail.x, tail.y)
    lineTo(right.x, right.y)
    close()
}

/**
 * A point [along] the needle and [across] it, in pixels. Across is measured a quarter
 * turn clockwise from the way the needle points, so a positive value is its right hand.
 */
private fun vertex(middle: Offset, degrees: Double, along: Float, across: Float): Offset {
    val radians = degrees * PI / 180.0
    val forward = sin(radians).toFloat() to -cos(radians).toFloat()
    val sideways = cos(radians).toFloat() to sin(radians).toFloat()
    return Offset(
        x = middle.x + along * forward.first + across * sideways.first,
        y = middle.y + along * forward.second + across * sideways.second,
    )
}
