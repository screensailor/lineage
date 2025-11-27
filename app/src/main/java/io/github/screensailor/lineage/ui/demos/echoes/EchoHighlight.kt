package io.github.screensailor.lineage.ui.demos.echoes

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

sealed class EchoHighlightStyle {
    data class Fill(val color: Color) : EchoHighlightStyle()
    data class FillWithDashedBorder(
        val fillColor: Color,
        val borderColor: Color,
        val strokeWidth: Float = 3f,
        val dashIntervals: FloatArray = floatArrayOf(10f, 10f)
    ) : EchoHighlightStyle() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is FillWithDashedBorder) return false
            return fillColor == other.fillColor &&
                    borderColor == other.borderColor &&
                    strokeWidth == other.strokeWidth &&
                    dashIntervals.contentEquals(other.dashIntervals)
        }
        override fun hashCode(): Int {
            var result = fillColor.hashCode()
            result = 31 * result + borderColor.hashCode()
            result = 31 * result + strokeWidth.hashCode()
            result = 31 * result + dashIntervals.contentHashCode()
            return result
        }
    }
}

data class EchoHighlightElement(
    val style: EchoHighlightStyle,
    val shape: Shape?
) : ModifierNodeElement<EchoHighlightNode>() {

    override fun create(): EchoHighlightNode = EchoHighlightNode(style, shape)

    override fun update(node: EchoHighlightNode) {
        node.style = style
        node.shape = shape
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "echoHighlight"
        properties["style"] = style
        properties["shape"] = shape
    }
}

class EchoHighlightNode(
    var style: EchoHighlightStyle,
    var shape: Shape?
) : Modifier.Node(), DrawModifierNode {

    override fun ContentDrawScope.draw() {
        val s = shape
        when (val st = style) {
            is EchoHighlightStyle.Fill -> {
                if (s != null) {
                    val outline = s.createOutline(size, layoutDirection, this)
                    val path = Path().apply { addOutline(outline) }
                    clipPath(path) {
                        drawRect(st.color, size = size)
                    }
                } else {
                    drawRect(st.color, size = size)
                }
            }
            is EchoHighlightStyle.FillWithDashedBorder -> {
                if (s != null) {
                    val outline = s.createOutline(size, layoutDirection, this)
                    val path = Path().apply { addOutline(outline) }
                    clipPath(path) {
                        drawRect(st.fillColor, size = size)
                    }
                    drawPath(
                        path = path,
                        color = st.borderColor,
                        style = Stroke(
                            width = st.strokeWidth,
                            pathEffect = PathEffect.dashPathEffect(st.dashIntervals)
                        )
                    )
                } else {
                    drawRect(st.fillColor, size = size)
                    drawRect(
                        color = st.borderColor,
                        style = Stroke(
                            width = st.strokeWidth,
                            pathEffect = PathEffect.dashPathEffect(st.dashIntervals)
                        )
                    )
                }
            }
        }
        drawContent()
    }
}

fun insertHighlightIntoModifier(incoming: Modifier, style: EchoHighlightStyle): Modifier {
    val elements = incoming.foldIn(mutableListOf<Modifier.Element>()) { acc, element ->
        acc.apply { add(element) }
    }

    val clipInfo = findClipInfoInElements(elements)
    val lastBackgroundIndex = findLastBackgroundIndexInElements(elements)
    val transformIndex = findTransformIndexInElements(elements)

    val insertIndex = when {
        transformIndex != null -> transformIndex
        lastBackgroundIndex != null -> lastBackgroundIndex + 1
        clipInfo != null -> clipInfo.index + 1
        else -> 0
    }

    val highlight = EchoHighlightElement(style, clipInfo?.shape)
    val injected = elements.toMutableList().apply { add(insertIndex, highlight) }

    return injected.foldRight(Modifier as Modifier) { element, acc -> element.then(acc) }
}

private data class ClipInfoResult(val index: Int, val shape: Shape?)

private fun findClipInfoInElements(elements: List<Modifier.Element>): ClipInfoResult? {
    for ((index, element) in elements.withIndex()) {
        val clazz = element::class.java
        if (!clazz.simpleName.contains("GraphicsLayer")) continue

        try {
            val clipField = clazz.getDeclaredField("clip").apply { isAccessible = true }
            val isClip = clipField.get(element) as? Boolean ?: false
            if (isClip) {
                val shapeField = clazz.getDeclaredField("shape").apply { isAccessible = true }
                val shape = shapeField.get(element) as? Shape
                return ClipInfoResult(index, shape)
            }
        } catch (_: Exception) {
        }
    }
    return null
}

private fun findLastBackgroundIndexInElements(elements: List<Modifier.Element>): Int? {
    var lastIndex: Int? = null
    for ((index, element) in elements.withIndex()) {
        val className = element::class.java.simpleName
        if (className.contains("Background")) {
            lastIndex = index
        }
    }
    return lastIndex
}

private fun findTransformIndexInElements(elements: List<Modifier.Element>): Int? {
    for ((index, element) in elements.withIndex()) {
        val clazz = element::class.java
        val simpleName = clazz.simpleName

        if (simpleName.contains("BlockGraphicsLayer")) {
            return index
        }

        if (!simpleName.contains("GraphicsLayer")) continue

        try {
            val hasTransform = hasNonIdentityTransformInElement(element, clazz)
            if (hasTransform) {
                return index
            }
        } catch (_: Exception) {
        }
    }
    return null
}

