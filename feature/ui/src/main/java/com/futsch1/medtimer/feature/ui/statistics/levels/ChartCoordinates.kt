package com.futsch1.medtimer.feature.ui.statistics.levels

import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext

/**
 * Maps chart values to canvas pixels the same way Vico's line layer places its points, so custom decorations follow scrolling and
 * zooming. Vico does not expose this mapping itself.
 */
internal fun CartesianDrawingContext.canvasX(x: Double): Float {
    val start = if (isLtr) layerBounds.left else layerBounds.right
    return start + layoutDirectionMultiplier * layerDimensions.startPadding - scroll +
            layoutDirectionMultiplier * layerDimensions.xSpacing * ((x - ranges.minX) / ranges.xStep).toFloat()
}

/** The inverse of [canvasX]: the chart x value drawn at [canvasX]. */
internal fun CartesianDrawingContext.chartX(canvasX: Float): Double {
    val start = if (isLtr) layerBounds.left else layerBounds.right
    val offset = (canvasX - start + scroll) / layoutDirectionMultiplier - layerDimensions.startPadding
    return ranges.minX + offset / layerDimensions.xSpacing * ranges.xStep
}

/** The chart x range currently visible in the plot area. */
internal fun CartesianDrawingContext.visibleXRange(): ClosedFloatingPointRange<Double> {
    val a = chartX(layerBounds.left)
    val b = chartX(layerBounds.right)
    return minOf(a, b)..maxOf(a, b)
}

internal fun CartesianDrawingContext.canvasY(y: Double): Float {
    val yRange = ranges.getYRange(null)
    return layerBounds.bottom - ((y - yRange.minY) / yRange.length).toFloat() * layerBounds.height
}

internal fun CartesianDrawingContext.isVisibleX(canvasX: Float): Boolean = canvasX in layerBounds.left..layerBounds.right
