package io.github.screensailor.lineage.ui.demos.echoes

fun walkLayoutTree(
    root: Any,
    startPath: IntArray = intArrayOf(0),
    relativeFromStart: Boolean = false
): List<EchoNode> {
    val startNode = navigateToPath(root, startPath) ?: return emptyList()
    return walkSubtree(startNode, startPath, relativeFromStart, startPath)
}

private fun navigateToPath(root: Any, path: IntArray): Any? {
    if (path.isEmpty()) return root

    var current = root
    for (i in 1 until path.size) {
        val childIndex = path[i]
        val children = LayoutNodeReflection.foldedChildrenField.get(current)
        val childCount = LayoutNodeReflection.vectorSizeMethod.invoke(children) as Int

        if (childIndex >= childCount) return null

        current = LayoutNodeReflection.vectorGetMethod.invoke(children, childIndex)!!
    }
    return current
}

private fun walkSubtree(
    node: Any,
    absolutePath: IntArray,
    relativeFromStart: Boolean,
    startPath: IntArray
): List<EchoNode> {
    val result = mutableListOf<EchoNode>()

    val children = LayoutNodeReflection.foldedChildrenField.get(node)
    val childCount = LayoutNodeReflection.vectorSizeMethod.invoke(children) as Int

    val outputPath = if (relativeFromStart) {
        intArrayOf(0) + absolutePath.drop(startPath.size).toIntArray()
    } else {
        absolutePath
    }

    val description = extractNodeDescription(node)
    result.add(EchoNode.create(outputPath, description, childCount, node))

    for (i in 0 until childCount) {
        val child = LayoutNodeReflection.vectorGetMethod.invoke(children, i)!!
        result.addAll(walkSubtree(child, absolutePath + i, relativeFromStart, startPath))
    }

    return result
}

fun extractNodeDescription(node: Any, semanticsCache: SemanticsCache? = null): String {
    val echoName = extractEchoName(node)
    if (echoName != null) {
        return echoName
    }

    val textContent = extractTextContent(node, semanticsCache)
    if (textContent != null) {
        return "\"$textContent\""
    }

    val measurePolicy = LayoutNodeReflection.measurePolicyField.get(node) ?: return "?"
    val policyClassName = measurePolicy::class.java.simpleName
    val fullClassName = measurePolicy::class.java.name
    val baseName = cleanupPolicyName(policyClassName)
    if (baseName == "?") {
        return cleanupPolicyName(fullClassName.substringAfterLast("."))
    }
    return baseName
}

private fun extractEchoName(node: Any): String? {
    val parentData = getParentData(node) ?: return null
    return (parentData as? EchoName)?.value
}

private fun extractTextContent(node: Any, semanticsCache: SemanticsCache? = null): String? {
    if (semanticsCache != null) {
        semanticsCache.getTextFor(node)?.let { return it }
        semanticsCache.getEditableTextFor(node)?.let { return it }
        return null
    }

    return extractTextContentViaReflection(node)
}

private fun extractTextContentViaReflection(node: Any): String? {
    return try {
        val semanticsConfig = LayoutNodeReflection.semanticsConfigurationField.get(node)
            ?: return null

        val configIterable = semanticsConfig as? Iterable<*> ?: return null

        for (entry in configIterable) {
            if (entry == null) continue

            val entryClass = entry::class.java
            val keyField = entryClass.getDeclaredMethod("getKey")
            val valueField = entryClass.getDeclaredMethod("getValue")

            val key = keyField.invoke(entry) ?: continue
            val keyName = key::class.java.getDeclaredMethod("getName").invoke(key) as? String
                ?: continue

            if (keyName == "Text") {
                val value = valueField.invoke(entry) ?: continue
                val textList = value as? List<*> ?: continue
                if (textList.isNotEmpty()) {
                    val firstText = textList[0] ?: continue
                    val text = firstText.toString()
                    return if (text.length > 20) text.take(17) + "..." else text
                }
            }
        }
        null
    } catch (_: Exception) {
        null
    }
}

private fun cleanupPolicyName(policyName: String): String = when {
    policyName.contains("Column") -> "Column"
    policyName.contains("Row") -> "Row"
    policyName.contains("Box") && !policyName.contains("Checkbox") -> "Box"
    policyName.contains("Surface") -> "Surface"
    policyName.contains("Scaffold") -> "Scaffold"
    policyName.contains("TabRow") -> "TabRow"
    policyName.contains("TabBaseline") -> "Tab"
    policyName.contains("Tab") -> "Tab"
    policyName.contains("Text") -> "Text"
    policyName.contains("Spacer") -> "Spacer"
    policyName.contains("Root") -> "Root"
    policyName.contains("MultiContent") -> "TabRow"
    policyName.contains("Companion") -> "Layout"
    policyName.contains("Error") -> "Layout"
    policyName.contains("Empty") -> "Spacer"
    policyName.contains("Paragraph") -> "Text"
    policyName.contains("BasicText") -> "Text"
    policyName.contains("TextController") -> "Text"
    policyName.isEmpty() -> "?"
    else -> policyName
}
