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

/**
 * The Statistics screen's state contract — the read-only view the UI sees. The view-model owns the
 * mutable implementation ([MutableStatisticsScreenState]) and is the only writer; composables read
 * these properties directly (Compose snapshot state is already reactive, so no
 * `collectAsStateWithLifecycle` is needed). See `docs/guidelines/jetpack-compose.md` §State holders.
 */
interface StatisticsScreenState {
    val activeView: StatisticFragment
    val analysisDays: Int
    val charts: ChartsState?

    /** The Table view's filter text — updates instantly so the field stays responsive. */
    val query: String

    /** Rows for the Table view, already filtered by [query] off the main thread. */
    val filteredRows: ImmutableList<SortableTableRow>

    val calendarDayEvents: ImmutableMap<LocalDate, List<CalendarDayEvent>>
}

/**
 * Mutable implementation owned by [StatisticsScreenViewModel]. Repository flows are collected in the
 * view-model and written into these properties; user actions go through view-model methods that
 * mutate them. The screen never writes here — it only reads the [StatisticsScreenState] interface.
 */
class MutableStatisticsScreenState : StatisticsScreenState {
    override var activeView by mutableStateOf(StatisticFragment.CHARTS)
    override var analysisDays by mutableIntStateOf(DEFAULT_ANALYSIS_DAYS)
    override var charts by mutableStateOf<ChartsState?>(null)
    override var query by mutableStateOf("")
    override var calendarDayEvents by mutableStateOf<ImmutableMap<LocalDate, List<CalendarDayEvent>>>(persistentMapOf())

    /**
     * The presented table rows before the in-screen text filter. Internal to the holder — the UI only
     * ever reads [filteredRows]; this is the raw input the table flow writes and the filter loop reads.
     */
    var tableRows by mutableStateOf<ImmutableList<SortableTableRow>>(persistentListOf())

    /**
     * The normalized search query, lagging [query] by the debounce window. Internal — only the
     * off-main-thread filter loop reads it, so a keystroke storm doesn't trigger a scan each time.
     */
    var debouncedQuery by mutableStateOf("")

    /**
     * Unlike a `derivedStateOf`, this is written by the view-model's off-main-thread filter loop: the
     * substring scan can be heavy for long reminder histories, so the UI thread never runs it. See
     * [StatisticsScreenViewModel] and [com.futsch1.medtimer.feature.ui.statistics.table.ReminderRowFilter].
     */
    override var filteredRows by mutableStateOf<ImmutableList<SortableTableRow>>(persistentListOf())

    companion object {
        const val DEFAULT_ANALYSIS_DAYS = 7
    }
}
