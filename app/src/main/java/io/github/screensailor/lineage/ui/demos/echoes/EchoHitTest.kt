package io.github.screensailor.lineage.ui.demos.echoes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates

fun hitTestEchoNodes(
    nodes: List<EchoNode>,
    windowOffset: Offset,
    coordinatesCache: CoordinatesCache? = null
): EchoNode? {
    var deepestHit: EchoNode? = null
    var deepestDepth = -1

    for (node in nodes) {
        val layoutNode = node.reference.get() ?: continue
        val coords = getCoordinates(layoutNode, coordinatesCache) ?: continue
        if (!coords.isAttached) continue

        val local = coords.windowToLocal(windowOffset)
        val size = coords.size

        val inBounds = local.x >= 0 &&
                local.y >= 0 &&
                local.x <= size.width &&
                local.y <= size.height

        if (inBounds && node.depth > deepestDepth) {
            deepestHit = node
            deepestDepth = node.depth
        }
    }

    return deepestHit
}

private fun getCoordinates(
    layoutNode: Any,
    cache: CoordinatesCache?
): LayoutCoordinates? {
    cache?.get(layoutNode)?.let { return it }
    return getLookaheadCoordinator(layoutNode) as? LayoutCoordinates
}
