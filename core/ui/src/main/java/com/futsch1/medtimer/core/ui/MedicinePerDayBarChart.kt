package com.futsch1.medtimer.core.ui

import com.futsch1.medtimer.core.designsystem.MedTimerTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
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
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import com.patrykandpatrick.vico.compose.common.rememberHorizontalLegend
import com.patrykandpatrick.vico.compose.common.vicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val LegendLabelKey = ExtraStore.Key<List<String>>()
private val LegendColorKey = ExtraStore.Key<List<Color>>()

private val FALLBACK_COLORS = listOf(
    Color(0xFF003f5c),
    Color(0xFF2f4b7c),
    Color(0xFF665191),
    Color(0xFFa05195),
    Color(0xFFd45087),
    Color(0xFFf95d6a),
    Color(0xFFff7c43),
    Color(0xFFffa600),
    Color(0xFF004c6d),
    Color(0xFF295d7d),
    Color(0xFF436f8e),
    Color(0xFF5b829f),
    Color(0xFF7295b0),
    Color(0xFF89a8c2),
    Color(0xFFa1bcd4),
    Color(0xFFb8d0e6),
    Color(0xFFd0e5f8),
)

@Composable
fun MedicinePerDayBarChart(
    data: MedicinePerDayData,
    modifier: Modifier = Modifier,
) {
    if (data.series.isEmpty()) return

    ProvideVicoTheme(rememberM3VicoTheme()) {
        MedicinePerDayBarChartContent(data, modifier)
    }
}

@Composable
private fun MedicinePerDayBarChartContent(
    data: MedicinePerDayData,
    modifier: Modifier = Modifier,
) {
    val resolvedColors = remember(data.series) {
        data.series.mapIndexed { index, series ->
            series.color ?: FALLBACK_COLORS[index % FALLBACK_COLORS.size]
        }
    }

    val dayEpochs = remember(data.days) { data.days.map { it.toEpochDay() } }
    val dateFormat = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT) }
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(data) {
        modelProducer.runTransaction {
            columnSeries {
                data.series.forEach { seriesData ->
                    series(x = dayEpochs, y = seriesData.values)
                }
            }
            extras { extraStore ->
                extraStore[LegendLabelKey] = data.series.map { it.name }
                extraStore[LegendColorKey] = resolvedColors
            }
        }
    }

    val bottomAxisValueFormatter = CartesianValueFormatter { _, value, _ ->
        LocalDate.ofEpochDay(value.toLong()).format(dateFormat)
    }

    val legendItemLabelComponent = rememberTextComponent(TextStyle(vicoTheme.textColor))

    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .semantics { contentDescription = data.title },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(
                    columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                        resolvedColors.map { color ->
                            rememberLineComponent(fill = Fill(color), thickness = 16.dp)
                        }
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
                    items = { extraStore ->
                        val labels = extraStore[LegendLabelKey]
                        val colors = extraStore[LegendColorKey]
                        labels.forEachIndexed { index, label ->
                            add(
                                LegendItem(
                                    icon = ShapeComponent(
                                        Fill(colors[index % colors.size]),
                                        CircleShape,
                                    ),
                                    labelComponent = legendItemLabelComponent,
                                    label = label,
                                )
                            )
                        }
                    },
                    padding = Insets(top = 8.dp),
                ),
            ),
            modelProducer = modelProducer,
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            zoomState = rememberVicoZoomState(zoomEnabled = false),
        )
    }
}

@PreviewLightDark
@Composable
private fun MedicinePerDayBarChartPreview() {
    MedTimerTheme {
        Surface {
            MedicinePerDayBarChart(
                data = MedicinePerDayData(
                    title = "Last 7 days",
                    days = listOf(
                        LocalDate.of(2023, 12, 1),
                        LocalDate.of(2023, 12, 2),
                        LocalDate.of(2023, 12, 3),
                        LocalDate.of(2023, 12, 4),
                        LocalDate.of(2023, 12, 5),
                    ),
                    series = listOf(
                        MedicineSeriesData("Aspirin", listOf(1, 2, 1, 0, 2), null),
                        MedicineSeriesData("Ibuprofen", listOf(0, 1, 1, 1, 0), null),
                    ),
                ),
            )
        }
    }
}
