package com.futsch1.medtimer.feature.ui.statistics.levels

/** A label to place near the point ([x], [y]), with its measured size; all values in pixels. */
internal data class LabelAnchor(val x: Float, val y: Float, val width: Float, val height: Float)

internal data class LabelBox(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun overlaps(other: LabelBox, gap: Float): Boolean =
        left < other.right + gap && other.left < right + gap && top < other.bottom + gap && other.top < bottom + gap
}

/** A drawn curve in pixels, with [xs] ascending, that labels must not cover. */
internal class Polyline(val xs: FloatArray, val ys: FloatArray) {
    init {
        require(xs.size == ys.size)
    }

    /** Whether the curve passes through [box], grown by [gap] on every side. */
    fun crosses(box: LabelBox, gap: Float): Boolean {
        val left = box.left - gap
        val right = box.right + gap
        if (xs.isEmpty() || right < xs.first() || left > xs.last()) {
            return false
        }
        // The curve's lowest and highest point over the box's width, including where it enters and leaves the box
        var minY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        fun include(y: Float) {
            minY = minOf(minY, y)
            maxY = maxOf(maxY, y)
        }
        include(yAt(left.coerceIn(xs.first(), xs.last())))
        include(yAt(right.coerceIn(xs.first(), xs.last())))
        var i = firstIndexAtOrAfter(left)
        while (i < xs.size && xs[i] <= right) {
            include(ys[i])
            i++
        }
        return minY <= box.bottom + gap && maxY >= box.top - gap
    }

    private fun yAt(x: Float): Float {
        if (xs.size == 1) return ys[0]
        val i = firstIndexAtOrAfter(x).coerceIn(1, xs.size - 1)
        val x0 = xs[i - 1]
        val x1 = xs[i]
        return if (x1 == x0) ys[i] else ys[i - 1] + (ys[i] - ys[i - 1]) * (x - x0) / (x1 - x0)
    }

    private fun firstIndexAtOrAfter(x: Float): Int {
        var low = 0
        var high = xs.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (xs[mid] < x) low = mid + 1 else high = mid
        }
        return low
    }
}

/**
 * Places labels near their anchor points without overlapping each other, connected to their point by a short leader line. Labels
 * are tried below their point at increasing distances first, as the curve usually rises after a dose, then above, keeping each
 * horizontally centered on its point as far as the bounds allow. Anchors are placed from left to right, each taking the first spot
 * that also keeps clear of the drawn [Polyline]s, or if there is none, the first spot that at least does not overlap other labels.
 *
 * If any label cannot be placed without overlapping another, placement gives up and returns null rather than showing an arbitrary
 * subset: when the chart is too compressed for labels, none are shown.
 */
internal object LabelPlacer {
    const val MAX_LABELS = 60

    /**
     * @param leaderLength distance between a point and the nearest edge of its label in the first tier
     * @param gap minimum space between two labels, and between a label and the next tier
     * @param curves drawn curves that labels avoid covering where possible
     * @param curveClearance minimum space between a label and a curve's center line, so it clears the drawn stroke
     * @param tiers number of distances tried on each side of the point
     * @return one box per anchor, in the order of [anchors], or null if they cannot all be placed
     */
    fun place(
        anchors: List<LabelAnchor>,
        bounds: LabelBox,
        leaderLength: Float,
        gap: Float,
        curves: List<Polyline> = emptyList(),
        curveClearance: Float = gap,
        tiers: Int = 3,
    ): List<LabelBox>? {
        if (anchors.size > MAX_LABELS) {
            return null
        }
        val placed = arrayOfNulls<LabelBox>(anchors.size)
        val taken = mutableListOf<LabelBox>()
        for (index in anchors.indices.sortedBy { anchors[it].x }) {
            val free = candidates(anchors[index], bounds, leaderLength, gap, tiers).filter { candidate -> taken.none { it.overlaps(candidate, gap) } }
            val box = free.firstOrNull { candidate -> curves.none { it.crosses(candidate, curveClearance) } }
                ?: free.firstOrNull()
                ?: return null
            placed[index] = box
            taken.add(box)
        }
        return placed.map { it!! }
    }

    private fun candidates(anchor: LabelAnchor, bounds: LabelBox, leaderLength: Float, gap: Float, tiers: Int): Sequence<LabelBox> {
        if (anchor.width > bounds.right - bounds.left) {
            return emptySequence()
        }
        val left = (anchor.x - anchor.width / 2).coerceIn(bounds.left, bounds.right - anchor.width)
        val below = (0 until tiers).asSequence().map { tier ->
            val top = anchor.y + leaderLength + tier * (anchor.height + gap)
            LabelBox(left, top, left + anchor.width, top + anchor.height)
        }
        val above = (0 until tiers).asSequence().map { tier ->
            val bottom = anchor.y - leaderLength - tier * (anchor.height + gap)
            LabelBox(left, bottom - anchor.height, left + anchor.width, bottom)
        }
        return (below + above).filter { it.top >= bounds.top && it.bottom <= bounds.bottom }
    }
}