private fun hasNonIdentityTransformInElement(element: Any, clazz: Class<*>): Boolean {
    fun floatField(name: String, identity: Float): Boolean {
        return try {
            val field = clazz.getDeclaredField(name).apply { isAccessible = true }
            val value = field.get(element) as? Float ?: identity
            value != identity
        } catch (_: Exception) {
            false
        }
    }

    return floatField("rotationX", 0f) ||
            floatField("rotationY", 0f) ||
            floatField("rotationZ", 0f) ||
            floatField("scaleX", 1f) ||
            floatField("scaleY", 1f) ||
            floatField("translationX", 0f) ||
            floatField("translationY", 0f)
}

object EchoHighlightInjection {

    private val getModifierMethod by lazy {
        LayoutNodeReflection.layoutNodeClass.getMethod("getModifier")
    }

    private val setModifierMethod by lazy {
        LayoutNodeReflection.layoutNodeClass.getMethod("setModifier", Modifier::class.java)
    }

    internal fun injectHighlightDirect(layoutNode: Any, style: EchoHighlightStyle) {
        val current = getModifier(layoutNode)
        val elements = linearize(current)

        val existingHighlight = elements.filterIsInstance<EchoHighlightElement>().firstOrNull()
        if (existingHighlight != null && existingHighlight.style == style) {
            return
        }

        val filtered = elements.filterNot { it is EchoHighlightElement }
        val clipInfo = findClipInfo(filtered)
        val lastBackgroundIndex = findLastBackgroundIndex(filtered)
        val transformIndex = findTransformIndex(filtered)

        val insertIndex = when {
            lastBackgroundIndex != null && transformIndex != null ->
                maxOf(lastBackgroundIndex, transformIndex) + 1
            lastBackgroundIndex != null -> lastBackgroundIndex + 1
            transformIndex != null -> transformIndex + 1
            clipInfo != null -> clipInfo.index + 1
            else -> 0
        }

        val highlight = EchoHighlightElement(style, clipInfo?.shape)
        val injected = filtered.toMutableList().apply { add(insertIndex, highlight) }

        val rebuilt = rebuild(injected)
        setModifier(layoutNode, rebuilt)
    }

    fun removeHighlight(layoutNode: Any) {
        val current = getModifier(layoutNode)
        val elements = linearize(current)

        val filtered = elements.filterNot { it is EchoHighlightElement }
        if (filtered.size == elements.size) return

        val rebuilt = rebuild(filtered)
        setModifier(layoutNode, rebuilt)
    }

    private fun getModifier(layoutNode: Any): Modifier =
        getModifierMethod.invoke(layoutNode) as Modifier

    private fun setModifier(layoutNode: Any, modifier: Modifier) {
        setModifierMethod.invoke(layoutNode, modifier)
    }

    private fun linearize(modifier: Modifier): List<Modifier.Element> =
        modifier.foldIn(mutableListOf()) { acc, element -> acc.apply { add(element) } }

    private fun rebuild(elements: List<Modifier.Element>): Modifier =
        elements.foldRight(Modifier as Modifier) { element, acc -> element.then(acc) }

    private data class ClipInfo(val index: Int, val shape: Shape?)

    private fun findLastBackgroundIndex(elements: List<Modifier.Element>): Int? {
        var lastIndex: Int? = null
        for ((index, element) in elements.withIndex()) {
            val className = element::class.java.simpleName
            if (className.contains("Background")) {
                lastIndex = index
            }
        }
        return lastIndex
    }

    private fun findClipInfo(elements: List<Modifier.Element>): ClipInfo? {
        for ((index, element) in elements.withIndex()) {
            val clazz = element::class.java
            if (!clazz.simpleName.contains("GraphicsLayer")) continue

            try {
                val clipField = clazz.getDeclaredField("clip").apply { isAccessible = true }
                val isClip = clipField.get(element) as? Boolean ?: false
                if (isClip) {
                    val shapeField = clazz.getDeclaredField("shape").apply { isAccessible = true }
                    val shape = shapeField.get(element) as? Shape
                    return ClipInfo(index, shape)
                }
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun findTransformIndex(elements: List<Modifier.Element>): Int? {
        for ((index, element) in elements.withIndex()) {
            val clazz = element::class.java
            val simpleName = clazz.simpleName

            if (simpleName.contains("BlockGraphicsLayer")) {
                return index
            }

            if (!simpleName.contains("GraphicsLayer")) continue

            try {
                val hasTransform = hasNonIdentityTransform(element, clazz)
                if (hasTransform) {
                    return index
                }
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun hasNonIdentityTransform(element: Any, clazz: Class<*>): Boolean {
        fun floatField(name: String, identity: Float): Boolean {
            return try {
                val field = clazz.getDeclaredField(name).apply { isAccessible = true }
                val value = field.get(element) as? Float ?: identity
                value != identity
            } catch (_: Exception) {
                false
            }
        }

        return floatField("rotationX", 0f) ||
                floatField("rotationY", 0f) ||
                floatField("rotationZ", 0f) ||
                floatField("scaleX", 1f) ||
                floatField("scaleY", 1f) ||
                floatField("translationX", 0f) ||
                floatField("translationY", 0f)
    }
}
