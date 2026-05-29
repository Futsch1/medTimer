package com.futsch1.medtimer.feature.ui.statistics.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.feature.ui.statistics.ChartsState
import com.futsch1.medtimer.feature.ui.statistics.MedicinePerDayData
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.pie.PieChart
import com.patrykandpatrick.vico.compose.pie.PieChartHost
import com.patrykandpatrick.vico.compose.pie.data.PieChartModelProducer
import com.patrykandpatrick.vico.compose.pie.data.pieSeries
import com.patrykandpatrick.vico.compose.pie.rememberPieChart

// Tag-independent: tag filtering applies to the Table only, not to Charts.
@Composable
fun ChartsContent(state: ChartsState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TakenSkippedPieChart(
                title = pluralStringResource(R.plurals.last_n_days, state.days, state.days),
                taken = state.takenPeriod,
                skipped = state.skippedPeriod,
                modifier = Modifier.weight(1f),
            )
            TakenSkippedPieChart(
                title = stringResource(R.string.total),
                taken = state.takenTotal,
                skipped = state.skippedTotal,
                modifier = Modifier.weight(1f),
            )
        }

        MedicinePerDayBarChart(
            data = state.perDay,
            dayLabels = state.dayLabels,
            seriesColors = state.seriesColors,
            modifier = Modifier.fillMaxWidth().height(280.dp),
        )
    }
}

@Composable
private fun TakenSkippedPieChart(
    title: String,
    taken: Long,
    skipped: Long,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
        if (taken + skipped <= 0L) {
            Text(text = "—", style = MaterialTheme.typography.bodyMedium)
            return@Column
        }

        val takenColor = MaterialTheme.colorScheme.primary
        val skippedColor = MaterialTheme.colorScheme.secondary
        val modelProducer = remember { PieChartModelProducer() }
        LaunchedEffect(taken, skipped) {
            modelProducer.runTransaction { pieSeries { series(taken, skipped) } }
        }
        val chart = rememberPieChart(
            sliceProvider = PieChart.SliceProvider.series(
                listOf(
                    PieChart.Slice(fill = Fill(takenColor)),
                    PieChart.Slice(fill = Fill(skippedColor)),
                ),
            ),
        )
        PieChartHost(chart, modelProducer, modifier = Modifier.fillMaxWidth().height(160.dp))
    }
}

@Composable
private fun MedicinePerDayBarChart(
    data: MedicinePerDayData,
    dayLabels: List<String>,
    seriesColors: List<Int>,
    modifier: Modifier = Modifier,
) {
    if (data.series.isEmpty()) {
        Text(text = "—", style = MaterialTheme.typography.bodyMedium, modifier = modifier)
        return
    }

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(data) {
        modelProducer.runTransaction {
            columnSeries { data.series.forEach { series(it.counts) } }
        }
    }

    // Build one colored column per medicine series (composable calls in a plain loop).
    val columns = ArrayList<LineComponent>(data.series.size)
    for (index in data.series.indices) {
        columns += rememberLineComponent(fill = Fill(Color(seriesColors.getOrElse(index) { 0 })))
    }
    val columnProvider = remember(columns) { ColumnCartesianLayer.ColumnProvider.series(columns) }

    CartesianChartHost(
        rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = columnProvider,
                mergeMode = { ColumnCartesianLayer.MergeMode.Stacked },
            ),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = CartesianValueFormatter { _, value, _ ->
                    dayLabels.getOrElse(value.toInt()) { "" }
                },
            ),
        ),
        modelProducer,
        modifier = modifier,
    )
}
