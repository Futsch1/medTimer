package com.futsch1.medtimer.feature.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.core.common.di.Dispatcher
import com.futsch1.medtimer.core.common.di.MedTimerDispatchers
import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.StatisticFragment
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.domain.repository.TagRepository
import com.futsch1.medtimer.core.ui.TimeFormatter
import com.futsch1.medtimer.core.ui.component.SortableTableCell
import com.futsch1.medtimer.core.ui.component.SortableTableRow
import com.futsch1.medtimer.core.ui.filter.TagEventFilter
import com.futsch1.medtimer.feature.ui.statistics.charts.ChartsPresenter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatisticsScreenViewModel @Inject constructor(
    private val statisticsProvider: StatisticsProvider,
    private val chartsPresenter: ChartsPresenter,
    private val calendarEventsProvider: CalendarEventsProvider,
    private val medicineRepository: MedicineRepository,
    private val reminderEventRepository: ReminderEventRepository,
    private val tagRepository: TagRepository,
    private val persistentDataDataSource: PersistentDataDataSource,
    private val tagEventFilter: TagEventFilter,
    private val timeFormatter: TimeFormatter,
    @param:Dispatcher(MedTimerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStatisticsScreenState()
    val state: StatisticsScreenState get() = _state

    private val analysisDays = MutableStateFlow(persistentDataDataSource.data.value.analysisDays)

    // One shared read of the taken/skipped reminder events feeds charts, table, and calendar — the
    // Room flow is cold, so subscribing per view would spin up three independent DB observers.
    private val reminderEvents = reminderEventRepository
        .getAllFlow(0, ReminderEvent.statusValuesTakenOrSkipped)
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    init {
        val data = persistentDataDataSource.data.value
        _state.activeView = data.activeStatisticsFragment
        _state.analysisDays = data.analysisDays
        observeCharts()
        observeTableRows()
        observeCalendar()
    }

    fun onSelectView(view: StatisticFragment) {
        _state.activeView = view
        persistentDataDataSource.setActiveStatisticsFragment(view)
    }

    fun onSelectRange(days: Int) {
        if (days == _state.analysisDays) return
        _state.analysisDays = days
        persistentDataDataSource.setAnalysisDays(days)
        analysisDays.value = days
    }

    private fun observeCharts() {
        // Recompute whenever the reminder events change or the selected range changes — same reactive
        // pattern as observeTableRows, so adding an event refreshes the charts without a manual reload.
        viewModelScope.launch {
            combine(reminderEvents, analysisDays) { events, days -> buildChartsState(events, days) }
                .flowOn(ioDispatcher)
                .collect { _state.charts = it }
        }
    }

    private suspend fun buildChartsState(events: List<ReminderEvent>, days: Int): ChartsState {
        val data = statisticsProvider.aggregate(events, days)
        // First custom color wins if names somehow collide — matches the prior firstOrNull lookup.
        val medicineColorsByName = medicineRepository.getAll()
            .filter { it.useColor }
            .reversed()
            .associate { it.name to it.color }
        return chartsPresenter.present(data, medicineColorsByName, days)
    }

    private fun observeCalendar() {
        // The calendar shows past + scheduled reminders; recompute on event changes so it stays in
        // sync with the rest of the screen. The emitted list is just a change trigger — the provider
        // reads its own time-windowed slice (getLastDays), which differs from the charts query.
        viewModelScope.launch {
            reminderEvents
                .map { calendarEventsProvider.getStructuredEvents(ALL_MEDICINES, CALENDAR_PAST_MONTHS, CALENDAR_FUTURE_MONTHS).toImmutableMap() }
                .flowOn(ioDispatcher)
                .collect { _state.calendarDayEvents = it }
        }
    }

    private fun observeTableRows() {
        viewModelScope.launch {
            combine(
                reminderEvents,
                persistentDataDataSource.data,
                tagRepository.getAllFlow(),
            ) { events, persistent, tags ->
                val selectedTagIds = persistent.filterTags.mapNotNull { it.toIntOrNull() }.toSet()
                tagEventFilter.filter(events, selectedTagIds, tags).map { it.toTableRow() }.toImmutableList()
            }
                .flowOn(ioDispatcher)
                .collect { _state.tableRows = it }
        }
    }

    private fun ReminderEvent.toTableRow(): SortableTableRow {
        val takenText = if (status == ReminderEvent.ReminderStatus.TAKEN) {
            timeFormatter.toDateTimeString(processedTimestamp)
        } else {
            "-"
        }
        return SortableTableRow(
            id = reminderEventId.toLong(),
            cells = persistentListOf(
                SortableTableCell(takenText, processedTimestamp),
                SortableTableCell(medicineName),
                SortableTableCell(amount),
                SortableTableCell(timeFormatter.toDateTimeString(remindedTimestamp), remindedTimestamp),
            ),
        )
    }

    companion object {
        private const val ALL_MEDICINES = -1
        private const val CALENDAR_PAST_MONTHS = 3
        private const val CALENDAR_FUTURE_MONTHS = 0
    }
}
