package io.github.screensailor.lineage.ui.demos.paths

import androidx.compose.runtime.Immutable
import kotlin.math.abs

@Immutable
data class Trek(
    val stride: Int?,
    val trail: List<Int>
) {
    val isAbsolute: Boolean
        get() = stride == 0 && trail.isNotEmpty()

    val isIdentity: Boolean
        get() = stride == 0 && trail.isEmpty()

    val isUnrelated: Boolean
        get() = stride == null

    val isRelated: Boolean
        get() = stride != null

    val isDescendantDelta: Boolean
        get() = stride != null && stride > 0

    val isAncestorDelta: Boolean
        get() = stride != null && stride < 0

    operator fun minus(other: Trek): Trek {
        if (this.isUnrelated || other.isUnrelated) return Unrelated

        return when {
            this.stride == 0 && other.stride == 0 ->
                absoluteMinusAbsolute(this.trail, other.trail)

            this.isDescendantDelta && other.isDescendantDelta ->
                deltaMinusDeltaSameSign(this.trail, other.trail, positive = true)

            this.isAncestorDelta && other.isAncestorDelta ->
                deltaMinusDeltaSameSign(this.trail, other.trail, positive = false)

            else -> Unrelated
        }
    }

    operator fun plus(delta: Trek): Trek {
        if (this.isUnrelated || delta.isUnrelated) return Unrelated

        return when {
            this.stride == 0 && delta.stride == 0 && delta.trail.isEmpty() ->
                this

            this.stride == 0 && delta.isDescendantDelta ->
                Trek(stride = 0, trail = trail + delta.trail)

            this.stride == 0 && delta.isAncestorDelta -> {
                val k = abs(delta.stride!!)
                if (trail.size >= k && trail.takeLast(k) == delta.trail)
                    Trek(stride = 0, trail = trail.dropLast(k))
                else
                    Unrelated
            }

            this.isDescendantDelta && delta.isDescendantDelta ->
                Trek(stride = stride!! + delta.stride!!, trail = trail + delta.trail)

            this.isAncestorDelta && delta.isAncestorDelta ->
                Trek(stride = stride!! + delta.stride!!, trail = delta.trail + trail)

            this.isDescendantDelta && delta.isAncestorDelta -> {
                val k1 = stride!!
                val k2 = abs(delta.stride!!)
                if (k2 <= k1 && trail.takeLast(k2) == delta.trail) {
                    val prefix = trail.dropLast(k2)
                    Trek(stride = prefix.size, trail = prefix)
                } else {
                    Unrelated
                }
            }

            this.isAncestorDelta && delta.isDescendantDelta -> {
                val k1 = abs(stride!!)
                val k2 = delta.stride!!
                if (k2 >= k1 && delta.trail.take(k1) == trail) {
                    val suffix = delta.trail.drop(k1)
                    Trek(stride = suffix.size, trail = suffix)
                } else {
                    Unrelated
                }
            }

            else -> Unrelated
        }
    }

    companion object {
        val Root = Trek(stride = 0, trail = emptyList())
        val Unrelated = Trek(stride = null, trail = emptyList())

        fun absolute(vararg segments: Int): Trek =
            Trek(stride = 0, trail = segments.toList())

        fun absolute(segments: List<Int>): Trek =
            Trek(stride = 0, trail = segments)

        fun descendant(vararg segments: Int): Trek =
            Trek(stride = segments.size, trail = segments.toList())

        fun ancestor(vararg segments: Int): Trek =
            Trek(stride = -segments.size, trail = segments.toList())

        private fun absoluteMinusAbsolute(a: List<Int>, b: List<Int>): Trek {
            val common = a.zip(b).takeWhile { (x, y) -> x == y }.size

            return when {
                common == a.size && common == b.size ->
                    Trek(stride = 0, trail = emptyList())

                common == b.size -> {
                    val segments = a.subList(common, a.size)
                    Trek(stride = segments.size, trail = segments)
                }

                common == a.size -> {
                    val segments = b.subList(common, b.size)
                    Trek(stride = -segments.size, trail = segments)
                }

                else -> Unrelated
            }
        }

        private fun deltaMinusDeltaSameSign(
            t1: List<Int>,
            t2: List<Int>,
            positive: Boolean
        ): Trek {
            val common = t1.zip(t2).takeWhile { (x, y) -> x == y }.size

            return when {
                common == t1.size && common == t2.size ->
                    Trek(stride = 0, trail = emptyList())

                common == t2.size -> {
                    val suffix = t1.subList(common, t1.size)
                    Trek(
                        stride = if (positive) suffix.size else -suffix.size,
                        trail = suffix
                    )
                }

                common == t1.size -> {
                    val suffix = t2.subList(common, t2.size)
                    Trek(
                        stride = if (positive) -suffix.size else suffix.size,
                        trail = suffix
                    )
                }

                else -> Unrelated
            }
        }
    }
}
