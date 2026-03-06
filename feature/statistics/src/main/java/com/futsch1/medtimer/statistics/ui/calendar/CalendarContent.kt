package com.futsch1.medtimer.statistics.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.designsystem.MedTimerTheme
import com.futsch1.medtimer.statistics.ui.preview.PreviewData
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.OutDateStyle
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.core.nextMonth
import com.kizitonwose.calendar.core.previousMonth
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarContent(
    viewModel: CalendarEventsViewModel,
    modifier: Modifier = Modifier,
    medicineId: Int = -1,
    pastMonths: Int = 3,
    futureMonths: Int = 0,
) {
    LaunchedEffect(medicineId, pastMonths, futureMonths) {
        viewModel.getEventForMonths(medicineId, pastMonths, futureMonths)
    }
    val dayEvents = viewModel.state.dayEvents

    CalendarContent(
        dayEvents = dayEvents,
        pastMonths = pastMonths,
        futureMonths = futureMonths,
        modifier = modifier,
    )
}

@Composable
fun CalendarContent(
    dayEvents: ImmutableMap<LocalDate, List<CalendarDayEvent>>,
    modifier: Modifier = Modifier,
    pastMonths: Int = 3,
    futureMonths: Int = 0,
) {
    var selectedDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    val startMonth = remember { YearMonth.now().minusMonths(pastMonths.toLong()) }
    val endMonth = remember { YearMonth.now().plusMonths(futureMonths.toLong()) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }

    val calendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = YearMonth.now(),
        firstDayOfWeek = firstDayOfWeek,
        outDateStyle = OutDateStyle.EndOfGrid,
    )
    val coroutineScope = rememberCoroutineScope()

    val visibleMonth = calendarState.firstVisibleMonth.yearMonth

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                CalendarNavigationRow(
                    yearMonth = visibleMonth,
                    startMonth = startMonth,
                    endMonth = endMonth,
                    onPrev = {
                        coroutineScope.launch {
                            calendarState.animateScrollToMonth(visibleMonth.previousMonth)
                        }
                    },
                    onNext = {
                        coroutineScope.launch {
                            calendarState.animateScrollToMonth(visibleMonth.nextMonth)
                        }
                    },
                    onYearSelected = { year ->
                        val target = YearMonth.of(year, visibleMonth.monthValue)
                            .coerceIn(startMonth, endMonth)
                        coroutineScope.launch {
                            calendarState.animateScrollToMonth(target)
                        }
                    },
                )

                HorizontalCalendar(
                    state = calendarState,
                    dayContent = { day ->
                        val hasEvents = dayEvents[day.date]?.isNotEmpty() == true
                        DayCell(
                            day = day,
                            isSelected = day.date == selectedDate && hasEvents,
                            hasEvents = hasEvents,
                            onClick = {
                                if (hasEvents) selectedDate = day.date
                            },
                        )
                    },
                    monthHeader = { _ ->
                        WeekDaysRow(firstDayOfWeek = firstDayOfWeek)
                    },
                    modifier = Modifier
                        .widthIn(max = 400.dp)
                        .align(Alignment.CenterHorizontally),
                )
            }
        }

        val selectedDayEvents = selectedDate?.let { dayEvents[it] }
        AnimatedContent(
            targetState = if (!selectedDayEvents.isNullOrEmpty()) selectedDate to selectedDayEvents else null,
            transitionSpec = {
                (fadeIn(tween(300)) + expandVertically(tween(300)))
                    .togetherWith(fadeOut(tween(300)) + shrinkVertically(tween(300)))
                    .using(SizeTransform(clip = false))
            },
            label = "dayEventsCard",
        ) { state ->
            val date = state?.first ?: return@AnimatedContent
            DayEventsCard(
                date = date,
                events = state.second,
            )
        }
    }
}

@Composable
private fun WeekDaysRow(firstDayOfWeek: DayOfWeek) {
    val weekDays = remember(firstDayOfWeek) { daysOfWeek(firstDayOfWeek = firstDayOfWeek) }
    Row(modifier = Modifier.fillMaxWidth()) {
        weekDays.forEach { dayOfWeek ->
            Text(
                modifier = Modifier.weight(1f),
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun CalendarContentEmptyPreview() {
    MedTimerTheme {
        CalendarContent(
            dayEvents = persistentMapOf(),
        )
    }
}

@PreviewLightDark
@Composable
private fun CalendarContentWithEventsPreview() {
    MedTimerTheme {
        CalendarContent(
            dayEvents = PreviewData.sampleDayEvents,
        )
    }
}
