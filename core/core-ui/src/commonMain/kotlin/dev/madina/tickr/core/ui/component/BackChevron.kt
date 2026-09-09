package dev.madina.tickr.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A back arrow drawn rather than typed.
 *
 * The obvious version is the character `←`, and it works on Android and iOS because the system font
 * has the glyph. On web it does not: Skia renders with a bundled font that has no arrows, so the
 * button showed a tofu box. Drawing it removes the dependency on what any platform's font happens
 * to contain, which is a recurring trap for symbols in Compose Multiplatform.
 */
@Composable
fun BackChevron(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = DefaultSize,
) {
    Canvas(modifier = modifier.size(size)) {
        val width = this.size.width
        val height = this.size.height
        val stroke = StrokeWidth.toPx()
        val midY = height / 2f
        val left = width * LeftInset
        val right = width * RightInset

        drawLine(
            color = color,
            start = Offset(left, midY),
            end = Offset(right, midY),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(left, midY),
            end = Offset(left + width * HeadLength, midY - height * HeadSpread),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(left, midY),
            end = Offset(left + width * HeadLength, midY + height * HeadSpread),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

private val DefaultSize = 18.dp
private val StrokeWidth = 2.dp
private const val LeftInset = 0.15f
private const val RightInset = 0.85f
private const val HeadLength = 0.28f
private const val HeadSpread = 0.22f
