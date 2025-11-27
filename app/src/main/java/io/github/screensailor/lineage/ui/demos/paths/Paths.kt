package io.github.screensailor.lineage.ui.demos.paths

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.screensailor.lineage.ui.theme.CornflowerBlue
import io.github.screensailor.lineage.ui.theme.dashedBorder
import io.github.screensailor.lineage.ui.theme.rememberSpinProgress

private fun Color.contrastingText(): Color =
    if (this == Color.White) Color.Black else Color.White

@Composable
fun Paths() {
    Trail {
        val d = clicked.trek?.let { (trek - it).stride }
        val backgroundColor = if (d != null && d < 0) CornflowerBlue else Color.White

        Column(
            modifier = Modifier
                .background(backgroundColor)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Paths",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = backgroundColor.contrastingText()
            )

            val spin by rememberSpinProgress()

            DashingFrame {
                DashingFrame {
                    DashingFrame {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Matryoshka(gen = 6)
                            Matryoshka(gen = 6, spin = spin)
                        }
                    }
                }
            }

            Text(
                text = "Tap on the Matryoshka squares!",
                fontSize = 14.sp,
                color = backgroundColor.contrastingText()
            )
        }
    }
}

@Composable
private fun TrailScope.DashingFrame(
    modifier: Modifier = Modifier,
    padding: Dp = 24.dp,
    corner: Dp = 8.dp,
    content: @Composable TrailScope.() -> Unit
) {
    Trail {
        val d = clicked.trek?.let { (trek - it).stride }
        val backgroundColor = if (d != null && d < 0) CornflowerBlue else Color.White
        val borderColor = backgroundColor.contrastingText()

        Column(
            modifier = modifier
                .clickable { clicked.trek = null }
                .dashedBorder(borderColor, corner)
                .clip(RoundedCornerShape(corner))
                .background(backgroundColor)
                .fillMaxWidth()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            content()
        }
    }
}

@Composable
private fun TrailScope.Matryoshka(
    gen: Int,
    depth: Int = 0,
    corner: Dp = 8.dp,
    spin: Float = 0f
) {
    if (gen <= 0) return

    Trail {
        val d = clicked.trek?.let { (trek - it).stride }
        val backgroundColor = when {
            d == null -> Color.White
            d < 0 -> CornflowerBlue
            else -> Color.hsv((d * 18f).coerceIn(0f, 360f), 1f, 1f)
        }
        val borderColor = backgroundColor.contrastingText()

        Box(
            modifier = Modifier
                .clickable { clicked.trek = if (clicked.trek == trek) null else trek }
                .border(1.dp, borderColor, RoundedCornerShape(corner))
                .clip(RoundedCornerShape(corner))
                .background(backgroundColor)
                .graphicsLayer {
                    rotationZ = if (spin != 0f) spin * 180f else 0f
                }
                .fillMaxWidth()
                .padding((2 * gen + 4).dp)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Matryoshka(
                gen = gen - 1,
                depth = depth + 1,
                corner = corner,
                spin = spin
            )
        }
    }
}
