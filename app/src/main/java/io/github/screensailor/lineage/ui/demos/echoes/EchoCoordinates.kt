package io.github.screensailor.lineage.ui.demos.echoes

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import java.lang.ref.WeakReference

fun Modifier.echoCoordinates(
    layoutNode: Any,
    cache: CoordinatesCache
): Modifier = this.then(EchoCoordinatesElement(layoutNode, cache))

private data class EchoCoordinatesElement(
    private val layoutNode: Any,
    private val cache: CoordinatesCache
) : ModifierNodeElement<EchoCoordinatesNode>() {

    override fun create(): EchoCoordinatesNode =
        EchoCoordinatesNode(WeakReference(layoutNode), cache)

    override fun update(node: EchoCoordinatesNode) {
        node.layoutNodeRef = WeakReference(layoutNode)
        node.cache = cache
    }
}

private class EchoCoordinatesNode(
    var layoutNodeRef: WeakReference<Any>,
    var cache: CoordinatesCache
) : Modifier.Node(), LayoutAwareModifierNode {

    override fun onPlaced(coordinates: LayoutCoordinates) {
        layoutNodeRef.get()?.let { node ->
            cache.store(node, coordinates)
        }
    }
}
