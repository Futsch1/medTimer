package com.futsch1.medtimer.statistics.ui.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.designsystem.MedTimerTheme
import com.futsch1.medtimer.statistics.R
import com.futsch1.medtimer.statistics.ui.preview.PreviewData
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


@Composable
fun DayEventsCard(
    date: LocalDate,
    events: List<CalendarDayEvent>,
) {
    val timeFormatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    val dateTimeFormatter = remember { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT) }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = date.format(
                    java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
                ),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            val skippedLabel = stringResource(R.string.skipped)
            val bodySmall = MaterialTheme.typography.bodySmall
            val textMeasurer = rememberTextMeasurer()
            val density = LocalDensity.current
            val formattedTimes = remember(events, date) {
                events.map { event ->
                    val formatter = if (event.time.toLocalDate() == date) timeFormatter else dateTimeFormatter
                    event.time.format(formatter)
                }
            }
            val maxTimeWidth = remember(formattedTimes, bodySmall) {
                with(density) {
                    formattedTimes.maxOf { time ->
                        textMeasurer.measure(time, bodySmall).size.width
                    }.toDp()
                }
            }
            events.forEachIndexed { index, event ->
                val suffix = when (event.status) {
                    CalendarDayEvent.Status.SKIPPED -> " ($skippedLabel)"
                    CalendarDayEvent.Status.RAISED -> " (?)"
                    else -> ""
                }
                Row {
                    Text(
                        text = formattedTimes[index],
                        style = bodySmall,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(maxTimeWidth),
                    )
                    Text(
                        text = ": ${event.amount} ${event.medicineName}$suffix",
                        style = bodySmall,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun DayEventsCardPreview() {
    MedTimerTheme {
        DayEventsCard(
            date = PreviewData.baseDate,
            events = listOf(
                PreviewData.aspirinTaken,
                PreviewData.ibuprofenSkipped,
                PreviewData.vitaminDRaised,
                PreviewData.melatoninScheduled,
            ),
        )
    }
}