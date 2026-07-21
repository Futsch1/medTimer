package com.futsch1.medtimer.feature.ui.impl.statistics

import androidx.compose.runtime.snapshotFlow
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
import com.futsch1.medtimer.feature.ui.impl.statistics.charts.ChartsPresenter
import com.futsch1.medtimer.feature.ui.impl.statistics.table.ReminderRowFilter
import com.futsch1.medtimer.feature.ui.impl.statistics.table.ReminderRowFilterInputs
import com.futsch1.medtimer.feature.ui.impl.statistics.table.ReminderTablePresenter
import com.futsch1.medtimer.feature.ui.impl.statistics.table.filteredRows
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Drives the Statistics screen. Following `docs/guidelines/jetpack-compose.md` §State holders, the
 * view-model owns a [MutableStatisticsScreenState] and exposes only the read-only [state]; it does
 * not publish a `StateFlow` of the screen state. Repository flows are collected here and written into
 * `_state.*`, and the screen reads `state.*` directly.
 */
@OptIn(FlowPreview::class)
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
    @Dispatcher(MedTimerDispatchers.Default) filterDispatcher: CoroutineDispatcher,
) : ViewModel() {

    // Seed the screen state from persistence at construction time. Doing this in the constructor
    // (rather than defaulting to the fallback and mutating in init) avoids a race where the charts'
    // snapshotFlow reads the default range before the persisted value is visible across dispatchers.
    private val _state = MutableStatisticsScreenState(
        activeView = persistentDataDataSource.data.value.activeStatisticsFragment,
        analysisDays = persistentDataDataSource.data.value.analysisDays,
    )
    val state: StatisticsScreenState get() = _state

    // Search input lives on a flow so the field updates instantly while only the (potentially heavy)
    // row scan is debounced and pushed off the main thread.
    private val searchTextFlow = MutableStateFlow("")

    // One shared read of the taken/skipped reminder events feeds charts, table, and calendar — the
    // Room flow is cold, so subscribing per view would spin up three independent DB observers.
    private val reminderEvents = reminderEventRepository
        .getAllFlow(Instant.EPOCH, ReminderEvent.statusValuesTakenOrSkipped)
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    init {
        // Charts depend on the Analysis range, which now lives in Compose state — bridge it back into
        // the flow world with snapshotFlow so the aggregation stays reactive while the heavy work runs
        // off the main thread. Each slice writes only its own property, so one input recomputes one slice.
        combine(
            reminderEvents,
            snapshotFlow { _state.analysisDays },
            medicineRepository.getAllFlow(),
        ) { events, days, medicines ->
            val data = statisticsProvider.aggregate(events, days)
            // First custom color wins if names somehow collide — matches the prior firstOrNull lookup.
            val medicineColorsByName = medicines
                .filter { it.useColor }
                .reversed()
                .associate { it.name to it.color }
            chartsPresenter.present(data, medicineColorsByName, days)
        }
            .flowOn(ioDispatcher)
            .onEach { _state.charts = it }
            .launchIn(viewModelScope)

        // All taken/skipped events, tag-filtered, then presented as rows. The in-screen text filter is
        // applied separately as filteredRows in the state holder.
        combine(
            reminderEvents,
            persistentDataDataSource.data,
            tagRepository.getAllFlow(),
        ) { events, persistent, tags ->
            val selectedTagIds = persistent.filterTags.mapNotNull { it.toIntOrNull() }.toSet()
            reminderTablePresenter.present(tagEventFilter.filter(events, selectedTagIds, tags))
        }
            .flowOn(ioDispatcher)
            .onEach { _state.tableRows = it }
            .launchIn(viewModelScope)

        // The provider re-reads its own time window on each emission of the shared event flow, so the
        // events payload is only a change trigger — the provider owns the calendar's reactivity.
        calendarEventsProvider
            .structuredEventsFlow(ALL_MEDICINES, CALENDAR_PAST_MONTHS)
            .map { it.toImmutableMap() }
            .flowOn(ioDispatcher)
            .onEach { _state.calendarDayEvents = it }
            .launchIn(viewModelScope)

        // Table text filter: the field reflects every keystroke immediately via `query`, while only
        // the debounced query drives the row scan, which runs off the main thread so a long reminder
        // history can't jank typing. Wired last so the first scan sees the table rows the combine
        // above already produced.
        searchTextFlow
            .onEach { _state.query = it }
            .launchIn(viewModelScope)

        searchTextFlow
            .debounce(SEARCH_DEBOUNCE_MILLIS.milliseconds)
            .onEach { _state.debouncedQuery = ReminderRowFilter.normalizeQuery(it) }
            .launchIn(viewModelScope)

        snapshotFlow { ReminderRowFilterInputs(_state.tableRows, _state.debouncedQuery) }
            .filteredRows(filterDispatcher)
            .onEach { _state.filteredRows = it }
            .launchIn(viewModelScope)
    }

    // Write-through selections: skip a redundant persist when unchanged, then update Compose state.
    fun onSelectView(view: StatisticFragment) {
        if (view == _state.activeView) return
        persistentDataDataSource.setActiveStatisticsFragment(view)
        _state.activeView = view
    }

    fun onSelectRange(days: Int) {
        if (days == _state.analysisDays) return
        persistentDataDataSource.setAnalysisDays(days)
        _state.analysisDays = days
    }

    fun onSearchQueryChange(query: String) {
        searchTextFlow.value = query
    }

    companion object {
        private const val ALL_MEDICINES = -1
        private const val CALENDAR_PAST_MONTHS = 3
        private const val SEARCH_DEBOUNCE_MILLIS = 300L
    }
}
