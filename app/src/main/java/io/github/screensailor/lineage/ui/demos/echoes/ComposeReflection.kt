package io.github.screensailor.lineage.ui.demos.echoes

import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity

object LayoutNodeReflection {

    val layoutNodeClass: Class<*> by lazy {
        Class.forName("androidx.compose.ui.node.LayoutNode")
    }

    val setModifierLambda: Any? by lazy {
        try {
            val companionField = Class.forName("androidx.compose.ui.node.ComposeUiNode")
                .getDeclaredField("Companion")
                .apply { isAccessible = true }
            val companion = companionField.get(null)
            Class.forName("androidx.compose.ui.node.ComposeUiNode\$Companion")
                .getDeclaredField("SetModifier")
                .apply { isAccessible = true }
                .get(companion)
        } catch (e: Exception) {
            android.util.Log.w("LayoutNodeReflection", "setModifierLambda not available: ${e.message}")
            null
        }
    }

    val foldedChildrenField by lazy {
        layoutNodeClass.getDeclaredField("_foldedChildren").apply {
            isAccessible = true
        }
    }

    val measurePolicyField by lazy {
        layoutNodeClass.getDeclaredField("measurePolicy").apply {
            isAccessible = true
        }
    }

    val semanticsConfigurationField by lazy {
        layoutNodeClass.getDeclaredField("_semanticsConfiguration").apply {
            isAccessible = true
        }
    }

    val mutableVectorClass: Class<*> by lazy {
        Class.forName("androidx.compose.ui.node.MutableVectorWithMutationTracking")
    }

    val vectorSizeMethod by lazy {
        mutableVectorClass.getDeclaredMethod("getSize")
    }

    val vectorGetMethod by lazy {
        mutableVectorClass.getDeclaredMethod("get", Int::class.java)
    }

    val androidComposeViewClass: Class<*> by lazy {
        Class.forName("androidx.compose.ui.platform.AndroidComposeView")
    }

    val getRootMethod by lazy {
        androidComposeViewClass.getDeclaredMethod("getRoot").apply {
            isAccessible = true
        }
    }

    val nodeChainClass: Class<*> by lazy {
        Class.forName("androidx.compose.ui.node.NodeChain")
    }

    val nodesField by lazy {
        layoutNodeClass.getDeclaredField("nodes").apply {
            isAccessible = true
        }
    }

    val outerCoordinatorField by lazy {
        nodeChainClass.getDeclaredField("outerCoordinator").apply {
            isAccessible = true
        }
    }
}

fun ComponentActivity.findAndroidComposeView(): Any? {
    fun findInViewHierarchy(view: View): Any? {
        if (LayoutNodeReflection.androidComposeViewClass.isInstance(view)) {
            return view
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findInViewHierarchy(view.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }
    return findInViewHierarchy(window.decorView)
}

fun Any.getLayoutRoot(): Any = LayoutNodeReflection.getRootMethod.invoke(this)!!

fun getLookaheadCoordinator(layoutNode: Any): Any? {
    return try {
        val nodes = LayoutNodeReflection.nodesField.get(layoutNode)
        LayoutNodeReflection.outerCoordinatorField.get(nodes)
    } catch (e: Exception) {
        null
    }
}

fun getParentData(layoutNode: Any): Any? {
    return try {
        val nodes = LayoutNodeReflection.nodesField.get(layoutNode) ?: return null
        val outerCoordinator = LayoutNodeReflection.outerCoordinatorField.get(nodes) ?: return null
        val getParentData = outerCoordinator::class.java.getMethod("getParentData")
        getParentData.invoke(outerCoordinator)
    } catch (e: Exception) {
        null
    }
}
