package com.futsch1.medtimer.feature.ui.statistics

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.futsch1.medtimer.core.domain.model.StatisticFragment
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.feature.ui.statistics.calendar.CalendarContent
import com.futsch1.medtimer.feature.ui.statistics.charts.ChartsContent
import com.futsch1.medtimer.feature.ui.statistics.table.ReminderTable

/** Stateful entry point: binds the ViewModel to the stateless [StatisticsScreen]. */
@Composable
fun StatisticsScreen(
    viewModel: StatisticsScreenViewModel,
    onEditEvent: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatisticsScreen(
        state = state,
        onSelectView = viewModel::onSelectView,
        onSelectRange = viewModel::onSelectRange,
        onEditEvent = onEditEvent,
        modifier = modifier,
    )
}

/** Stateless screen — the `@Preview`/test target. Renders purely from its inputs. */
@Composable
fun StatisticsScreen(
    state: StatisticsUiState,
    onSelectView: (StatisticFragment) -> Unit,
    onSelectRange: (Int) -> Unit,
    onEditEvent: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ViewChip(R.drawable.ic_bar_chart, R.string.analysis, state.activeView == StatisticFragment.CHARTS) {
                    onSelectView(StatisticFragment.CHARTS)
                }
                ViewChip(R.drawable.ic_table_chart, R.string.tabular_view, state.activeView == StatisticFragment.TABLE) {
                    onSelectView(StatisticFragment.TABLE)
                }
                ViewChip(R.drawable.ic_calendar_month, R.string.calendar, state.activeView == StatisticFragment.CALENDAR) {
                    onSelectView(StatisticFragment.CALENDAR)
                }
            }

            // The Analysis range drives the Charts only — show the control only there (matches the legacy app).
            AnimatedVisibility(visible = state.activeView == StatisticFragment.CHARTS) {
                RangeDropdown(days = state.analysisDays, onSelectRange = onSelectRange)
            }
        }

        AnimatedContent(
            targetState = state.activeView,
            transitionSpec = {
                // Slide in the direction of travel between tabs, with a cross-fade.
                val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                (slideInHorizontally { width -> direction * width } + fadeIn()) togetherWith
                        (slideOutHorizontally { width -> -direction * width } + fadeOut())
            },
            label = "tabContent",
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp, 4.dp, 16.dp, 16.dp),
        ) { view ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (view) {
                    StatisticFragment.CHARTS -> state.charts?.let { ChartsContent(it) }
                    StatisticFragment.TABLE -> ReminderTable(rows = state.tableRows, onEditEvent = onEditEvent)
                    StatisticFragment.CALENDAR -> CalendarContent(dayEvents = state.calendarDayEvents)
                }
            }
        }
    }
}

@Composable
private fun ViewChip(iconRes: Int, labelRes: Int, selected: Boolean, onClick: () -> Unit) {
    // FilterChip switches its container/content colors instantly; animate them for a smooth selection.
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        label = "chipContainerColor",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "chipContentColor",
    )
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Icon(painterResource(iconRes), contentDescription = stringResource(labelRes), tint = contentColor) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = containerColor,
            selectedContainerColor = containerColor,
            labelColor = contentColor,
            selectedLabelColor = contentColor,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangeDropdown(days: Int, onSelectRange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val labels = stringArrayResource(R.array.analysis_days)
    var expanded by remember { mutableStateOf(false) }
    val selectedIndex = ANALYSIS_DAYS_VALUES.indexOf(days).coerceAtLeast(0)
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "rangeArrowRotation")

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        FilterChip(
            selected = true,
            onClick = {},
            label = { Text(labels.getOrElse(selectedIndex) { "" }) },
            trailingIcon = {
                Icon(painterResource(R.drawable.ic_arrow_drop_down), contentDescription = null, modifier = Modifier.rotate(rotation))
            },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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

// mirrors R.array.analysis_days_values — must stay in sync with that resource array
private val ANALYSIS_DAYS_VALUES = intArrayOf(1, 2, 3, 7, 14, 30)

@MedTimerPreview
@Composable
private fun StatisticsScreenPreview() {
    val state = StatisticsUiState(activeView = StatisticFragment.TABLE)
    MedTimerTheme {
        StatisticsScreen(
            state = state,
            onSelectView = {},
            onSelectRange = {},
            onEditEvent = {},
        )
    }
}
