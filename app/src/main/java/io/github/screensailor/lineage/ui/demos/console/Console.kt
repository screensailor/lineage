package io.github.screensailor.lineage.ui.demos.console

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.screensailor.lineage.ui.demos.echoes.EchoActivity
import io.github.screensailor.lineage.ui.demos.echoes.EchoNode
import io.github.screensailor.lineage.ui.demos.echoes.TreeSnapshot

@Composable
fun Console() {
    val activity = LocalActivity.current as? EchoActivity ?: return

    DisposableEffect(Unit) {
        activity.treeSnapshotEnabled = false
        onDispose { activity.treeSnapshotEnabled = true }
    }

    val snapshot by activity.treeSnapshot.collectAsState()

    ConsoleContent(snapshot)
}

@Composable
private fun ConsoleContent(snapshot: TreeSnapshot) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text(
            text = "Echoes of the previous tab · ${snapshot.nodes.size} nodes",
            fontSize = 12.sp,
            color = Color.Gray,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = formatTree(snapshot.nodes),
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        )
    }
}

private fun formatTree(nodes: List<EchoNode>): String {
    if (nodes.isEmpty()) return ""
    val childCounts = nodes.associate { it.path.toList() to it.childCount }
    return nodes.joinToString("\n") { node -> formatNode(node, childCounts) }
}

private fun formatNode(node: EchoNode, childCounts: Map<List<Int>, Int>): String {
    if (node.path.size == 1) return node.description

    val prefix = StringBuilder()

    for (ancestorDepth in 1 until node.depth) {
        val ancestorParentPath = if (ancestorDepth == 1) emptyList() else node.path.take(ancestorDepth - 1).toList()
        val parentChildCount = childCounts[ancestorParentPath] ?: 1
        val ancestorIndex = node.path[ancestorDepth - 1]
        val isLastAtLevel = ancestorIndex == parentChildCount - 1
        prefix.append(if (isLastAtLevel) "  " else "│ ")
    }

    val parentPath = node.path.dropLast(1).toList()
    val parentChildCount = childCounts[parentPath] ?: 1
    val myIndex = node.path.last()
    val isLast = myIndex == parentChildCount - 1
    prefix.append(if (isLast) "└─" else "├─")

    return prefix.toString() + node.description
}
