package com.futsch1.medtimer.feature.ui.statistics.calendar

import android.widget.TextView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.util.LocalePreferences
import com.futsch1.medtimer.feature.ui.R
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
// AndroidView wraps a TextView because Compose Text cannot render the Spanned medicine-icon formatting.
@Composable
fun CalendarContent(
    dayEvents: Map<LocalDate, CharSequence>,
    modifier: Modifier = Modifier,
) {
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(3) }
    val firstDayOfWeek = remember {
        if (LocalePreferences.getFirstDayOfWeek() == LocalePreferences.FirstDayOfWeek.SUNDAY) {
            DayOfWeek.SUNDAY
        } else {
            DayOfWeek.MONDAY
        }
    }
    val orderedDaysOfWeek = remember(firstDayOfWeek) { daysOfWeek(firstDayOfWeek) }

    var selectedEpochDay by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = currentMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek,
    )

    Column(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        HorizontalCalendar(
            state = state,
            dayContent = { day ->
                DayCell(
                    day = day,
                    isSelected = day.date.toEpochDay() == selectedEpochDay,
                    hasEvents = dayEvents[day.date]?.isNotEmpty() == true,
                    onClick = { selectedEpochDay = day.date.toEpochDay() },
                )
            },
            monthHeader = { month -> MonthHeader(month, orderedDaysOfWeek) },
            modifier = Modifier.fillMaxWidth(),
        )

        val detail = dayEvents[selectedDate]
        AndroidView(
            factory = { context -> TextView(context).also { it.id = R.id.currentDayEvents } },
            update = { it.text = detail ?: "" },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
    }
}

@Composable
private fun DayCell(
    day: CalendarDay,
    isSelected: Boolean,
    hasEvents: Boolean,
    onClick: () -> Unit,
) {
    val isInMonth = day.position == DayPosition.MonthDate
    val color = when {
        !isInMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(enabled = isInMonth, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            color = color,
            fontWeight = if (hasEvents) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun MonthHeader(month: CalendarMonth, orderedDaysOfWeek: List<DayOfWeek>) {
    val locale = LocalConfiguration.current.locales[0]
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = month.yearMonth.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            orderedDaysOfWeek.forEach { dayOfWeek ->
                Text(
                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
