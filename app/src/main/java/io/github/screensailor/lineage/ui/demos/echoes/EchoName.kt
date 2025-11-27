package io.github.screensailor.lineage.ui.demos.echoes

import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density

data class EchoName(val value: String)

fun Modifier.echo(name: String): Modifier = this then EchoNameElement(name)

private data class EchoNameElement(private val name: String) : ModifierNodeElement<EchoNameNode>() {
    override fun create() = EchoNameNode(name)
    override fun update(node: EchoNameNode) {
        node.name = name
    }
    override fun InspectorInfo.inspectableProperties() {
        this.name = "echo"
        value = this@EchoNameElement.name
    }
}

private class EchoNameNode(var name: String) : Modifier.Node(), ParentDataModifierNode {
    override fun Density.modifyParentData(parentData: Any?): Any {
        return when (parentData) {
            null -> EchoName(name)
            is EchoName -> EchoName(name)
            else -> parentData
        }
    }
}
