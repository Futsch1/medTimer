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
import com.futsch1.medtimer.core.ui.component.SortableTableRow
import com.futsch1.medtimer.core.ui.filter.TagEventFilter
import com.futsch1.medtimer.feature.ui.statistics.calendar.CalendarDayEvent
import com.futsch1.medtimer.feature.ui.statistics.charts.ChartsPresenter
import com.futsch1.medtimer.feature.ui.statistics.table.ReminderTablePresenter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
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
    persistentDataDataSource: PersistentDataDataSource,
    tagEventFilter: TagEventFilter,
    @Dispatcher(MedTimerDispatchers.IO) ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    // The two session selections share one shape (seeded from persistence, written through on change),
    // so the persist-and-skip-unchanged rule lives in PersistedSelection rather than per handler.
    private val activeView = PersistedSelection(
        initial = persistentDataDataSource.data.value.activeStatisticsFragment,
        persist = persistentDataDataSource::setActiveStatisticsFragment,
    )

    private val analysisDays = PersistedSelection(
        initial = persistentDataDataSource.data.value.analysisDays,
        persist = persistentDataDataSource::setAnalysisDays,
    )

    // One shared read of the taken/skipped reminder events feeds charts, table, and calendar — the
    // Room flow is cold, so subscribing per view would spin up three independent DB observers.
    private val reminderEvents = reminderEventRepository
        .getAllFlow(0, ReminderEvent.statusValuesTakenOrSkipped)
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    // Each view slice derives independently, so one input change recomputes only its own slice rather
    // than the whole screen state.
    private val charts: StateFlow<ChartsState?> =
        combine(reminderEvents, analysisDays.value) { events, days ->
            val data = statisticsProvider.aggregate(events, days)
            // First custom color wins if names somehow collide — matches the prior firstOrNull lookup.
            val medicineColorsByName = medicineRepository.getAll()
                .filter { it.useColor }
                .reversed()
                .associate { it.name to it.color }
            chartsPresenter.present(data, medicineColorsByName, days)
        }
            .flowOn(ioDispatcher)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // All taken/skipped events, tag-filtered, then presented as rows.
    private val tableRows: StateFlow<ImmutableList<SortableTableRow>> =
        combine(
            reminderEvents,
            persistentDataDataSource.data,
            tagRepository.getAllFlow(),
        ) { events, persistent, tags ->
            val selectedTagIds = persistent.filterTags.mapNotNull { it.toIntOrNull() }.toSet()
            reminderTablePresenter.present(tagEventFilter.filter(events, selectedTagIds, tags))
        }
            .flowOn(ioDispatcher)
            .stateIn(viewModelScope, SharingStarted.Eagerly, persistentListOf())

    // The provider re-reads its own time window on each emission of the shared event flow, so the
    // events payload is only a change trigger — the provider owns the calendar's reactivity.
    private val calendarDayEvents: StateFlow<ImmutableMap<LocalDate, List<CalendarDayEvent>>> =
        calendarEventsProvider
            .structuredEventsFlow(reminderEvents, ALL_MEDICINES, CALENDAR_PAST_MONTHS, CALENDAR_FUTURE_MONTHS)
            .map { it.toImmutableMap() }
            .flowOn(ioDispatcher)
            .stateIn(viewModelScope, SharingStarted.Eagerly, persistentMapOf())

    // The single read-only state the screen collects: the two selections combined with the three
    // independently-derived view slices. Each slice keeps its own recompute trigger above.
    val uiState: StateFlow<StatisticsUiState> =
        combine(
            activeView.value,
            analysisDays.value,
            charts,
            tableRows,
            calendarDayEvents,
        ) { view, days, chartsState, rows, calendar ->
            StatisticsUiState(view, days, chartsState, rows, calendar)
        }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                StatisticsUiState(activeView = activeView.value.value, analysisDays = analysisDays.value.value),
            )

    fun onSelectView(view: StatisticFragment) = activeView.set(view)

    fun onSelectRange(days: Int) = analysisDays.set(days)

    companion object {
        private const val ALL_MEDICINES = -1
        private const val CALENDAR_PAST_MONTHS = 3
        private const val CALENDAR_FUTURE_MONTHS = 0
    }
}
