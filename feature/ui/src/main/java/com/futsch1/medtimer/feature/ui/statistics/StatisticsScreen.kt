package com.futsch1.medtimer.feature.ui.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.domain.model.StatisticFragment
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.feature.ui.statistics.calendar.CalendarContent
import com.futsch1.medtimer.feature.ui.statistics.charts.ChartsContent
import com.futsch1.medtimer.feature.ui.statistics.table.ReminderTable
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import java.time.LocalDate

/** Stateful entry point: binds the ViewModels to the stateless [StatisticsScreen]. */
@Composable
fun StatisticsScreen(
    viewModel: StatisticsScreenViewModel,
    calendarViewModel: CalendarEventsViewModel,
    onEditEvent: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val calendarDayEvents by produceState<ImmutableMap<LocalDate, CharSequence>>(persistentMapOf(), calendarViewModel) {
        calendarViewModel.getEventForMonths(ALL_MEDICINES, PAST_MONTHS, FUTURE_MONTHS).collect { map ->
            value = map.entries.associate { (date, text) -> date to (text as CharSequence) }.toImmutableMap()
        }
    }
    StatisticsScreen(
        state = viewModel.state,
        calendarDayEvents = calendarDayEvents,
        onSelectView = viewModel::onSelectView,
        onSelectRange = viewModel::onSelectRange,
        onEditEvent = onEditEvent,
        modifier = modifier,
    )
}

/** Stateless screen — the `@Preview`/test target. Renders purely from its inputs. */
@Composable
fun StatisticsScreen(
    state: StatisticsScreenState,
    calendarDayEvents: Map<LocalDate, CharSequence>,
    onSelectView: (StatisticFragment) -> Unit,
    onSelectRange: (Int) -> Unit,
    onEditEvent: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ViewChip(R.string.analysis, state.activeView == StatisticFragment.CHARTS) { onSelectView(StatisticFragment.CHARTS) }
            ViewChip(R.string.tabular_view, state.activeView == StatisticFragment.TABLE) { onSelectView(StatisticFragment.TABLE) }
            ViewChip(R.string.calendar, state.activeView == StatisticFragment.CALENDAR) { onSelectView(StatisticFragment.CALENDAR) }

            // The Analysis range drives the Charts only — show the control only there (matches the legacy app).
            if (state.activeView == StatisticFragment.CHARTS) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    RangeDropdown(days = state.analysisDays, onSelectRange = onSelectRange)
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (state.activeView) {
                StatisticFragment.CHARTS -> state.charts?.let { ChartsContent(it) }
                StatisticFragment.TABLE -> ReminderTable(rows = state.tableRows, onEditEvent = onEditEvent)
                StatisticFragment.CALENDAR -> CalendarContent(dayEvents = calendarDayEvents)
            }
        }
    }
}

@Composable
private fun ViewChip(labelRes: Int, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(stringResource(labelRes)) })
}

@Composable
private fun RangeDropdown(days: Int, onSelectRange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val labels = stringArrayResource(R.array.analysis_days)
    var expanded by remember { mutableStateOf(false) }
    val selectedIndex = ANALYSIS_DAYS_VALUES.indexOf(days).coerceAtLeast(0)

    Box(modifier = modifier) {
        TextButton(onClick = { expanded = true }) { Text(labels.getOrElse(selectedIndex) { "" }) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            labels.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        onSelectRange(ANALYSIS_DAYS_VALUES[index])
                    },
                )
            }
        }
    }
}

private const val ALL_MEDICINES = -1
private const val PAST_MONTHS = 3
private const val FUTURE_MONTHS = 0
// mirrors R.array.analysis_days_values — must stay in sync with that resource array
private val ANALYSIS_DAYS_VALUES = intArrayOf(1, 2, 3, 7, 14, 30)

@MedTimerPreview
@Composable
private fun StatisticsScreenPreview() {
    val state = MutableStatisticsScreenState().apply {
        activeView = StatisticFragment.TABLE
        tableRows = persistentListOf()
    }
    MedTimerTheme {
        StatisticsScreen(
            state = state,
            calendarDayEvents = persistentMapOf(),
            onSelectView = {},
            onSelectRange = {},
            onEditEvent = {},
        )
    }
}
