package io.github.screensailor.lineage

import io.github.screensailor.lineage.ui.demos.paths.Trek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrekTest {

    @Test
    fun absolute_path_construction() {
        val path = Trek.absolute(0, 1, 2)

        assertEquals(0, path.stride)
        assertEquals(listOf(0, 1, 2), path.trail)
        assertTrue(path.isAbsolute)
        assertFalse(path.isIdentity)
        assertTrue(path.isRelated)
    }

    @Test
    fun root_is_identity() {
        val root = Trek.Root

        assertEquals(0, root.stride)
        assertEquals(emptyList<Int>(), root.trail)
        assertTrue(root.isIdentity)
        assertFalse(root.isAbsolute)
        assertTrue(root.isRelated)
    }

    @Test
    fun unrelated_construction() {
        val unrelated = Trek.Unrelated

        assertNull(unrelated.stride)
        assertEquals(emptyList<Int>(), unrelated.trail)
        assertTrue(unrelated.isUnrelated)
        assertFalse(unrelated.isRelated)
    }

    @Test
    fun descendant_delta_construction() {
        val delta = Trek.descendant(2, 3)

        assertEquals(2, delta.stride)
        assertEquals(listOf(2, 3), delta.trail)
        assertTrue(delta.isDescendantDelta)
        assertFalse(delta.isAncestorDelta)
    }

    @Test
    fun ancestor_delta_construction() {
        val delta = Trek.ancestor(2, 3)

        assertEquals(-2, delta.stride)
        assertEquals(listOf(2, 3), delta.trail)
        assertTrue(delta.isAncestorDelta)
        assertFalse(delta.isDescendantDelta)
    }

    @Test
    fun minus_same_node_yields_identity() {
        val path = Trek.absolute(0, 1, 2)
        val delta = path - path

        assertEquals(0, delta.stride)
        assertEquals(emptyList<Int>(), delta.trail)
        assertTrue(delta.isIdentity)
    }

    @Test
    fun minus_descendant_yields_positive_stride() {
        val ancestor = Trek.absolute(0, 1)
        val descendant = Trek.absolute(0, 1, 2, 3)

        val delta = descendant - ancestor

        assertEquals(2, delta.stride)
        assertEquals(listOf(2, 3), delta.trail)
    }

    @Test
    fun minus_ancestor_yields_negative_stride() {
        val ancestor = Trek.absolute(0, 1)
        val descendant = Trek.absolute(0, 1, 2, 3)

        val delta = ancestor - descendant

        assertEquals(-2, delta.stride)
        assertEquals(listOf(2, 3), delta.trail)
    }

    @Test
    fun minus_siblings_yields_unrelated() {
        val sibling1 = Trek.absolute(0, 1, 2)
        val sibling2 = Trek.absolute(0, 1, 3)

        val delta = sibling1 - sibling2

        assertTrue(delta.isUnrelated)
        assertNull(delta.stride)
    }

    @Test
    fun minus_cousins_yields_unrelated() {
        val cousin1 = Trek.absolute(0, 1, 2, 5)
        val cousin2 = Trek.absolute(0, 1, 3, 6)

        val delta = cousin1 - cousin2

        assertTrue(delta.isUnrelated)
    }

    @Test
    fun minus_different_roots_yields_unrelated() {
        val path1 = Trek.absolute(0, 1, 2)
        val path2 = Trek.absolute(1, 2, 3)

        val delta = path1 - path2

        assertTrue(delta.isUnrelated)
    }

    @Test
    fun minus_with_unrelated_operand_yields_unrelated() {
        val path = Trek.absolute(0, 1, 2)

        assertEquals(Trek.Unrelated, path - Trek.Unrelated)
        assertEquals(Trek.Unrelated, Trek.Unrelated - path)
    }

    @Test
    fun stride_magnitude_equals_trail_size_for_descendant() {
        val ancestor = Trek.absolute(0, 1)
        val descendant = Trek.absolute(0, 1, 2, 3, 4)

        val delta = descendant - ancestor

        assertEquals(delta.stride, delta.trail.size)
    }

    @Test
    fun stride_magnitude_equals_trail_size_for_ancestor() {
        val ancestor = Trek.absolute(0, 1)
        val descendant = Trek.absolute(0, 1, 2, 3, 4)

        val delta = ancestor - descendant

        assertEquals(delta.stride?.let { -it }, delta.trail.size)
    }

    @Test
    fun plus_identity_returns_same_path() {
        val path = Trek.absolute(0, 1, 2)
        val identity = Trek.Root

        val result = path + identity

        assertEquals(path, result)
    }

    @Test
    fun plus_descendant_delta_extends_path() {
        val base = Trek.absolute(0, 1)
        val delta = Trek.descendant(2, 3)

        val result = base + delta

        assertEquals(Trek.absolute(0, 1, 2, 3), result)
    }

    @Test
    fun plus_ancestor_delta_shortens_path() {
        val base = Trek.absolute(0, 1, 2, 3)
        val delta = Trek.ancestor(2, 3)

        val result = base + delta

        assertEquals(Trek.absolute(0, 1), result)
    }

    @Test
    fun plus_ancestor_delta_misaligned_yields_unrelated() {
        val base = Trek.absolute(0, 1, 2, 3)
        val delta = Trek.ancestor(9, 9)

        val result = base + delta

        assertTrue(result.isUnrelated)
    }

    @Test
    fun plus_with_unrelated_yields_unrelated() {
        val path = Trek.absolute(0, 1, 2)

        assertEquals(Trek.Unrelated, path + Trek.Unrelated)
        assertEquals(Trek.Unrelated, Trek.Unrelated + path)
    }

    @Test
    fun roundtrip_descendant() {
        val a = Trek.absolute(0, 1, 2, 3)
        val b = Trek.absolute(0, 1)

        val delta = a - b
        val reconstructed = b + delta

        assertEquals(a, reconstructed)
    }

    @Test
    fun roundtrip_ancestor() {
        val a = Trek.absolute(0, 1)
        val b = Trek.absolute(0, 1, 2, 3)

        val delta = a - b
        val reconstructed = b + delta

        assertEquals(a, reconstructed)
    }

    @Test
    fun roundtrip_same_node() {
        val a = Trek.absolute(0, 1, 2)
        val b = Trek.absolute(0, 1, 2)

        val delta = a - b
        val reconstructed = b + delta

        assertEquals(a, reconstructed)
    }

    @Test
    fun symmetry_magnitudes_equal() {
        val a = Trek.absolute(0, 1, 2, 3)
        val b = Trek.absolute(0, 1)

        val deltaAB = a - b
        val deltaBA = b - a

        assertEquals(
            deltaAB.stride?.let { kotlin.math.abs(it) },
            deltaBA.stride?.let { kotlin.math.abs(it) }
        )
    }

    @Test
    fun symmetry_strides_opposite_signs() {
        val a = Trek.absolute(0, 1, 2, 3)
        val b = Trek.absolute(0, 1)

        val deltaAB = a - b
        val deltaBA = b - a

        assertEquals(deltaAB.stride, deltaBA.stride?.let { -it })
    }

    @Test
    fun symmetry_trails_equal() {
        val a = Trek.absolute(0, 1, 2, 3)
        val b = Trek.absolute(0, 1)

        val deltaAB = a - b
        val deltaBA = b - a

        assertEquals(deltaAB.trail, deltaBA.trail)
    }

    @Test
    fun root_minus_any_is_ancestor() {
        val root = Trek.Root
        val deep = Trek.absolute(0, 1, 2)

        val delta = root - deep

        assertEquals(-3, delta.stride)
        assertEquals(listOf(0, 1, 2), delta.trail)
    }

    @Test
    fun any_minus_root_is_descendant() {
        val root = Trek.Root
        val deep = Trek.absolute(0, 1, 2)

        val delta = deep - root

        assertEquals(3, delta.stride)
        assertEquals(listOf(0, 1, 2), delta.trail)
    }

    @Test
    fun single_segment_paths() {
        val a = Trek.absolute(0)
        val b = Trek.absolute(0, 1)

        val deltaDown = b - a
        val deltaUp = a - b

        assertEquals(1, deltaDown.stride)
        assertEquals(listOf(1), deltaDown.trail)

        assertEquals(-1, deltaUp.stride)
        assertEquals(listOf(1), deltaUp.trail)
    }

    @Test
    fun highlighting_use_case_ancestor() {
        val myPath = Trek.absolute(0, 1)
        val clickedPath = Trek.absolute(0, 1, 2, 3)

        val d = (myPath - clickedPath).stride

        assertTrue(d != null && d < 0)
    }

    @Test
    fun highlighting_use_case_descendant() {
        val myPath = Trek.absolute(0, 1, 2, 3)
        val clickedPath = Trek.absolute(0, 1)

        val d = (myPath - clickedPath).stride

        assertTrue(d != null && d > 0)
    }

    @Test
    fun highlighting_use_case_self() {
        val myPath = Trek.absolute(0, 1, 2)
        val clickedPath = Trek.absolute(0, 1, 2)

        val d = (myPath - clickedPath).stride

        assertEquals(0, d)
    }

    @Test
    fun highlighting_use_case_unrelated() {
        val myPath = Trek.absolute(0, 1, 2)
        val clickedPath = Trek.absolute(0, 1, 3)

        val d = (myPath - clickedPath).stride

        assertNull(d)
    }

    @Test
    fun delta_minus_delta_same_descendant_identity() {
        val d1 = Trek.descendant(2, 3)
        val d2 = Trek.descendant(2, 3)

        val result = d1 - d2

        assertTrue(result.isIdentity)
    }

    @Test
    fun delta_minus_delta_descendant_longer_minus_shorter() {
        val longer = Trek.descendant(2, 3, 4)
        val shorter = Trek.descendant(2)

        val result = longer - shorter

        assertEquals(2, result.stride)
        assertEquals(listOf(3, 4), result.trail)
    }

    @Test
    fun delta_minus_delta_descendant_shorter_minus_longer() {
        val longer = Trek.descendant(2, 3, 4)
        val shorter = Trek.descendant(2)

        val result = shorter - longer

        assertEquals(-2, result.stride)
        assertEquals(listOf(3, 4), result.trail)
    }

    @Test
    fun delta_minus_delta_descendant_divergent_yields_unrelated() {
        val d1 = Trek.descendant(2, 3)
        val d2 = Trek.descendant(2, 4)

        val result = d1 - d2

        assertTrue(result.isUnrelated)
    }

    @Test
    fun delta_minus_delta_same_ancestor_identity() {
        val d1 = Trek.ancestor(2, 3)
        val d2 = Trek.ancestor(2, 3)

        val result = d1 - d2

        assertTrue(result.isIdentity)
    }

    @Test
    fun delta_minus_delta_ancestor_longer_minus_shorter() {
        val longer = Trek.ancestor(2, 3, 4)
        val shorter = Trek.ancestor(2)

        val result = longer - shorter

        assertEquals(-2, result.stride)
        assertEquals(listOf(3, 4), result.trail)
    }

    @Test
    fun delta_minus_delta_mixed_signs_yields_unrelated() {
        val desc = Trek.descendant(2, 3)
        val anc = Trek.ancestor(2, 3)

        assertTrue((desc - anc).isUnrelated)
        assertTrue((anc - desc).isUnrelated)
    }

    @Test
    fun delta_plus_delta_descendant_concatenates() {
        val d1 = Trek.descendant(2, 3)
        val d2 = Trek.descendant(4)

        val result = d1 + d2

        assertEquals(3, result.stride)
        assertEquals(listOf(2, 3, 4), result.trail)
    }

    @Test
    fun delta_plus_delta_ancestor_combines() {
        val d1 = Trek.ancestor(3)
        val d2 = Trek.ancestor(2)

        val result = d1 + d2

        assertEquals(-2, result.stride)
        assertEquals(listOf(2, 3), result.trail)
    }

    @Test
    fun delta_plus_delta_descendant_then_ancestor_partial_undo() {
        val down = Trek.descendant(2, 3)
        val up = Trek.ancestor(3)

        val result = down + up

        assertEquals(1, result.stride)
        assertEquals(listOf(2), result.trail)
    }

    @Test
    fun delta_plus_delta_descendant_then_ancestor_full_undo() {
        val down = Trek.descendant(2, 3)
        val up = Trek.ancestor(2, 3)

        val result = down + up

        assertTrue(result.isIdentity)
    }

    @Test
    fun delta_plus_delta_descendant_then_ancestor_misaligned() {
        val down = Trek.descendant(2, 3)
        val up = Trek.ancestor(9)

        val result = down + up

        assertTrue(result.isUnrelated)
    }

    @Test
    fun delta_plus_delta_ancestor_then_descendant_retrace() {
        val up = Trek.ancestor(3)
        val down = Trek.descendant(3, 4)

        val result = up + down

        assertEquals(1, result.stride)
        assertEquals(listOf(4), result.trail)
    }

    @Test
    fun delta_plus_delta_ancestor_then_descendant_exact_retrace() {
        val up = Trek.ancestor(3)
        val down = Trek.descendant(3)

        val result = up + down

        assertTrue(result.isIdentity)
    }

    @Test
    fun delta_plus_delta_ancestor_then_descendant_misaligned() {
        val up = Trek.ancestor(3)
        val down = Trek.descendant(9, 9)

        val result = up + down

        assertTrue(result.isUnrelated)
    }

    @Test
    fun absolute_plus_absolute_yields_unrelated() {
        val a = Trek.absolute(0, 1)
        val b = Trek.absolute(2, 3)

        val result = a + b

        assertTrue(result.isUnrelated)
    }

    @Test
    fun delta_plus_absolute_yields_unrelated() {
        val delta = Trek.descendant(2, 3)
        val absolute = Trek.absolute(0, 1)

        val result = delta + absolute

        assertTrue(result.isUnrelated)
    }

    @Test
    fun algebra_closure_minus_never_throws() {
        val cases = listOf(
            Trek.Root,
            Trek.Unrelated,
            Trek.absolute(0, 1, 2),
            Trek.descendant(2, 3),
            Trek.ancestor(2, 3)
        )

        for (a in cases) {
            for (b in cases) {
                val result = a - b
                assertTrue(result.isRelated || result.isUnrelated)
            }
        }
    }

    @Test
    fun algebra_closure_plus_never_throws() {
        val cases = listOf(
            Trek.Root,
            Trek.Unrelated,
            Trek.absolute(0, 1, 2),
            Trek.descendant(2, 3),
            Trek.ancestor(2, 3)
        )

        for (a in cases) {
            for (b in cases) {
                val result = a + b
                assertTrue(result.isRelated || result.isUnrelated)
            }
        }
    }
}
