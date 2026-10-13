package com.futsch1.medtimer.feature.ui.statistics.levels

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.Dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.decoration.Decoration
import com.patrykandpatrick.vico.compose.common.component.TextComponent

/**
 * Labels each dose on its curve: a dot at the level when the dose was taken, connected by a short leader line to a label with the
 * amount. Labels are placed by [LabelPlacer] on every draw, so they adapt to scrolling and zooming, and avoid covering the [curves];
 * when they do not all fit, none are drawn.
 */
internal data class DoseLabelDecoration(
    private val markers: List<Marker>,
    private val curves: List<Curve>,
    private val dotSize: Dp,
    private val leaderLength: Dp,
    private val leaderThickness: Dp,
    private val gap: Dp,
    private val curveThickness: Dp,
) : Decoration {
    data class Marker(val x: Double, val y: Double, val text: String, val color: Color, val label: TextComponent)

    /** A drawn curve in chart values, with [xs] ascending. */
    class Curve(val xs: DoubleArray, val ys: DoubleArray)

    override fun drawOverLayers(context: CartesianDrawingContext) {
        with(context) {
            val visible = markers.mapNotNull { marker ->
                canvasX(marker.x).takeIf { isVisibleX(it) }?.let { Triple(marker, it, canvasY(marker.y)) }
            }
            if (visible.isEmpty() || visible.size > LabelPlacer.MAX_LABELS) return
            val anchors = visible.map { (marker, x, y) ->
                LabelAnchor(x, y, marker.label.getWidth(context, marker.text, pad = true), marker.label.getHeight(context, marker.text, pad = true))
            }
            val bounds = LabelBox(layerBounds.left, layerBounds.top, layerBounds.right, layerBounds.bottom)
            val boxes = LabelPlacer.place(
                anchors, bounds, leaderLength.pixels, gap.pixels, visiblePolylines(), curveClearance = curveThickness.pixels / 2 + gap.pixels,
            ) ?: return

            val paint = Paint().apply { strokeWidth = leaderThickness.pixels }
            val radius = dotSize.pixels / 2
            visible.zip(boxes).forEach { (visibleMarker, box) ->
                val (marker, x, y) = visibleMarker
                paint.color = marker.color
                // The leader ends at the nearest point of the label's edge facing the dot
                val leaderEnd = Offset(x.coerceIn(box.left, box.right), if (box.bottom <= y) box.bottom else box.top)
                canvas.drawLine(Offset(x, y), leaderEnd, paint)
                canvas.drawCircle(Offset(x, y), radius, paint)
                marker.label.draw(context, marker.text, (box.left + box.right) / 2, (box.top + box.bottom) / 2)
            }
        }
    }

    /** The visible part of each curve in pixels, plus one point beyond each edge so the edges are interpolated correctly. */
    private fun CartesianDrawingContext.visiblePolylines(): List<Polyline> {
        val visible = visibleXRange()
        return curves.map { curve ->
            val from = (curve.xs.firstIndexAtOrAfter(visible.start) - 1).coerceAtLeast(0)
            val to = (curve.xs.firstIndexAtOrAfter(visible.endInclusive) + 1).coerceAtMost(curve.xs.size)
            val points = (from until to).map { canvasX(curve.xs[it]) to canvasY(curve.ys[it]) }.sortedBy { it.first }
            Polyline(FloatArray(points.size) { points[it].first }, FloatArray(points.size) { points[it].second })
        }
    }

    private fun DoubleArray.firstIndexAtOrAfter(x: Double): Int {
        var low = 0
        var high = size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (this[mid] < x) low = mid + 1 else high = mid
        }
        return low
    }
}
