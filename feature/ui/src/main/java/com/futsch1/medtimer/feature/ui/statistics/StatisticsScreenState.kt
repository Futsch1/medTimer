package com.futsch1.medtimer.feature.ui.statistics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.futsch1.medtimer.core.domain.model.StatisticFragment
import com.futsch1.medtimer.core.ui.component.SortableTableRow
import com.futsch1.medtimer.feature.ui.statistics.calendar.CalendarDayEvent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import java.time.LocalDate

/** Read-only contract the Statistics screen renders from (see [MutableStatisticsScreenState]). */
interface StatisticsScreenState {
    val activeView: StatisticFragment
    val analysisDays: Int
    val charts: ChartsState?
    val tableRows: ImmutableList<SortableTableRow>
    val calendarDayEvents: ImmutableMap<LocalDate, List<CalendarDayEvent>>
}

/** Mutable state holder owned and written only by [StatisticsScreenViewModel]. */
class MutableStatisticsScreenState : StatisticsScreenState {
    override var activeView by mutableStateOf(StatisticFragment.CHARTS)
    override var analysisDays by mutableIntStateOf(7)
    override var charts by mutableStateOf<ChartsState?>(null)
    override var tableRows by mutableStateOf<ImmutableList<SortableTableRow>>(persistentListOf())
    override var calendarDayEvents by mutableStateOf<ImmutableMap<LocalDate, List<CalendarDayEvent>>>(persistentMapOf())
}
