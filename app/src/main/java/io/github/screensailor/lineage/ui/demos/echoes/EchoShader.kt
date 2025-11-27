package io.github.screensailor.lineage.ui.demos.echoes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color

@Suppress("FunctionName")
@Composable
fun EchoShader(block: (my: EchoNode, event: EchoEvent) -> EchoIntent) {
    DisposableEffect(Unit) {
        val shader = object : EchoShader {
            override fun invoke(my: EchoNode, event: EchoEvent) = block(my, event)
        }
        EchoShaders += shader
        onDispose { EchoShaders -= shader }
    }
}

data class EchoEvent(
    val target: EchoNode,
    val timestampNanos: Long = System.nanoTime()
)

sealed class EchoIntent {
    data class Highlight(val color: Color) : EchoIntent()
    data object Clear : EchoIntent()
}

fun interface EchoShader {
    operator fun invoke(my: EchoNode, event: EchoEvent): EchoIntent
}

val EchoNode.pathString: String
    get() = path.joinToString(",", "[", "]")

fun EchoNode.depthRelativeTo(other: EchoNode): Int? = depthRelativeTo(other.path)

fun EchoNode.depthRelativeTo(otherPath: IntArray): Int? = when {
    path contentEquals otherPath -> 0
    path.isPrefixOf(otherPath) -> -(otherPath.size - path.size)
    otherPath.isPrefixOf(path) -> path.size - otherPath.size
    else -> null
}

private fun IntArray.isPrefixOf(other: IntArray): Boolean {
    if (size > other.size) return false
    for (i in indices) if (this[i] != other[i]) return false
    return true
}
