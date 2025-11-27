package io.github.screensailor.lineage.ui.demos.echoes

import java.lang.ref.WeakReference

data class EchoNode(
    val path: IntArray,
    val description: String,
    val childCount: Int,
    val reference: WeakReference<Any>
) {
    val depth: Int get() = path.size
    val isAlive: Boolean get() = reference.get() != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EchoNode) return false
        return path.contentEquals(other.path)
    }

    override fun hashCode(): Int = path.contentHashCode()

    override fun toString(): String {
        val pathStr = path.joinToString(",", "[", "]")
        val suffix = if (isAlive) "" else "?"
        return "$description$suffix $pathStr"
    }

    companion object {
        fun create(
            path: IntArray,
            description: String,
            childCount: Int,
            node: Any
        ): EchoNode = EchoNode(path, description, childCount, WeakReference(node))
    }
}
