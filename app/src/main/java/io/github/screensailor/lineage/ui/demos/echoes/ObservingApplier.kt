package io.github.screensailor.lineage.ui.demos.echoes

import androidx.compose.runtime.Applier
import androidx.compose.ui.Modifier

class ObservingApplier<N>(
    private val delegate: Applier<N>,
    private val onEvent: (ApplierEvent, N?) -> Unit,
    private val onSetModifier: ((node: N, incoming: Modifier) -> Modifier)? = null,
    private val onInsert: ((parent: N, index: Int, node: N) -> Unit)? = null,
    private val onRemove: ((parent: N, index: Int, count: Int) -> Unit)? = null,
    private val onMove: ((parent: N, from: Int, to: Int, count: Int) -> Unit)? = null,
    private val onDown: ((node: N) -> Unit)? = null,
    private val onUp: (() -> Unit)? = null,
    private val onClear: (() -> Unit)? = null
) : Applier<N> by delegate {

    override fun onBeginChanges() {
        onEvent(ApplierEvent.BatchStart, null)
        delegate.onBeginChanges()
    }

    override fun onEndChanges() {
        delegate.onEndChanges()
        onEvent(ApplierEvent.BatchEnd, null)
    }

    override fun down(node: N) {
        delegate.down(node)
        onDown?.invoke(node)
        onEvent(ApplierEvent.Down, node)
    }

    override fun up() {
        delegate.up()
        onUp?.invoke()
        onEvent(ApplierEvent.Up, null)
    }

    override fun insertTopDown(index: Int, instance: N) {
        onInsert?.invoke(delegate.current, index, instance)
        onEvent(ApplierEvent.InsertTopDown, instance)
        delegate.insertTopDown(index, instance)
    }

    override fun insertBottomUp(index: Int, instance: N) {
        delegate.insertBottomUp(index, instance)
        onEvent(ApplierEvent.InsertBottomUp, instance)
    }

    override fun remove(index: Int, count: Int) {
        onRemove?.invoke(delegate.current, index, count)
        onEvent(ApplierEvent.Remove, delegate.current)
        delegate.remove(index, count)
    }

    override fun move(from: Int, to: Int, count: Int) {
        onMove?.invoke(delegate.current, from, to, count)
        onEvent(ApplierEvent.Move, delegate.current)
        delegate.move(from, to, count)
    }

    override fun clear() {
        onClear?.invoke()
        onEvent(ApplierEvent.Clear, delegate.current)
        delegate.clear()
    }

    override fun apply(block: N.(Any?) -> Unit, value: Any?) {
        val node = delegate.current
        val interceptor = onSetModifier
        val setModifierLambda = LayoutNodeReflection.setModifierLambda

        if (interceptor != null && setModifierLambda != null && block === setModifierLambda && value is Modifier) {
            val wrapped = interceptor(node, value)
            delegate.apply(block, wrapped)
        } else {
            delegate.apply(block, value)
        }

        onEvent(ApplierEvent.NodeUpdated, node)
    }
}
