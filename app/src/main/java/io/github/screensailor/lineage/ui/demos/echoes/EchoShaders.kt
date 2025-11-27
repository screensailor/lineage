package io.github.screensailor.lineage.ui.demos.echoes

object EchoShaders {
    private val shaders = mutableListOf<EchoShader>()

    operator fun plusAssign(shader: EchoShader) {
        shaders += shader
    }

    operator fun minusAssign(shader: EchoShader) {
        shaders -= shader
    }

    fun dispatch(event: EchoEvent, nodes: List<EchoNode>, registry: EchoRegistry) {
        for (node in nodes) {
            val layoutNode = node.reference.get() ?: continue
            var intent: EchoIntent = EchoIntent.Clear
            for (shader in shaders) {
                val shaderIntent = shader(node, event)
                if (shaderIntent is EchoIntent.Highlight) {
                    intent = shaderIntent
                    break
                }
            }
            when (intent) {
                is EchoIntent.Highlight -> registry.setHighlight(layoutNode, intent.color)
                EchoIntent.Clear -> registry.clearIntent(layoutNode)
            }
        }
    }

    fun clear(nodes: List<EchoNode>, registry: EchoRegistry) {
        for (node in nodes) {
            val layoutNode = node.reference.get() ?: continue
            registry.clearIntent(layoutNode)
        }
    }
}
