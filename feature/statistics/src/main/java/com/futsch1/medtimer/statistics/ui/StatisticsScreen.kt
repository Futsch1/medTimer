package com.futsch1.medtimer.statistics.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.designsystem.MedTimerTheme
import com.futsch1.medtimer.statistics.R
import com.futsch1.medtimer.statistics.domain.AnalysisDays
import com.futsch1.medtimer.statistics.domain.StatisticsTabType
import com.futsch1.medtimer.statistics.ui.calendar.CalendarContent
import com.futsch1.medtimer.statistics.ui.calendar.CalendarDayEvent
import com.futsch1.medtimer.statistics.ui.charts.ChartsContent
import com.futsch1.medtimer.statistics.ui.charts.DaysDropdown
import com.futsch1.medtimer.statistics.ui.preview.PreviewData
import com.futsch1.medtimer.statistics.ui.table.ReminderTable
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import java.time.LocalDate

@Composable
fun StatisticsScreen(
    viewModel: StatisticsScreenViewModel,
    onEditReminderEvent: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = viewModel.state
    val days = state.selectedDays.days
    val periodTitle = pluralStringResource(R.plurals.last_n_days, days, days)
    val totalTitle = stringResource(R.string.total)

    LaunchedEffect(days) {
        viewModel.loadChartData(days, periodTitle, totalTitle)
    }

    StatisticsScreen(
        selectedTab = state.selectedTab,
        onTabSelected = viewModel::selectTab,
        selectedDays = state.selectedDays,
        onDaysSelected = viewModel::selectDays,
        tableState = state,
        onFilterTextChanged = viewModel::updateFilterText,
        onEditReminderEvent = onEditReminderEvent,
        dayEvents = state.dayEvents,
        modifier = modifier,
    )
}

@Composable
fun StatisticsScreen(
    selectedTab: StatisticsTabType,
    onTabSelected: (StatisticsTabType) -> Unit,
    selectedDays: AnalysisDays,
    onDaysSelected: (AnalysisDays) -> Unit,
    tableState: StatisticsScreenState,
    onFilterTextChanged: (String) -> Unit,
    onEditReminderEvent: (Int) -> Unit,
    dayEvents: ImmutableMap<LocalDate, List<CalendarDayEvent>>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)
    ) {
        StatisticsHeaderRow(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            selectedDays = selectedDays,
            onDaysSelected = onDaysSelected,
        )

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tabContent",
        ) { tab ->
            when (tab) {
                StatisticsTabType.CHARTS -> ChartsContent(
                    medicinePerDayData = tableState.medicinePerDayData,
                    takenSkippedData = tableState.takenSkippedData,
                    takenSkippedTotalData = tableState.takenSkippedTotalData,
                )

                StatisticsTabType.TABLE -> TableContent(
                    tableState = tableState,
                    onFilterTextChanged = onFilterTextChanged,
                    onEditReminderEvent = onEditReminderEvent,
                )

                StatisticsTabType.CALENDAR -> CalendarContent(
                    dayEvents = dayEvents,
                )
            }
        }
    }
}


@Composable
private fun TableContent(
    tableState: StatisticsScreenState,
    onFilterTextChanged: (String) -> Unit,
    onEditReminderEvent: (Int) -> Unit,
) {
    ReminderTable(
        rows = tableState.tableRows,
        filterText = tableState.filterText,
        onFilterTextChanged = onFilterTextChanged,
        onEditEvent = onEditReminderEvent,
    )
}

@Composable
private fun StatisticsHeaderRow(
    selectedTab: StatisticsTabType,
    onTabSelected: (StatisticsTabType) -> Unit,
    selectedDays: AnalysisDays,
    onDaysSelected: (AnalysisDays) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilterChip(
                selected = selectedTab == StatisticsTabType.CHARTS,
                onClick = { onTabSelected(StatisticsTabType.CHARTS) },
                label = { Icon(Icons.Rounded.BarChart, contentDescription = null) },
                modifier = Modifier.testTag(StatisticsTestTags.CHART_CHIP),
            )
            FilterChip(
                selected = selectedTab == StatisticsTabType.TABLE,
                onClick = { onTabSelected(StatisticsTabType.TABLE) },
                label = { Icon(Icons.Rounded.TableChart, contentDescription = null) },
                modifier = Modifier.testTag(StatisticsTestTags.TABLE_CHIP),
            )
            FilterChip(
                selected = selectedTab == StatisticsTabType.CALENDAR,
                onClick = { onTabSelected(StatisticsTabType.CALENDAR) },
                label = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null) },
                modifier = Modifier.testTag(StatisticsTestTags.CALENDAR_CHIP),
            )
        }
        AnimatedVisibility(visible = selectedTab == StatisticsTabType.CHARTS) {
            DaysDropdown(
                selected = selectedDays,
                onSelected = onDaysSelected,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun StatisticsScreenEmptyPreview() {
    MedTimerTheme {
        Surface {
            StatisticsScreen(
                selectedTab = StatisticsTabType.CHARTS,
                onTabSelected = {},
                selectedDays = AnalysisDays.SEVEN_DAYS,
                onDaysSelected = {},
                tableState = PreviewData.emptyStatisticsScreenState,
                onFilterTextChanged = {},
                onEditReminderEvent = {},
                dayEvents = persistentMapOf(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun StatisticsScreenWithChartsPreview() {
    MedTimerTheme {
        Surface {
            StatisticsScreen(
                selectedTab = StatisticsTabType.CHARTS,
                onTabSelected = {},
                selectedDays = AnalysisDays.SEVEN_DAYS,
                onDaysSelected = {},
                tableState = PreviewData.chartsStatisticsScreenState,
                onFilterTextChanged = {},
                onEditReminderEvent = {},
                dayEvents = PreviewData.sampleDayEvents,
            )
        }
    }
}
