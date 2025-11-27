package io.github.screensailor.lineage.ui.demos.echoes

import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.semantics.getOrNull

class SemanticsCache {

    private var cachedNodes: Map<LayoutInfo, SemanticsNode> = emptyMap()
    private var dirty = true

    fun invalidate() {
        dirty = true
    }

    fun refresh(rootForTest: RootForTest) {
        if (!dirty) return
        dirty = false

        cachedNodes = rootForTest.semanticsOwner
            .getAllSemanticsNodes(mergingEnabled = false)
            .associateBy { it.layoutInfo }
    }

    fun getTextFor(node: Any): String? {
        val layoutInfo = node as? LayoutInfo ?: return null
        val semanticsNode = cachedNodes[layoutInfo] ?: return null
        val textList = semanticsNode.config.getOrNull(SemanticsProperties.Text)
        val text = textList?.firstOrNull()?.text ?: return null
        return if (text.length > 20) text.take(17) + "..." else text
    }

    fun getEditableTextFor(node: Any): String? {
        val layoutInfo = node as? LayoutInfo ?: return null
        val semanticsNode = cachedNodes[layoutInfo] ?: return null
        val text = semanticsNode.config.getOrNull(SemanticsProperties.EditableText)?.text
            ?: return null
        return if (text.length > 20) text.take(17) + "..." else text
    }
}
