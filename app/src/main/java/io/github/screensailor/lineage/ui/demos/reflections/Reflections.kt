package io.github.screensailor.lineage.ui.demos.reflections

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

data class Properties(
    val backgroundColor: Color = Color.White,
    val foregroundColor: Color = Color.Black
)

fun My<Properties>.highlightLineage(hueStep: Float = 18f) {
    our.store.clear()

    val root = view.findSemanticsRoot() ?: return
    val trigger = root.findById(id) ?: return

    trigger.ancestors()
        .mapNotNull { it.myOwnId }
        .forEach { ancestorId ->
            our.store[ancestorId] = Properties(
                backgroundColor = CornflowerBlue,
                foregroundColor = CornflowerBlue.contrastingText()
            )
        }

    trigger.descendants(withMe = true)
        .mapNotNull { node -> node.myOwnId?.let { id -> node to id } }
        .forEach { (node, descendantId) ->
            val depth = node.depthRelativeTo(trigger) ?: return@forEach
            val hue = (depth * hueStep).coerceIn(0f, 360f)
            val color = Color.hsv(hue, 1f, 1f)
            our.store[descendantId] = Properties(
                backgroundColor = color,
                foregroundColor = color.contrastingText()
            )
        }
}

fun My<Properties>.clearHighlighting() {
    our.store.clear()
}

@Composable
fun Reflections() {
    OurOwn(Properties()) {
        val my = myOwn<Properties>()

        Column(
            modifier = my.modifier
                .background(my.own.backgroundColor)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Reflections",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = my.own.foregroundColor
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
                color = my.own.foregroundColor
            )
        }
    }
}

@Composable
private fun DashingFrame(
    modifier: Modifier = Modifier,
    padding: Dp = 24.dp,
    corner: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    val my = myOwn<Properties>()

    Column(
        modifier = my.modifier
            .then(modifier)
            .clickable { my.clearHighlighting() }
            .dashedBorder(my.own.foregroundColor, corner)
            .clip(RoundedCornerShape(corner))
            .background(my.own.backgroundColor)
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

    val my = myOwn<Properties>()

    Box(
        modifier = my.modifier
            .clickable { my.highlightLineage() }
            .border(1.dp, my.own.foregroundColor, RoundedCornerShape(corner))
            .clip(RoundedCornerShape(corner))
            .background(my.own.backgroundColor)
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
