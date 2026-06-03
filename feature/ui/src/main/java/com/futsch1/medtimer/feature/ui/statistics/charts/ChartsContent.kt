package com.futsch1.medtimer.feature.ui.statistics.charts

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass.Companion.BREAKPOINTS_V1
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.computeWindowSizeClass
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.feature.ui.statistics.ChartsState
import com.futsch1.medtimer.feature.ui.statistics.MedicineDaySeries
import com.futsch1.medtimer.feature.ui.statistics.MedicinePerDayData
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
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
import com.patrykandpatrick.vico.compose.pie.PieChart
import com.patrykandpatrick.vico.compose.pie.PieChartHost
import com.patrykandpatrick.vico.compose.pie.data.PieChartModel
import com.patrykandpatrick.vico.compose.pie.data.PieValueFormatter
import com.patrykandpatrick.vico.compose.pie.rememberPieChart
import kotlinx.collections.immutable.persistentListOf
import java.time.LocalDate
import kotlin.math.roundToInt

// Tag-independent: tag filtering applies to the Table only, not to Charts.
@Composable
fun ChartsContent(
    state: ChartsState,
    modifier: Modifier = Modifier,
    // Injectable (defaults to the live value) so layout tests supply a computed window size. The
    // detection below stays inlined per the "no shared helper" decision. Matches Now in Android.
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
) {
    val configuration = LocalConfiguration.current
    val isTabletLandscape = remember(windowAdaptiveInfo, configuration) {
        windowAdaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND) &&
            configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    // Single bar-chart definition shared by both arrangements; the caller supplies the scoped weight.
    val barChart: @Composable (Modifier) -> Unit = { barModifier ->
        MedicinePerDayBarChart(
            epochDays = state.perDay.epochDays,
            series = state.perDay.series.map { it.counts },
            seriesNames = state.perDay.series.map { it.medicineName },
            dayLabels = state.dayLabels,
            seriesColors = state.seriesColors,
            modifier = barModifier,
        )
    }

    ProvideVicoTheme(rememberM3VicoTheme()) {
        if (isTabletLandscape) {
            Row(
                modifier = modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                barChart(Modifier.fillMaxHeight().weight(2f))
                TwoPies(state = state, stacked = true, modifier = Modifier.fillMaxHeight().weight(1f))
            }
        } else {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                barChart(Modifier.fillMaxWidth().weight(2f))
                TwoPies(state = state, stacked = false, modifier = Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}

@Composable
private fun TwoPies(state: ChartsState, stacked: Boolean, modifier: Modifier = Modifier) {
    val periodTitle = pluralStringResource(R.plurals.last_n_days, state.days, state.days)
    val totalTitle = stringResource(R.string.total)
    if (stacked) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TakenSkippedPieChart(periodTitle, state.takenPeriod, state.skippedPeriod, Modifier.weight(1f))
            TakenSkippedPieChart(totalTitle, state.takenTotal, state.skippedTotal, Modifier.weight(1f))
        }
    } else {
        Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TakenSkippedPieChart(periodTitle, state.takenPeriod, state.skippedPeriod, Modifier.weight(1f))
            TakenSkippedPieChart(totalTitle, state.takenTotal, state.skippedTotal, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TakenSkippedPieChart(
    title: String,
    taken: Long,
    skipped: Long,
    modifier: Modifier = Modifier,
) {
    val takenColor = MaterialTheme.colorScheme.primary
    val skippedColor = MaterialTheme.colorScheme.primaryContainer
    val takenLabel = stringResource(R.string.taken)
    val skippedLabel = stringResource(R.string.skipped)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            if (taken + skipped <= 0L) {
                // Mirror the original widget: an all-zero series renders only an empty circle — no chart, no legend.
                EmptyPieCircle(modifier = Modifier.fillMaxWidth().weight(1f))
            } else {
                // Render from a synchronously-built model (not an async PieChartModelProducer): the producer
                // path leaves the pie blank on first composition until a later data change re-triggers it.
                val model = remember(taken, skipped) { PieChartModel.build(taken, skipped) }
                val takenLabelComponent = rememberTextComponent(TextStyle(color = MaterialTheme.colorScheme.onPrimary))
                val skippedLabelComponent = rememberTextComponent(TextStyle(color = MaterialTheme.colorScheme.onPrimaryContainer))
                val percentFormatter = remember {
                    PieValueFormatter { context, value, _ ->
                        if (context.model.sum == 0f || value == 0f) "" else "${(value / context.model.sum * 100).roundToInt()}%"
                    }
                }
                val chart = rememberPieChart(
                    sliceProvider = PieChart.SliceProvider.series(
                        PieChart.Slice(fill = Fill(takenColor), label = PieChart.SliceLabel.Inside(takenLabelComponent)),
                        PieChart.Slice(fill = Fill(skippedColor), label = PieChart.SliceLabel.Inside(skippedLabelComponent)),
                    ),
                    valueFormatter = percentFormatter,
                )
                PieChartHost(chart, model, modifier = Modifier.fillMaxWidth().weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                ) {
                    LegendDot(takenColor, takenLabel)
                    LegendDot(skippedColor, skippedLabel)
                }
            }
        }
    }
}

@Composable
private fun EmptyPieCircle(modifier: Modifier = Modifier) {
    val circleColor = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(circleColor),
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@MedTimerPreview
@Composable
private fun ChartsContentPreview() {
    MedTimerTheme {
        Surface {
            ChartsContent(
                state = ChartsState(
                    perDay = MedicinePerDayData(
                        epochDays = listOf(20200L, 20201L, 20202L),
                        series = listOf(
                            MedicineDaySeries("Vitamin X 500 mg", listOf(1, 2, 1)),
                            MedicineDaySeries("Medicine A", listOf(0, 1, 2)),
                        ),
                    ),
                    dayLabels = persistentListOf("May 26", "May 27", "May 28"),
                    seriesColors = persistentListOf(0xFF003F5C.toInt(), 0xFFFF7C43.toInt()),
                    takenPeriod = 7,
                    skippedPeriod = 3,
                    takenTotal = 42,
                    skippedTotal = 8,
                    days = 7,
                ),
            )
        }
    }
}

@Preview(name = "Charts — tablet landscape", widthDp = 900, heightDp = 480)
@Composable
private fun ChartsContentLandscapePreview() {
    // A wide @Preview canvas alone drives neither currentWindowAdaptiveInfo() nor orientation, so the
    // tablet-landscape branch is forced explicitly: a medium-width window size class + a landscape config.
    val landscapeConfiguration = Configuration(LocalConfiguration.current).apply {
        orientation = Configuration.ORIENTATION_LANDSCAPE
    }
    MedTimerTheme {
        Surface {
            CompositionLocalProvider(LocalConfiguration provides landscapeConfiguration) {
                ChartsContent(
                    state = ChartsState(
                        perDay = MedicinePerDayData(
                            epochDays = listOf(20200L, 20201L, 20202L),
                            series = listOf(
                                MedicineDaySeries("Vitamin X 500 mg", listOf(1, 2, 1)),
                                MedicineDaySeries("Medicine A", listOf(0, 1, 2)),
                            ),
                        ),
                        dayLabels = persistentListOf("May 26", "May 27", "May 28"),
                        seriesColors = persistentListOf(0xFF003F5C.toInt(), 0xFFFF7C43.toInt()),
                        takenPeriod = 7,
                        skippedPeriod = 3,
                        takenTotal = 42,
                        skippedTotal = 8,
                        days = 7,
                    ),
                    windowAdaptiveInfo = WindowAdaptiveInfo(
                        windowSizeClass = BREAKPOINTS_V1.computeWindowSizeClass(widthDp = 900f, heightDp = 480f),
                        windowPosture = Posture(),
                    ),
                )
            }
        }
    }
}

@Composable
private fun MedicinePerDayBarChart(
    epochDays: List<Long>,
    series: List<List<Int>>,
    seriesNames: List<String>,
    dayLabels: List<String>,
    seriesColors: List<Int>,
    modifier: Modifier = Modifier,
) {
    if (series.isEmpty()) {
        Card(
            modifier = modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {}
        return
    }

    val resolvedColors = remember(seriesColors) { seriesColors.map { Color(it) } }
    val modelProducer = remember { CartesianChartModelProducer() }
    // Plot against the real epoch-day x-values so labels can be derived from the x-value itself.
    LaunchedEffect(epochDays, series) {
        modelProducer.runTransaction { columnSeries { series.forEach { series(x = epochDays, y = it) } } }
    }

    // Vico 3.x throws if the formatter ever returns a blank string, so the fallback must be non-blank.
    val labelByEpoch = remember(epochDays, dayLabels) { epochDays.zip(dayLabels).toMap() }
    val bottomAxisValueFormatter = remember(labelByEpoch) {
        CartesianValueFormatter { _, value, _ ->
            labelByEpoch[value.toLong()] ?: LocalDate.ofEpochDay(value.toLong()).toString()
        }
    }
    val legendLabelComponent = rememberTextComponent(TextStyle(vicoTheme.textColor))

    val chart = rememberCartesianChart(
        rememberColumnCartesianLayer(
            columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                resolvedColors.map { color -> rememberLineComponent(fill = Fill(color), thickness = 16.dp) },
            ),
            mergeMode = { ColumnCartesianLayer.MergeMode.Stacked },
        ),
        startAxis = VerticalAxis.rememberStart(
            valueFormatter = remember { CartesianValueFormatter.decimal(decimalCount = 0) },
            itemPlacer = remember { VerticalAxis.ItemPlacer.step(step = { 1.0 }) },
            guideline = null,
        ),
        bottomAxis = HorizontalAxis.rememberBottom(
            valueFormatter = bottomAxisValueFormatter,
            itemPlacer = remember { HorizontalAxis.ItemPlacer.aligned(spacing = { 2 }) },
            guideline = null,
        ),
        legend = rememberHorizontalLegend(
            items = {
                seriesNames.forEachIndexed { index, name ->
                    add(
                        LegendItem(
                            icon = ShapeComponent(Fill(resolvedColors[index % resolvedColors.size]), CircleShape),
                            labelComponent = legendLabelComponent,
                            label = name,
                        )
                    )
                }
            },
            padding = Insets(top = 8.dp),
        ),
    )
    val zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = remember { Zoom.Content })
    val hostModifier = Modifier.fillMaxSize().padding(16.dp)

    Card(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        if (LocalInspectionMode.current) {
            // @Preview renders don't run the LaunchedEffect that fills the producer, so the bars would
            // be blank. Build the model synchronously here; production keeps the producer for its
            // difference animations.
            val previewModel = remember(epochDays, series) {
                CartesianChartModel(
                    ColumnCartesianLayerModel.build { series.forEach { series(x = epochDays, y = it) } },
                )
            }
            CartesianChartHost(chart = chart, model = previewModel, modifier = hostModifier, zoomState = zoomState)
        } else {
            CartesianChartHost(chart = chart, modelProducer = modelProducer, modifier = hostModifier, zoomState = zoomState)
        }
    }
}
