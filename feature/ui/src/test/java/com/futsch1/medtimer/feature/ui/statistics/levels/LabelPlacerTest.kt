package com.futsch1.medtimer.feature.ui.statistics.levels

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LabelPlacerTest {

    private val bounds = LabelBox(0f, 0f, 400f, 200f)

    private fun place(vararg anchors: LabelAnchor, curves: List<Polyline> = emptyList()) =
        LabelPlacer.place(anchors.toList(), bounds, leaderLength = 10f, gap = 2f, curves = curves)

    private fun polyline(vararg points: Pair<Float, Float>) =
        Polyline(points.map { it.first }.toFloatArray(), points.map { it.second }.toFloatArray())

    private fun assertNoOverlaps(boxes: List<LabelBox>) {
        for (i in boxes.indices) for (j in i + 1 until boxes.size) {
            assertTrue(!boxes[i].overlaps(boxes[j], 0f), "Labels $i and $j overlap: ${boxes[i]} / ${boxes[j]}")
        }
    }

    @Test
    fun `a single label sits centered below its point`() {
        val box = assertNotNull(place(LabelAnchor(100f, 100f, 40f, 12f))).single()

        assertEquals(LabelBox(80f, 110f, 120f, 122f), box)
    }

    @Test
    fun `nearby labels are stacked into free tiers without overlapping`() {
        val boxes = assertNotNull(place(LabelAnchor(100f, 100f, 40f, 12f), LabelAnchor(110f, 100f, 40f, 12f), LabelAnchor(120f, 100f, 40f, 12f)))

        assertNoOverlaps(boxes)
        assertEquals(listOf(110f, 124f, 138f), boxes.map { it.top })
    }

    @Test
    fun `labels near the bottom edge go above their point`() {
        val box = assertNotNull(place(LabelAnchor(100f, 195f, 40f, 12f))).single()

        assertEquals(185f, box.bottom)
    }

    @Test
    fun `labels are kept inside the chart horizontally`() {
        val box = assertNotNull(place(LabelAnchor(5f, 100f, 40f, 12f))).single()

        assertEquals(0f, box.left)
    }

    @Test
    fun `labels avoid covering a curve`() {
        // A second curve drops steeply just below the point, through all three tiers below it
        val curve = polyline(0f to 105f, 80f to 105f, 120f to 155f, 400f to 155f)
        val box = assertNotNull(place(LabelAnchor(100f, 100f, 40f, 12f), curves = listOf(curve))).single()

        assertEquals(90f, box.bottom)
        assertFalse(curve.crosses(box, 2f))
    }

    @Test
    fun `a dose at the low point of its own curve keeps its label below`() {
        // Decaying into the dose from the left, rising after it on the right; in pixels, down is larger y
        val ownCurve = polyline(0f to 20f, 100f to 100f, 130f to 40f, 400f to 60f)
        val box = assertNotNull(place(LabelAnchor(100f, 100f, 40f, 12f), curves = listOf(ownCurve))).single()

        assertEquals(110f, box.top)
    }

    @Test
    fun `curves crossing the box between its sample points are detected`() {
        val steep = polyline(0f to 0f, 400f to 200f)

        assertTrue(steep.crosses(LabelBox(180f, 80f, 220f, 92f), 0f))
        assertFalse(steep.crosses(LabelBox(180f, 150f, 220f, 162f), 0f))
        assertFalse(polyline(500f to 100f).crosses(LabelBox(180f, 80f, 220f, 120f), 0f))
        assertTrue(polyline(200f to 100f).crosses(LabelBox(180f, 80f, 220f, 120f), 0f))
    }

    @Test
    fun `placement gives up when the labels do not all fit`() {
        // Seven labels at the same spot, but only three tiers below and three above
        val crowded = (0 until 7).map { LabelAnchor(100f, 100f, 40f, 12f) }

        assertNull(LabelPlacer.place(crowded, bounds, leaderLength = 10f, gap = 2f))
    }

    @Test
    fun `labels cover a curve rather than being hidden when curves leave no room`() {
        val zigzag = polyline(*(0..40).map { it * 10f to if (it % 2 == 0) 0f else 200f }.toTypedArray())
        val boxes = assertNotNull(place(LabelAnchor(100f, 100f, 40f, 12f), LabelAnchor(110f, 100f, 40f, 12f), curves = listOf(zigzag)))

        // The preferred spots below the point, still without overlapping each other
        assertEquals(listOf(110f, 124f), boxes.map { it.top })
        assertNoOverlaps(boxes)
    }

    @Test
    fun `labels still prefer a spot clear of curves over a closer one`() {
        val curve = polyline(0f to 105f, 80f to 105f, 120f to 155f, 400f to 155f)
        val zigzagElsewhere = polyline(300f to 0f, 310f to 200f)
        val box = assertNotNull(place(LabelAnchor(100f, 100f, 40f, 12f), curves = listOf(curve, zigzagElsewhere))).single()

        assertEquals(90f, box.bottom)
    }

    @Test
    fun `placement gives up with too many labels`() {
        val many = (0..LabelPlacer.MAX_LABELS).map { LabelAnchor(it * 1000f, 100f, 1f, 1f) }

        assertNull(LabelPlacer.place(many, LabelBox(0f, 0f, 1e6f, 200f), leaderLength = 10f, gap = 2f))
    }

    @Test
    fun `boxes are returned in the order of the anchors`() {
        val boxes = assertNotNull(place(LabelAnchor(300f, 100f, 40f, 12f), LabelAnchor(100f, 100f, 40f, 12f)))

        assertEquals(listOf(280f, 80f), boxes.map { it.left })
    }
}
