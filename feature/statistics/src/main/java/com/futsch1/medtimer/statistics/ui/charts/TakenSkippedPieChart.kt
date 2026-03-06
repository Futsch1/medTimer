package com.futsch1.medtimer.statistics.ui.charts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.designsystem.MedTimerTheme
import com.futsch1.medtimer.core.ui.PieChart
import com.futsch1.medtimer.core.ui.PieSegment
import com.futsch1.medtimer.statistics.R
import com.futsch1.medtimer.statistics.model.TakenSkippedData
import com.futsch1.medtimer.statistics.ui.preview.PreviewData
import kotlinx.collections.immutable.persistentListOf
import java.util.Locale

@Composable
fun TakenSkippedPieChart(
    data: TakenSkippedData?,
    modifier: Modifier = Modifier
) {
    if (data == null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PieChart(
                    segments = persistentListOf(),
                    labelStyle = MaterialTheme.typography.labelSmall,
                    showLegend = false
                )
            }
        }
        return
    }

    val takenLabel = stringResource(R.string.taken)
    val skippedLabel = stringResource(R.string.skipped)
    val labelStyle = MaterialTheme.typography.labelSmall

    val segments = if (!data.isEmpty) {
        persistentListOf(
            PieSegment(
                value = data.taken.toFloat(),
                color = MaterialTheme.colorScheme.primary,
                label = if (data.taken > 0) String.format(
                    Locale.US,
                    "%d%%",
                    data.takenPercent
                ) else "",
                labelColor = MaterialTheme.colorScheme.onPrimary,
                legendLabel = takenLabel
            ),
            PieSegment(
                value = data.skipped.toFloat(),
                color = MaterialTheme.colorScheme.primaryContainer,
                label = if (data.skipped > 0) String.format(
                    Locale.US,
                    "%d%%",
                    data.skippedPercent
                ) else "",
                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                legendLabel = skippedLabel
            )
        )
    } else {
        persistentListOf()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = data.title },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = data.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            PieChart(
                segments = segments,
                labelStyle = labelStyle,
                showLegend = true
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TakenSkippedPieChartPreview() {
    MedTimerTheme {
        Surface {
            TakenSkippedPieChart(
                data = PreviewData.sampleTakenSkippedData,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TakenSkippedPieChartAllTakenPreview() {
    MedTimerTheme {
        Surface {
            TakenSkippedPieChart(
                data = PreviewData.sampleTakenSkippedTotalData,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TakenSkippedPieChartEmptyPreview() {
    MedTimerTheme {
        Surface {
            TakenSkippedPieChart(
                data = TakenSkippedData(0, 0, "7 days"),
            )
        }
    }
}
