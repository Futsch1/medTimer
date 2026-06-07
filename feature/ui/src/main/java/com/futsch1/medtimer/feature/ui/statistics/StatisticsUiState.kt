package com.futsch1.medtimer.feature.ui.statistics

import androidx.compose.runtime.Immutable
import com.futsch1.medtimer.core.domain.model.StatisticFragment
import com.futsch1.medtimer.core.ui.component.SortableTableRow
import com.futsch1.medtimer.feature.ui.statistics.calendar.CalendarDayEvent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import java.time.LocalDate

/**
 * The render-ready snapshot the Statistics screen collects from [StatisticsScreenViewModel.uiState].
 * A single immutable value derived from the view-model's flows — the screen reads it via
 * collectAsStateWithLifecycle, so the flow-to-snapshot conversion lives only at the Compose edge.
 */
@Immutable
data class StatisticsUiState(
    val activeView: StatisticFragment = StatisticFragment.CHARTS,
    val analysisDays: Int = DEFAULT_ANALYSIS_DAYS,
    val charts: ChartsState? = null,
    val tableRows: ImmutableList<SortableTableRow> = persistentListOf(),
    val calendarDayEvents: ImmutableMap<LocalDate, List<CalendarDayEvent>> = persistentMapOf(),
) {
    companion object {
        const val DEFAULT_ANALYSIS_DAYS = 7
    }
}
