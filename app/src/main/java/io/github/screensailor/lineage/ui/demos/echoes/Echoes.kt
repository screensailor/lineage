package io.github.screensailor.lineage.ui.demos.echoes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

@Composable
fun Echoes() {

    EchoShader { my, event ->
        val d = my.depthRelativeTo(event.target)
        when {
            d == null -> EchoIntent.Clear
            d < 0 -> EchoIntent.Highlight(CornflowerBlue)
            else -> EchoIntent.Highlight(Color.hsv((d * 18f).coerceIn(0f, 360f), 1f, 1f))
        }
    }

    EchoDragEvents { Untouched() }
}

@Composable
private fun Untouched() {

    Column(
        modifier = Modifier
            .background(Color.White)
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Echoes",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
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
            text = "Slide through the hierarchy",
            fontSize = 14.sp,
            color = Color.Black
        )
    }
}

@Composable
private fun DashingFrame(
    modifier: Modifier = Modifier,
    padding: Dp = 24.dp,
    corner: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .dashedBorder(Color.Black, corner)
            .clip(RoundedCornerShape(corner))
            .background(Color.White)
            .fillMaxWidth()
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        content()
    }
}

@Composable
private fun Matryoshka(
    gen: Int,
    depth: Int = 0,
    corner: Dp = 8.dp,
    spin: Float = 0f
) {
    if (gen <= 0) return
    Box(
        modifier = Modifier
            .border(1.dp, Color.Black, RoundedCornerShape(corner))
            .clip(RoundedCornerShape(corner))
            .background(Color.White)
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
