package io.github.screensailor.lineage.ui.demos.paths

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

class Clicked internal constructor(private val state: MutableState<Trek?>) {
    var trek: Trek?
        get() = state.value
        set(value) { state.value = value }
}

interface TrailScope {
    val trek: Trek
    val clicked: Clicked
}

internal class ChildIndexAllocator {
    private val indices = mutableMapOf<Long, Int>()

    fun indexFor(compositeKey: Long): Int =
        indices.getOrPut(compositeKey) { indices.size }
}

private val LocalTrailScope = staticCompositionLocalOf<TrailScopeImpl?> { null }

private class TrailScopeImpl(
    override val trek: Trek,
    override val clicked: Clicked,
    val allocator: ChildIndexAllocator
) : TrailScope

@Composable
fun Trail(content: @Composable TrailScope.() -> Unit) {
    val rootAllocator = remember { ChildIndexAllocator() }
    val clickedState = remember { mutableStateOf<Trek?>(null) }
    val clicked = remember { Clicked(clickedState) }
    val scope = remember { TrailScopeImpl(Trek.Root, clicked, rootAllocator) }

    CompositionLocalProvider(LocalTrailScope provides scope) {
        scope.content()
    }
}

@Composable
fun TrailScope.Trail(id: Int? = null, content: @Composable TrailScope.() -> Unit) {
    val parent = LocalTrailScope.current
        ?: error("TrailScope.Trail must be called inside Trail { }")

    val compositeKey = currentCompositeKeyHashCode
    val autoIndex = remember(parent.allocator) {
        parent.allocator.indexFor(compositeKey)
    }
    val segment = id ?: autoIndex

    val childTrek = remember(parent.trek, segment) {
        parent.trek + Trek.descendant(segment)
    }

    val childAllocator = remember { ChildIndexAllocator() }
    val childScope = remember(childTrek) {
        TrailScopeImpl(childTrek, parent.clicked, childAllocator)
    }

    CompositionLocalProvider(LocalTrailScope provides childScope) {
        childScope.content()
    }
}
