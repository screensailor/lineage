package io.github.screensailor.lineage.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.dashedBorder(
    color: Color,
    corner: Dp,
    strokeWidth: Dp = 1.dp,
    intervals: FloatArray = floatArrayOf(10f, 10f)
): Modifier = drawWithCache {
    val cornerRadiusPx = corner.toPx()
    val strokeWidthPx = strokeWidth.toPx()
    onDrawWithContent {
        drawContent()
        drawRoundRect(
            color = color,
            style = Stroke(
                width = strokeWidthPx,
                pathEffect = PathEffect.dashPathEffect(intervals)
            ),
            cornerRadius = CornerRadius(cornerRadiusPx)
        )
    }
}
