package io.github.screensailor.lineage.ui.demos.reflections

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import java.util.concurrent.atomic.AtomicLong

@JvmInline
value class MyOwnId(val raw: Long)

val MyOwnKey = SemanticsPropertyKey<MyOwnId>("MyOwnId")

object IdGenerator {
    private val counter = AtomicLong(0)
    fun next() = MyOwnId(counter.incrementAndGet())
}

@Stable
class OurStore<Properties>(private val default: () -> Properties) {
    val store = mutableStateMapOf<MyOwnId, Properties>()

    fun own(id: MyOwnId): Properties = store.getOrPut(id, default)
}

data class My<Properties>(
    val id: MyOwnId,
    val modifier: Modifier,
    internal val our: OurStore<Properties>,
    internal val view: View
) {
    val own: Properties
        get() = our.own(id)
}

val LocalOurStore = compositionLocalOf<OurStore<*>?> { null }

@Composable
fun <P> OurOwn(
    default: () -> P,
    content: @Composable () -> Unit
) {
    val store = remember { OurStore(default) }
    CompositionLocalProvider(LocalOurStore provides store, content)
}

@Composable
fun <P> OurOwn(
    default: P,
    content: @Composable () -> Unit
) = OurOwn(default = { default }, content)

@Composable
inline fun <reified Properties> myOwn(): My<Properties> {
    @Suppress("UNCHECKED_CAST")
    val our = LocalOurStore.current as? OurStore<Properties>
        ?: error("OurStore<${Properties::class.simpleName}> not provided")
    val view = androidx.compose.ui.platform.LocalView.current
    val id = remember { IdGenerator.next() }

    androidx.compose.runtime.DisposableEffect(id) {
        onDispose {
            our.store.remove(id)
        }
    }

    val modifier = remember(id) { Modifier.semantics { this[MyOwnKey] = id } }

    return remember(id, our, view) {
        My(id, modifier, our, view)
    }
}

fun Color.contrastingText(): Color =
    if (this == Color.White) Color.Black else Color.White

val SemanticsNode.myOwnId: MyOwnId?
    get() = if (MyOwnKey in config) config[MyOwnKey] else null

fun SemanticsNode.ancestors(withMe: Boolean = false): Sequence<SemanticsNode> = sequence {
    if (withMe) yield(this@ancestors)
    yieldAll(generateSequence(parent) { it.parent })
}

fun SemanticsNode.descendants(withMe: Boolean = false): Sequence<SemanticsNode> = sequence {
    if (withMe) yield(this@descendants)
    children.forEach { child ->
        yieldAll(child.descendants(withMe = true))
    }
}

fun SemanticsNode.findById(id: MyOwnId): SemanticsNode? =
    if (myOwnId == id) this
    else children.firstNotNullOfOrNull { it.findById(id) }

fun SemanticsNode.depthRelativeTo(other: SemanticsNode): Int? {
    if (this.id == other.id) return 0

    generateSequence(parent) { it.parent }
        .forEachIndexed { index, ancestor ->
            if (ancestor.id == other.id) return index + 1
        }

    generateSequence(other.parent) { it.parent }
        .forEachIndexed { index, ancestor ->
            if (ancestor.id == this.id) return -(index + 1)
        }

    return null
}

@OptIn(ExperimentalComposeUiApi::class)
fun View.findSemanticsRoot(): SemanticsNode? =
    generateSequence(this) { it.parent as? View }
        .mapNotNull { it as? ViewRootForTest }
        .firstOrNull()
        ?.semanticsOwner
        ?.unmergedRootSemanticsNode
