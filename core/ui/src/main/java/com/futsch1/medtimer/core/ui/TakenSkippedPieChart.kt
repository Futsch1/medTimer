package com.futsch1.medtimer.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import com.futsch1.medtimer.core.designsystem.MedTimerTheme
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
import kotlinx.collections.immutable.persistentListOf
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun TakenSkippedPieChart(
    data: TakenSkippedData,
    modifier: Modifier = Modifier
) {
    val total = data.taken + data.skipped
    val takenLabel = stringResource(R.string.taken)
    val skippedLabel = stringResource(R.string.skipped)
    val labelStyle = MaterialTheme.typography.labelSmall

    val segments = if (total > 0L) {
        val takenPercent = (100f * data.taken / total).roundToInt()
        val skippedPercent = 100 - takenPercent
        persistentListOf(
            PieSegment(
                value = data.taken.toFloat(),
                color = MaterialTheme.colorScheme.primary,
                label = if (data.taken > 0) String.format(
                    Locale.US,
                    "%d%%",
                    takenPercent
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
                    skippedPercent
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
                data = TakenSkippedData(
                    taken = 7,
                    skipped = 3,
                    title = "Last 7 days",
                )
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
                data = TakenSkippedData(
                    taken = 10,
                    skipped = 0,
                    title = "Total",
                )
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
                data = TakenSkippedData(
                    taken = 0,
                    skipped = 0,
                    title = "Last 7 days",
                )
            )
        }
    }
}
