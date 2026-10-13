package com.futsch1.medtimer.feature.ui.statistics.levels

import androidx.compose.ui.unit.Dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.decoration.Decoration
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent

/**
 * Marks the current time: a vertical [line] across the plot area at [x] under the chart's lines, and a [Point] on each curve where
 * it crosses that line. Vico only ships a horizontal line decoration.
 */
internal data class NowMarkerDecoration(
    private val x: Double,
    private val line: LineComponent,
    private val points: List<Point>,
    private val pointSize: Dp,
) : Decoration {
    data class Point(val y: Double, val component: ShapeComponent)

    override fun drawUnderLayers(context: CartesianDrawingContext) {
        with(context) {
            val canvasX = canvasX(x).takeIf { isVisibleX(it) } ?: return
            line.drawVertical(context, canvasX, layerBounds.top, layerBounds.bottom)
        }
    }

    override fun drawOverLayers(context: CartesianDrawingContext) {
        with(context) {
            val canvasX = canvasX(x).takeIf { isVisibleX(it) } ?: return
            val radius = pointSize.pixels / 2
            for (point in points) {
                val canvasY = canvasY(point.y)
                point.component.draw(context, canvasX - radius, canvasY - radius, canvasX + radius, canvasY + radius)
            }
        }
    }
}
