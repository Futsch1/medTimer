package com.futsch1.medtimer.feature.ui.statistics.calendar

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass.Companion.BREAKPOINTS_V1
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.computeWindowSizeClass
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.OutDateStyle
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.core.nextMonth
import com.kizitonwose.calendar.core.previousMonth
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle

@Composable
fun CalendarContent(
    dayEvents: Map<LocalDate, List<CalendarDayEvent>>,
    modifier: Modifier = Modifier,
    pastMonths: Int = 3,
    futureMonths: Int = 0,
    // Injectable (defaults to the live value) so layout tests supply a computed window size. The
    // detection below stays inlined per the "no shared helper" decision. Matches ChartsContent.
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
) {
    // Pre-select today so its events show on open, matching the legacy calendar's default day.
    var selectedDate by rememberSaveable { mutableStateOf<LocalDate?>(LocalDate.now()) }
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

    val configuration = LocalConfiguration.current
    val isTabletLandscape = remember(windowAdaptiveInfo, configuration) {
        windowAdaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND) &&
            configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    // The calendar Card; the caller supplies the scoped weight (landscape) or default Modifier (portrait).
    val calendarCard: @Composable (Modifier) -> Unit = { cardModifier ->
        Card(
            modifier = cardModifier,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                CalendarNavigationRow(
                    yearMonth = visibleMonth,
                    startMonth = startMonth,
                    endMonth = endMonth,
                    onPrev = {
                        coroutineScope.launch { calendarState.animateScrollToMonth(visibleMonth.previousMonth) }
                    },
                    onNext = {
                        coroutineScope.launch { calendarState.animateScrollToMonth(visibleMonth.nextMonth) }
                    },
                    onYearSelected = { year ->
                        val target = YearMonth.of(year, visibleMonth.monthValue).coerceIn(startMonth, endMonth)
                        coroutineScope.launch { calendarState.animateScrollToMonth(target) }
                    },
                )

                HorizontalCalendar(
                    state = calendarState,
                    dayContent = { day ->
                        val hasEvents = dayEvents[day.date]?.isNotEmpty() == true
                        DayCell(
                            day = day,
                            isSelected = day.date == selectedDate,
                            hasEvents = hasEvents,
                            onClick = { selectedDate = day.date },
                        )
                    },
                    monthHeader = { WeekDaysRow(firstDayOfWeek = firstDayOfWeek) },
                    modifier = Modifier
                        .widthIn(max = 400.dp)
                        .align(Alignment.CenterHorizontally),
                )
            }
        }
    }

    // The day-events panel; the caller supplies the scoped weight (landscape) or default Modifier (portrait).
    val eventPanel: @Composable (Modifier) -> Unit = { panelModifier ->
        AnimatedContent(
            targetState = selectedDate,
            transitionSpec = {
                (fadeIn(tween(300)) + expandVertically(tween(300)))
                    .togetherWith(fadeOut(tween(300)) + shrinkVertically(tween(300)))
                    .using(SizeTransform(clip = false))
            },
            label = "dayEventsCard",
            modifier = panelModifier,
        ) { date ->
            if (date == null) return@AnimatedContent
            DayEventsCard(date = date, events = dayEvents[date].orEmpty())
        }
    }

    if (isTabletLandscape) {
        Row(modifier = modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            calendarCard(Modifier.weight(2f))
            eventPanel(Modifier.weight(1f))
        }
    } else {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            calendarCard(Modifier)
            eventPanel(Modifier)
        }
    }
}

@Composable
private fun WeekDaysRow(firstDayOfWeek: DayOfWeek) {
    val locale = LocalConfiguration.current.locales[0]
    val weekDays = remember(firstDayOfWeek) { daysOfWeek(firstDayOfWeek = firstDayOfWeek) }
    Row(modifier = Modifier.fillMaxWidth()) {
        weekDays.forEach { dayOfWeek ->
            Text(
                modifier = Modifier.weight(1f),
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@MedTimerPreview
@Composable
private fun CalendarContentPreview() {
    val today = LocalDate.now()
    MedTimerTheme {
        Surface {
            CalendarContent(
                dayEvents = mapOf(
                    today to listOf(
                        CalendarDayEvent(today.atTime(8, 0), "1 tablet", "Vitamin X 500 mg", CalendarDayEvent.Status.TAKEN),
                    ),
                    today.minusDays(2) to listOf(
                        CalendarDayEvent(today.minusDays(2).atTime(20, 0), "2 ml", "Medicine A", CalendarDayEvent.Status.SKIPPED),
                    ),
                ),
            )
        }
    }
}

@Preview(name = "Calendar — tablet landscape", widthDp = 900, heightDp = 480)
@Composable
private fun CalendarContentLandscapePreview() {
    // A wide @Preview canvas alone drives neither currentWindowAdaptiveInfo() nor orientation, so the
    // tablet-landscape branch is forced explicitly: a medium-width window size class + a landscape config.
    val landscapeConfiguration = Configuration(LocalConfiguration.current).apply {
        orientation = Configuration.ORIENTATION_LANDSCAPE
    }
    val today = LocalDate.now()
    MedTimerTheme {
        Surface {
            CompositionLocalProvider(LocalConfiguration provides landscapeConfiguration) {
                CalendarContent(
                    dayEvents = mapOf(
                        today to listOf(
                            CalendarDayEvent(today.atTime(8, 0), "1 tablet", "Vitamin X 500 mg", CalendarDayEvent.Status.TAKEN),
                        ),
                        today.minusDays(2) to listOf(
                            CalendarDayEvent(today.minusDays(2).atTime(20, 0), "2 ml", "Medicine A", CalendarDayEvent.Status.SKIPPED),
                        ),
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
