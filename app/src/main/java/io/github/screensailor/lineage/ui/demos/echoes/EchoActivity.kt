package io.github.screensailor.lineage.ui.demos.echoes

import android.content.Context
import android.view.Choreographer
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.findViewTreeCompositionContext
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TreeSnapshot(
    val nodes: List<EchoNode>,
    val timestamp: Long = System.nanoTime()
) {
    companion object {
        val Empty = TreeSnapshot(emptyList(), 0)
    }
}

abstract class EchoActivity : ComponentActivity() {

    val echoRegistry: EchoRegistry = EchoRegistryImpl()
    val shadowTree: ShadowTree = ShadowTree()
    val coordinatesCache: CoordinatesCache = CoordinatesCache()
    val semanticsCache: SemanticsCache = SemanticsCache()

    private val _treeSnapshot = MutableStateFlow(TreeSnapshot.Empty)
    val treeSnapshot: StateFlow<TreeSnapshot> = _treeSnapshot.asStateFlow()

    var treeSnapshotEnabled = true
    var useShadowTree = true
    var useCoordinatesCache = true
    var useSemanticsCache = true

    private var androidComposeView: Any? = null
    private var snapshotPending = false

    private fun handleApplierEvent(event: ApplierEvent, node: Any?) {
        when (event) {
            ApplierEvent.Remove -> node?.let { echoRegistry.clearIntent(it) }
            ApplierEvent.Clear -> echoRegistry.clearAll()
            ApplierEvent.BatchEnd -> {
                semanticsCache.invalidate()
                scheduleTreeSnapshot()
            }
            else -> {}
        }
    }

    private fun scheduleTreeSnapshot() {
        if (!treeSnapshotEnabled || snapshotPending) return
        snapshotPending = true
        Choreographer.getInstance().postFrameCallback {
            snapshotPending = false
            captureTreeSnapshot()
        }
    }

    private fun captureTreeSnapshot() {
        if (!treeSnapshotEnabled) return

        val composeView = androidComposeView ?: return

        if (useSemanticsCache) {
            (composeView as? RootForTest)?.let { root ->
                semanticsCache.refresh(root)
            }
        }

        val nodes = if (useShadowTree) {
            shadowTree.toEchoNodes { layoutNode ->
                extractNodeDescription(layoutNode, if (useSemanticsCache) semanticsCache else null)
            }
        } else {
            val root = composeView.getLayoutRoot()
            walkLayoutTree(root, startPath = intArrayOf(0), relativeFromStart = true)
        }

        _treeSnapshot.value = TreeSnapshot(nodes)
    }

    fun setContent(content: @Composable () -> Unit) {
        val parentContext: CompositionContext =
            window.decorView.findViewTreeCompositionContext() ?: createFallbackRecomposer()

        val composeView = createAndroidComposeView(this, parentContext.effectCoroutineContext)
        androidComposeView = composeView
        val viewAsView = composeView as View

        val existingComposition = viewAsView.getWrappedCompositionTag()
        if (existingComposition != null) {
            existingComposition.setContent(content)
            return
        }

        val root = composeView.getLayoutRoot()
        shadowTree.setRoot(root)

        val uiApplier = createUiApplier(root)
        val observingApplier = ObservingApplier(
            delegate = uiApplier,
            onEvent = { event, node -> handleApplierEvent(event, node) },
            onSetModifier = { node, incoming ->
                val withCoords = if (useCoordinatesCache) {
                    androidx.compose.ui.Modifier
                        .echoCoordinates(node as Any, coordinatesCache)
                        .then(incoming)
                } else {
                    incoming
                }
                echoRegistry.applyTransform(node as Any, withCoords)
            },
            onInsert = { _, index, node ->
                shadowTree.insert(index, node as Any)
            },
            onRemove = { _, index, count ->
                shadowTree.remove(index, count)
            },
            onMove = { _, from, to, count ->
                shadowTree.move(from, to, count)
            },
            onDown = { node ->
                shadowTree.down(node as Any)
            },
            onUp = {
                shadowTree.up()
            },
            onClear = {
                shadowTree.clear()
            }
        )
        val composition = Composition(observingApplier, parentContext)
        val wrappedComposition = createWrappedComposition(composeView, composition)

        viewAsView.setWrappedCompositionTag(wrappedComposition)
        wrappedComposition.setContent(content)

        setContentView(
            viewAsView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun createFallbackRecomposer(): Recomposer {
        val uiContext = AndroidUiDispatcher.Main
        val scope = CoroutineScope(SupervisorJob() + uiContext)
        val recomposer = Recomposer(scope.coroutineContext)

        scope.launch {
            recomposer.runRecomposeAndApplyChanges()
        }

        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    recomposer.cancel()
                    scope.cancel()
                }
            }
        )

        return recomposer
    }
}

private object ReflectionCache {

    val androidComposeViewCtor by lazy {
        LayoutNodeReflection.androidComposeViewClass.getDeclaredConstructor(
            Context::class.java,
            CoroutineContext::class.java
        ).apply {
            isAccessible = true
        }
    }

    val uiApplierClass: Class<*> by lazy {
        Class.forName("androidx.compose.ui.node.UiApplier")
    }

    val uiApplierCtor by lazy {
        uiApplierClass.getDeclaredConstructor(LayoutNodeReflection.layoutNodeClass).apply {
            isAccessible = true
        }
    }

    val wrappedCompositionClass: Class<*> by lazy {
        Class.forName("androidx.compose.ui.platform.WrappedComposition")
    }

    val wrappedCompositionCtor by lazy {
        wrappedCompositionClass.getDeclaredConstructor(
            LayoutNodeReflection.androidComposeViewClass,
            Composition::class.java
        ).apply {
            isAccessible = true
        }
    }

    val wrappedCompositionTagId: Int by lazy {
        val rClass = Class.forName("androidx.compose.ui.R\$id")
        rClass.getDeclaredField("wrapped_composition_tag").getInt(null)
    }
}

private fun createAndroidComposeView(
    context: Context,
    coroutineContext: CoroutineContext
): Any = ReflectionCache.androidComposeViewCtor.newInstance(context, coroutineContext)

@Suppress("UNCHECKED_CAST")
private fun createUiApplier(root: Any): Applier<Any?> =
    ReflectionCache.uiApplierCtor.newInstance(root) as Applier<Any?>

private fun createWrappedComposition(
    androidComposeView: Any,
    composition: Composition
): Composition = ReflectionCache.wrappedCompositionCtor
    .newInstance(androidComposeView, composition) as Composition

private fun View.setWrappedCompositionTag(composition: Composition) {
    setTag(ReflectionCache.wrappedCompositionTagId, composition)
}

private fun View.getWrappedCompositionTag(): Composition? =
    getTag(ReflectionCache.wrappedCompositionTagId) as? Composition
