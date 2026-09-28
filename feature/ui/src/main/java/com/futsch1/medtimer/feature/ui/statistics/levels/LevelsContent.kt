package com.futsch1.medtimer.feature.ui.statistics.levels

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.feature.ui.statistics.StatisticsTestTags
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Scroll
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.LegendItem
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.rememberHorizontalLegend
import com.patrykandpatrick.vico.compose.common.vicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.floor
import kotlin.math.roundToInt

/** Estimated relative level of each medicine with half-life and time to peak set over the Analysis range. */
@Composable
fun LevelsContent(
    state: LevelsState,
    modifier: Modifier = Modifier,
    zoomResetKey: Int = 0,
    onCustomZoomChange: (Boolean) -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            if (state.series.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    if (state.unitConflicts.isEmpty()) {
                        Text(
                            text = stringResource(R.string.pk_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        UnitConflictWarnings(state.unitConflicts, MaterialTheme.typography.bodyMedium, TextAlign.Center)
                    }
                }
            } else {
                ProvideVicoTheme(rememberM3VicoTheme()) {
                    LevelsChart(state, zoomResetKey, onCustomZoomChange, Modifier.fillMaxSize().padding(16.dp))
                }
            }
        }
        if (state.series.isNotEmpty()) {
            UnitConflictWarnings(state.unitConflicts, MaterialTheme.typography.bodySmall, TextAlign.Start)
        }
        if (state.series.isNotEmpty() && state.ignoredDoses > 0) {
            Text(
                text = stringResource(R.string.pk_ignored_doses, state.ignoredDoses),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            text = stringResource(R.string.pk_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** One warning per medicine left out of the estimate because its doses use different units, listing each unit with its dose count. */
@Composable
private fun UnitConflictWarnings(conflicts: List<UnitConflict>, style: TextStyle, textAlign: TextAlign) {
    val noUnit = stringResource(R.string.pk_no_unit)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        conflicts.forEach { conflict ->
            val units = conflict.units.joinToString(", ") { "${it.unit.ifEmpty { noUnit }} (${it.count})" }
            Text(
                text = stringResource(R.string.pk_unit_conflict, conflict.medicineName, units),
                style = style,
                color = MaterialTheme.colorScheme.error,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LevelsChart(
    state: LevelsState,
    zoomResetKey: Int,
    onCustomZoomChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The shared Charts palette starts with dark blues that vanish as thin lines on a dark card, so medicines without a custom
    // color cycle through the theme's line colors (primary, secondary, tertiary), which contrast in both light and dark mode.
    val themeColors = vicoTheme.lineCartesianLayerColors
    val colors = remember(state.series, themeColors) {
        var fallbackIndex = 0
        state.series.map { series -> series.color?.let { Color(it) } ?: themeColors[fallbackIndex++ % themeColors.size] }
    }
    // Two lines per medicine, in the same order as the model's series: solid up to now, dashed for the projection
    val lines = colors.flatMap { color ->
        val fill = remember(color) { LineCartesianLayer.LineFill.single(Fill(color)) }
        listOf(
            LineCartesianLayer.rememberLine(fill = fill, stroke = remember { LineCartesianLayer.LineStroke.Continuous(thickness = LINE_THICKNESS) }),
            LineCartesianLayer.rememberLine(fill = fill, stroke = remember { LineCartesianLayer.LineStroke.Dashed(thickness = LINE_THICKNESS) }),
        )
    }
    val rangeProvider = remember(state.windowHours) {
        CartesianLayerRangeProvider.fixed(minX = 0.0, maxX = state.windowHours, minY = 0.0, maxY = MAX_PERCENT)
    }
    // Vico 3.x throws if the formatter ever returns a blank string, so the fallback must be non-blank.
    val bottomAxisValueFormatter = remember(state.midnightLabels) {
        CartesianValueFormatter { _, value, _ ->
            val day = floor(value / LevelsState.HOURS_PER_DAY + 0.5).toInt()
            state.midnightLabels.getOrNull(day)?.takeIf { it.isNotBlank() } ?: day.toString()
        }
    }
    val legendLabelComponent = rememberTextComponent(TextStyle(vicoTheme.textColor))
    // Marks the current time, where the solid history turns into the dashed projection, with a dot on each medicine's curve.
    // The dots get a ring in the card color so they stand out where curves cross.
    val nowLine = rememberLineComponent(fill = Fill(vicoTheme.lineColor), thickness = NOW_LINE_THICKNESS)
    val cardColor = MaterialTheme.colorScheme.surfaceContainer
    // Dose labels in each medicine's color, on a card-colored background so they stay readable over the curves
    val labelStyle = MaterialTheme.typography.labelSmall
    val doseLabels = colors.map { color ->
        rememberTextComponent(
            style = labelStyle.copy(color = color),
            padding = remember { Insets(horizontal = DOSE_LABEL_PADDING_H, vertical = DOSE_LABEL_PADDING_V) },
            background = remember(color, cardColor) {
                ShapeComponent(Fill(cardColor), RoundedCornerShape(DOSE_LABEL_CORNER), strokeFill = Fill(color), strokeThickness = DOSE_LINE_THICKNESS)
            },
        )
    }
    val decorations = remember(state.series, state.nowHours, nowLine, colors, cardColor, doseLabels) {
        val points = state.series.mapIndexedNotNull { index, series ->
            series.past.percent.lastOrNull()?.let { y ->
                NowMarkerDecoration.Point(y, ShapeComponent(Fill(colors[index]), CircleShape, strokeFill = Fill(cardColor), strokeThickness = NOW_POINT_RING))
            }
        }
        val doseMarkers = state.series.flatMapIndexed { index, series ->
            series.doses.map { DoseLabelDecoration.Marker(it.hours, it.percent, it.label, colors[index], doseLabels[index]) }
        }
        val curves = state.series.flatMap { listOf(it.past, it.projection) }
            .map { DoseLabelDecoration.Curve(it.hours.toDoubleArray(), it.percent.toDoubleArray()) }
        listOf(
            NowMarkerDecoration(state.nowHours, nowLine, points, NOW_POINT_SIZE),
            DoseLabelDecoration(doseMarkers, curves, DOSE_DOT_SIZE, DOSE_LEADER_LENGTH, DOSE_LINE_THICKNESS, DOSE_LABEL_GAP, LINE_THICKNESS),
        )
    }

    val chart = rememberCartesianChart(
        rememberLineCartesianLayer(
            lineProvider = LineCartesianLayer.LineProvider.series(lines),
            rangeProvider = rangeProvider,
        ),
        // No y-axis: each medicine is normalized to its own peak, so a shared scale would suggest a comparison that does not exist
        bottomAxis = HorizontalAxis.rememberBottom(
            valueFormatter = bottomAxisValueFormatter,
            // Labels sit on midnights; Vico skips some of them when they would overlap, so they also adapt to pinch-zooming
            itemPlacer = remember { HorizontalAxis.ItemPlacer.aligned() },
            guideline = null,
        ),
        legend = rememberHorizontalLegend(
            items = {
                state.series.forEachIndexed { index, series ->
                    add(
                        LegendItem(
                            icon = ShapeComponent(Fill(colors[index]), CircleShape),
                            labelComponent = legendLabelComponent,
                            label = series.medicineName,
                        )
                    )
                }
            },
            padding = Insets(top = 8.dp),
        ),
        decorations = decorations,
        // Samples are irregular in time; one x step per day keeps the axis labels on local midnights.
        getXStep = { _, _, _ -> LevelsState.HOURS_PER_DAY },
    )
    val model = remember(state.series) {
        CartesianChartModel(
            LineCartesianLayerModel.build {
                state.series.forEach {
                    series(x = it.past.hours, y = it.past.percent)
                    series(x = it.projection.hours, y = it.projection.percent)
                }
            },
        )
    }
    // The range dropdown sets how much is visible at once; drag to scroll back through the history and pinch to pick a custom span.
    // Picking a range (the reset key) restores its span and jumps back to today; so does crossing midnight for the scroll position.
    val zoomState = rememberVicoZoomState(
        zoomEnabled = true,
        initialZoom = remember(state.visibleHours, zoomResetKey) { Zoom.x(state.visibleHours) },
        maxZoom = remember { Zoom.x(MIN_VISIBLE_HOURS) },
    )
    val scrollState = rememberVicoScrollState(
        scrollEnabled = true,
        initialScroll = remember(state.visibleHours, state.windowHours, zoomResetKey) { Scroll.Absolute.x(state.windowHours, bias = 1f) },
    )

    // Vico does not tell a pinch apart from its own initial zoom, so watch for two-finger movement without consuming it
    var customZoom by rememberSaveable(state.visibleHours, zoomResetKey) { mutableStateOf(false) }
    val currentOnCustomZoomChange by rememberUpdatedState(onCustomZoomChange)
    LaunchedEffect(customZoom) { currentOnCustomZoomChange(customZoom) }
    DisposableEffect(Unit) { onDispose { currentOnCustomZoomChange(false) } }
    val pinchModifier = Modifier.pointerInput(state.visibleHours, zoomResetKey) {
        awaitEachGesture {
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.count { it.pressed } >= 2 && event.changes.any { it.positionChanged() }) {
                    customZoom = true
                }
            } while (event.changes.any { it.pressed })
        }
    }

    CartesianChartHost(chart = chart, model = model, modifier = modifier.testTag(StatisticsTestTags.LEVELS_CHART).then(pinchModifier), scrollState = scrollState, zoomState = zoomState)
}

private val LINE_THICKNESS = 3.dp
private val NOW_LINE_THICKNESS = 1.dp
private val NOW_POINT_SIZE = 10.dp
private val NOW_POINT_RING = 2.dp
private val DOSE_DOT_SIZE = 5.dp
private val DOSE_LEADER_LENGTH = 10.dp
private val DOSE_LINE_THICKNESS = 1.dp
private val DOSE_LABEL_GAP = 2.dp
private val DOSE_LABEL_CORNER = 4.dp
private val DOSE_LABEL_PADDING_H = 4.dp
private val DOSE_LABEL_PADDING_V = 1.dp
private const val MAX_PERCENT = 100.0
private const val MIN_VISIBLE_HOURS = 6.0

@MedTimerPreview
@Composable
private fun LevelsContentPreview() {
    // Synthetic weekly injection: absorbs over ~2 days, decays with a 5-day half-life; now is at noon of day 7.
    fun curve(hours: List<Double>) = LevelCurve(
        hours = hours.toImmutableList(),
        percent = hours.map { h -> (100.0 * (1 - kotlin.math.exp(-h / 16.0)) * kotlin.math.exp(-h / 173.0)).roundToInt().toDouble() }
            .toImmutableList(),
    )
    val state = LevelsState(
        series = persistentListOf(
            LevelSeries("Medicine A", null, curve((0..156 step 2).map { it.toDouble() }), curve((156..216 step 2).map { it.toDouble() })),
        ),
        days = 7,
        historyDays = 7,
        projectionDays = 2,
        ignoredDoses = 0,
        unitConflicts = persistentListOf(),
        nowHours = 156.0,
        midnightLabels = (0..9).map { "Day $it" }.toImmutableList(),
    )
    MedTimerTheme {
        Surface {
            LevelsContent(state = state, modifier = Modifier.padding(16.dp))
        }
    }
}
