package io.github.screensailor.lineage.ui.demos.echoes

import java.lang.ref.WeakReference
import java.util.WeakHashMap

class ShadowNode(
    val layoutNode: WeakReference<Any>,
    var parent: ShadowNode?
) {
    val children: MutableList<ShadowNode> = mutableListOf()

    val isAlive: Boolean get() = layoutNode.get() != null

    fun pathFromRoot(): IntArray {
        val indices = mutableListOf<Int>()
        var current: ShadowNode? = this
        var par: ShadowNode? = current?.parent

        while (par != null) {
            val idx = par.children.indexOf(current)
            if (idx >= 0) indices.add(0, idx)
            current = par
            par = current.parent
        }

        return intArrayOf(0) + indices.toIntArray()
    }
}

class ShadowTree {
    private var root: ShadowNode? = null
    private val nodeMap = WeakHashMap<Any, ShadowNode>()
    private val parentStack = ArrayDeque<ShadowNode>()
    private var currentNode: ShadowNode? = null

    fun setRoot(layoutNode: Any) {
        val node = ShadowNode(WeakReference(layoutNode), null)
        root = node
        currentNode = node
        nodeMap[layoutNode] = node
        parentStack.clear()
    }

    fun down(layoutNode: Any) {
        val node = nodeMap[layoutNode] ?: return
        currentNode?.let { parentStack.addLast(it) }
        currentNode = node
    }

    fun up() {
        if (parentStack.isNotEmpty()) {
            currentNode = parentStack.removeLast()
        }
    }

    fun insert(index: Int, layoutNode: Any) {
        val parent = currentNode ?: return
        val node = ShadowNode(WeakReference(layoutNode), parent)
        nodeMap[layoutNode] = node

        if (index >= parent.children.size) {
            parent.children.add(node)
        } else {
            parent.children.add(index, node)
        }
    }

    fun remove(index: Int, count: Int) {
        val parent = currentNode ?: return
        if (index < 0 || index >= parent.children.size) return

        val endIndex = minOf(index + count, parent.children.size)
        for (i in (index until endIndex).reversed()) {
            val child = parent.children.removeAt(i)
            child.layoutNode.get()?.let { nodeMap.remove(it) }
        }
    }

    fun move(from: Int, to: Int, count: Int) {
        val parent = currentNode ?: return
        if (from < 0 || from >= parent.children.size) return

        val dest = if (from > to) to else to - count
        val toMove = mutableListOf<ShadowNode>()

        for (i in (from until minOf(from + count, parent.children.size)).reversed()) {
            toMove.add(0, parent.children.removeAt(i))
        }

        for ((i, node) in toMove.withIndex()) {
            val insertAt = minOf(dest + i, parent.children.size)
            parent.children.add(insertAt, node)
        }
    }

    fun clear() {
        root?.children?.clear()
        nodeMap.entries.removeIf { it.key !== root?.layoutNode?.get() }
        parentStack.clear()
        currentNode = root
    }

    fun toEchoNodes(descriptionProvider: (Any) -> String): List<EchoNode> {
        val rootNode = root ?: return emptyList()
        return collectNodes(rootNode, descriptionProvider)
    }

    private fun collectNodes(
        node: ShadowNode,
        descriptionProvider: (Any) -> String
    ): List<EchoNode> {
        val result = mutableListOf<EchoNode>()
        val layoutNode = node.layoutNode.get() ?: return result

        val path = node.pathFromRoot()
        val description = descriptionProvider(layoutNode)
        result.add(EchoNode.create(path, description, node.children.size, layoutNode))

        for (child in node.children) {
            result.addAll(collectNodes(child, descriptionProvider))
        }

        return result
    }
}
