package com.futsch1.medtimer.statistics.ui.charts

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
import com.futsch1.medtimer.core.designsystem.MedTimerTheme
import com.futsch1.medtimer.statistics.model.MedicinePerDayData
import com.futsch1.medtimer.statistics.ui.preview.PreviewData
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
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


@Composable
fun MedicinePerDayBarChart(
    data: MedicinePerDayData?,
    modifier: Modifier = Modifier,
) {
    if (data == null || data.series.isEmpty()) {
        Card(
            modifier = modifier
                .fillMaxSize(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {}
        return
    }

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
        data.series.map { it.color }
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

    val bottomAxisValueFormatter = remember(dateFormat) {
        CartesianValueFormatter { _, value, _ ->
            LocalDate.ofEpochDay(value.toLong()).format(dateFormat)
        }
    }

    val legendItemLabelComponent = rememberTextComponent(TextStyle(vicoTheme.textColor))

    Card(
        modifier = modifier
            .fillMaxSize()
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
            zoomState = rememberVicoZoomState(
                zoomEnabled = false,
                initialZoom = remember { Zoom.Content },
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun MedicinePerDayBarChartPreview() {
    MedTimerTheme {
        Surface {
            MedicinePerDayBarChart(
                data = PreviewData.sampleMedicinePerDayData,
            )
        }
    }
}
