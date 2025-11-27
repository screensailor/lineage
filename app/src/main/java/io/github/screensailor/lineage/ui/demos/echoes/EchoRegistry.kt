package io.github.screensailor.lineage.ui.demos.echoes

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

interface EchoRegistry {
    fun setHighlight(node: Any, color: Color)
    fun clearIntent(node: Any)
    fun clearAll()
    fun applyTransform(node: Any, incoming: Modifier): Modifier
    fun hasIntent(node: Any): Boolean
}

class EchoRegistryImpl : EchoRegistry {

    private val intents = mutableMapOf<Any, EchoHighlightStyle>()

    override fun setHighlight(node: Any, color: Color) {
        val style = EchoHighlightStyle.Fill(color)
        intents[node] = style
        EchoHighlightInjection.injectHighlightDirect(node, style)
    }

    override fun clearIntent(node: Any) {
        intents.remove(node)
        try {
            EchoHighlightInjection.removeHighlight(node)
        } catch (_: Exception) {}
    }

    override fun clearAll() {
        for (node in intents.keys.toList()) {
            try {
                EchoHighlightInjection.removeHighlight(node)
            } catch (_: Exception) {}
        }
        intents.clear()
    }

    override fun applyTransform(node: Any, incoming: Modifier): Modifier {
        val style = intents[node] ?: return incoming
        return insertHighlightIntoModifier(incoming, style)
    }

    override fun hasIntent(node: Any): Boolean = node in intents
}
