package com.futsch1.medtimer.feature.ui.statistics

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.domain.model.StatisticFragment
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.feature.ui.statistics.calendar.CalendarContent
import com.futsch1.medtimer.feature.ui.statistics.charts.ChartsContent
import com.futsch1.medtimer.feature.ui.statistics.table.ReminderTable

object StatisticsTestTags {
    const val RANGE_DROPDOWN = "statistics_range_dropdown"

    /** Scopes the range entries; each is then selected by its own label. */
    const val RANGE_MENU = "statistics_range_menu"
    const val TABLE_FILTER = "statistics_table_filter"

    fun viewChip(view: StatisticFragment) = "statistics_view_${view.name}"
}

/** Stateful entry point: binds the ViewModel to the stateless [StatisticsScreen]. */
@Composable
fun StatisticsScreen(
    viewModel: StatisticsScreenViewModel,
    onEditEvent: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The view-model's state is Compose snapshot state — read it directly, no collectAsState needed.
    StatisticsScreen(
        state = viewModel.state,
        onSelectView = viewModel::onSelectView,
        onSelectRange = viewModel::onSelectRange,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onEditEvent = onEditEvent,
        modifier = modifier,
    )
}

/** Stateless screen — the `@Preview`/test target. Renders purely from its inputs. */
@Composable
fun StatisticsScreen(
    state: StatisticsScreenState,
    onSelectView: (StatisticFragment) -> Unit,
    onSelectRange: (Int) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onEditEvent: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // FilterChip's clickable Surface reserves a 48dp minimum interactive size, making the header taller than the
        // 32dp chip pills. Disabling the reservation here is layout-only — the system still expands the touch target at
        // the input layer — so the row matches the chip height.
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ViewChip(R.drawable.bar_chart, R.string.analysis, StatisticFragment.CHARTS, state.activeView) {
                        onSelectView(StatisticFragment.CHARTS)
                    }
                    ViewChip(R.drawable.table, R.string.tabular_view, StatisticFragment.TABLE, state.activeView) {
                        onSelectView(StatisticFragment.TABLE)
                    }
                    ViewChip(R.drawable.calendar3, R.string.calendar, StatisticFragment.CALENDAR, state.activeView) {
                        onSelectView(StatisticFragment.CALENDAR)
                    }
                }

                // The Analysis range drives the Charts only — show the control only there (matches the legacy app).
                AnimatedVisibility(visible = state.activeView == StatisticFragment.CHARTS) {
                    RangeDropdown(days = state.analysisDays, onSelectRange = onSelectRange)
                }
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
                    StatisticFragment.TABLE -> ReminderTable(
                        rows = state.filteredRows,
                        query = state.query,
                        onQueryChange = onSearchQueryChange,
                        onEditEvent = onEditEvent,
                    )

                    StatisticFragment.CALENDAR -> CalendarContent(dayEvents = state.calendarDayEvents)
                }
            }
        }
    }
}

@Composable
private fun ViewChip(iconRes: Int, labelRes: Int, view: StatisticFragment, activeView: StatisticFragment, onClick: () -> Unit) {
    val selected = view == activeView
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
        modifier = Modifier.testTag(StatisticsTestTags.viewChip(view)),
        label = { Icon(painterResource(iconRes), contentDescription = stringResource(labelRes), tint = contentColor) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = containerColor,
            selectedContainerColor = containerColor,
            labelColor = contentColor,
            selectedLabelColor = contentColor,
        ),
    )
}

@MedTimerPreview
@Composable
private fun StatisticsScreenPreview() {
    val state = MutableStatisticsScreenState().apply { activeView = StatisticFragment.TABLE }
    MedTimerTheme {
        Surface {
            StatisticsScreen(
                state = state,
                onSelectView = {},
                onSelectRange = {},
                onSearchQueryChange = {},
                onEditEvent = {},
            )
        }
    }
}
