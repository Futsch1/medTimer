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
import com.futsch1.medtimer.core.ui.filter.TagEventFilter
import com.futsch1.medtimer.feature.ui.statistics.charts.ChartsPresenter
import com.futsch1.medtimer.feature.ui.statistics.table.ReminderTablePresenter
import dagger.hilt.android.lifecycle.HiltViewModel
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
    statisticsProvider: StatisticsProvider,
    chartsPresenter: ChartsPresenter,
    reminderTablePresenter: ReminderTablePresenter,
    calendarEventsProvider: CalendarEventsProvider,
    medicineRepository: MedicineRepository,
    reminderEventRepository: ReminderEventRepository,
    tagRepository: TagRepository,
    private val persistentDataDataSource: PersistentDataDataSource,
    tagEventFilter: TagEventFilter,
    @Dispatcher(MedTimerDispatchers.IO) ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStatisticsScreenState()
    val state: StatisticsScreenState get() = _state

    // The single source of truth for the Analysis range. The snapshot the dropdown reads
    // (_state.analysisDays) is a one-way projection of this flow, so the UI value and the value the
    // charts aggregate over cannot drift apart — there is nothing to hand-sync.
    private val analysisDays = MutableStateFlow(persistentDataDataSource.data.value.analysisDays)

    // One shared read of the taken/skipped reminder events feeds charts, table, and calendar — the
    // Room flow is cold, so subscribing per view would spin up three independent DB observers.
    private val reminderEvents = reminderEventRepository
        .getAllFlow(0, ReminderEvent.statusValuesTakenOrSkipped)
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    init {
        _state.activeView = persistentDataDataSource.data.value.activeStatisticsFragment
        _state.analysisDays = analysisDays.value

        // Mirror the range flow into the snapshot the dropdown reads — the only writer of
        // _state.analysisDays besides the init seed, both projecting the same flow.
        viewModelScope.launch {
            analysisDays.collect { _state.analysisDays = it }
        }

        // Recompute whenever the reminder events change or the selected range changes, so adding an
        // event refreshes the charts without a manual reload.
        viewModelScope.launch {
            combine(reminderEvents, analysisDays) { events, days ->
                val data = statisticsProvider.aggregate(events, days)
                // First custom color wins if names somehow collide — matches the prior firstOrNull lookup.
                val medicineColorsByName = medicineRepository.getAll()
                    .filter { it.useColor }
                    .reversed()
                    .associate { it.name to it.color }
                chartsPresenter.present(data, medicineColorsByName, days)
            }
                .flowOn(ioDispatcher)
                .collect { _state.charts = it }
        }

        // The reminder table shows all taken/skipped events, tag-filtered, then presented as rows.
        viewModelScope.launch {
            combine(
                reminderEvents,
                persistentDataDataSource.data,
                tagRepository.getAllFlow(),
            ) { events, persistent, tags ->
                val selectedTagIds = persistent.filterTags.mapNotNull { it.toIntOrNull() }.toSet()
                reminderTablePresenter.present(tagEventFilter.filter(events, selectedTagIds, tags))
            }
                .flowOn(ioDispatcher)
                .collect { _state.tableRows = it }
        }

        // The provider turns the shared event flow into a stream of structured month events; we just
        // render each emission. It re-reads its own time window (getLastDays), so the events payload
        // is only a change trigger — hence the shared flow is handed over, not its values.
        viewModelScope.launch {
            calendarEventsProvider
                .structuredEventsFlow(reminderEvents, ALL_MEDICINES, CALENDAR_PAST_MONTHS, CALENDAR_FUTURE_MONTHS)
                .map { it.toImmutableMap() }
                .flowOn(ioDispatcher)
                .collect { _state.calendarDayEvents = it }
        }
    }

    fun onSelectView(view: StatisticFragment) {
        _state.activeView = view
        persistentDataDataSource.setActiveStatisticsFragment(view)
    }

    fun onSelectRange(days: Int) {
        if (days == analysisDays.value) return
        persistentDataDataSource.setAnalysisDays(days)
        analysisDays.value = days
    }

    companion object {
        private const val ALL_MEDICINES = -1
        private const val CALENDAR_PAST_MONTHS = 3
        private const val CALENDAR_FUTURE_MONTHS = 0
    }
}
