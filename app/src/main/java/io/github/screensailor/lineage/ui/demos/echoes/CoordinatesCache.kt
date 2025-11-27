package io.github.screensailor.lineage.ui.demos.echoes

import androidx.compose.ui.layout.LayoutCoordinates
import java.util.WeakHashMap

class CoordinatesCache {
    private val cache = WeakHashMap<Any, LayoutCoordinates>()

    fun store(layoutNode: Any, coordinates: LayoutCoordinates) {
        cache[layoutNode] = coordinates
    }

    fun get(layoutNode: Any): LayoutCoordinates? = cache[layoutNode]
}
