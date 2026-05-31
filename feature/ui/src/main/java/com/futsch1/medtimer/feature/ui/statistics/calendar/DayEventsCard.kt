package com.futsch1.medtimer.feature.ui.statistics.calendar

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
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.ui.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun DayEventsCard(
    date: LocalDate,
    events: List<CalendarDayEvent>,
    modifier: Modifier = Modifier,
) {
    val timeFormatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    val dateTimeFormatter = remember { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT) }
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = date.format(dateFormatter),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            if (events.isEmpty()) {
                Text(text = stringResource(R.string.no_reminders), style = MaterialTheme.typography.bodySmall)
                return@Column
            }
            val skippedLabel = stringResource(R.string.skipped)
            val raisedLabel = stringResource(R.string.raised)
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
                    formattedTimes.maxOfOrNull { time ->
                        textMeasurer.measure(time, bodySmall).size.width
                    }?.toDp() ?: 0.dp
                }
            }
            events.forEachIndexed { index, event ->
                val suffix = when (event.status) {
                    CalendarDayEvent.Status.SKIPPED -> " ($skippedLabel)"
                    CalendarDayEvent.Status.RAISED -> " ($raisedLabel)"
                    else -> ""
                }
                Row {
                    Text(
                        text = formattedTimes[index],
                        style = bodySmall,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(maxTimeWidth),
                    )
                    Text(text = ": ${event.amount} ${event.medicineName}$suffix", style = bodySmall)
                }
            }
        }
    }
}
