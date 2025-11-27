package io.github.screensailor.lineage.ui.demos.echoes

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext

@Composable
fun EchoDragEvents(content: @Composable () -> Unit) {
    val activity = LocalContext.current as? EchoActivity
    val registry = activity?.echoRegistry
    var layoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var lastHitPathString by remember { mutableStateOf<String?>(null) }
    var lastNodes by remember { mutableStateOf<List<EchoNode>>(emptyList()) }

    fun highlightAt(localOffset: Offset): String? {
        val activity = activity ?: return null
        val registry = registry ?: return null
        val coords = layoutCoordinates ?: return null

        val windowOffset = coords.positionInWindow() + localOffset
        val nodes = if (activity.useShadowTree) {
            activity.shadowTree.toEchoNodes { extractNodeDescription(it) }
        } else {
            val composeView = activity.findAndroidComposeView() ?: return null
            val root = composeView.getLayoutRoot()
            walkLayoutTree(root)
        }

        val coordsCache = if (activity.useCoordinatesCache) activity.coordinatesCache else null
        val hit = hitTestEchoNodes(nodes, windowOffset, coordsCache)
        val hitPath = hit?.pathString

        if (hitPath == lastHitPathString) {
            return hitPath
        }

        if (hit != null) {
            val event = EchoEvent(hit)
            EchoShaders.dispatch(event, nodes, registry)
            lastNodes = nodes
        } else {
            EchoShaders.clear(lastNodes, registry)
            lastNodes = emptyList()
        }

        lastHitPathString = hitPath
        return hitPath
    }

    fun clearHighlights() {
        if (registry != null) {
            EchoShaders.clear(lastNodes, registry)
        }
        lastNodes = emptyList()
        lastHitPathString = null
    }

    Box(
        modifier = Modifier
            .echo("EchoDragEvents")
            .fillMaxSize()
            .onGloballyPositioned { layoutCoordinates = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    highlightAt(down.position)
                    val pointerId = down.id

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (change.changedToUp()) break
                        if (change.positionChanged()) {
                            highlightAt(change.position)
                        }
                    }

                    clearHighlights()
                }
            }
    ) {
        content()
    }
}
