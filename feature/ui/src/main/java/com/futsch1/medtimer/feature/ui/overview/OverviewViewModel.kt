package com.futsch1.medtimer.feature.ui.overview

import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.core.common.di.Dispatcher
import com.futsch1.medtimer.core.common.di.IsDebugBuild
import com.futsch1.medtimer.core.common.di.MedTimerDispatchers
import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.OverviewFilter
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.SimulatedReminder
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.ui.list.SelectionListController
import com.futsch1.medtimer.feature.reminders.api.SimulatedReminders
import com.futsch1.medtimer.feature.ui.TagFilterViewModel
import com.futsch1.medtimer.feature.ui.overview.model.OverviewEvent
import com.futsch1.medtimer.feature.ui.overview.model.OverviewState
import com.futsch1.medtimer.feature.ui.overview.model.PastReminderEvent
import com.futsch1.medtimer.feature.ui.overview.model.SimulatedReminderEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.seconds

@HiltViewModel(assistedFactory = OverviewViewModel.Factory::class)
class OverviewViewModel @AssistedInject constructor(
    private val preferencesDataSource: PreferencesDataSource,
    private val persistentDataDataSource: PersistentDataDataSource,
    medicineRepository: MedicineRepository,
    reminderEventRepository: ReminderEventRepository,
    private val simulatedRemindersRepository: SimulatedReminders,
    private val reminderEventFactory: PastReminderEvent.Factory,
    private val simulatedReminderEventFactory: SimulatedReminderEvent.Factory,
    private val powerManager: PowerManager,
    @param:ApplicationContext private val context: Context,
    @param:IsDebugBuild private val isDebugBuild: Boolean,
    @Dispatcher(MedTimerDispatchers.Default) defaultDispatcher: CoroutineDispatcher,
    @Assisted private val tagFilterViewModel: TagFilterViewModel
) : ViewModel() {

    @AssistedFactory
    fun interface Factory {
        fun create(tagFilterViewModel: TagFilterViewModel): OverviewViewModel
    }

    private data class FilterState(
        val activeFilters: Set<OverviewFilter>,
        val day: LocalDate,
        val tick: Long
    )

    private val _state = MutableOverviewScreenState()
    val state: OverviewScreenState get() = _state

    /** Selection state for the multi-select contextual bar. */
    val selection = SelectionListController<OverviewEvent> { it.id }

    private val filterState = MutableStateFlow(FilterState(emptySet(), LocalDate.now(), 0L))

    // Expands from default 6-day window to Instant.EPOCH when user scrolls into past
    private val queryStart = MutableStateFlow(Instant.now().minus(Duration.of(6, ChronoUnit.DAYS)))

    fun selectDay(value: LocalDate) {
        _state.day = value
        filterState.update { it.copy(day = value) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val reminderEvents: Flow<List<ReminderEvent>> =
        combine(
            queryStart.flatMapLatest { start ->
                reminderEventRepository.getAllFlow(
                    start,
                    ReminderEvent.statusValuesWithoutDelete
                )
            },
            tagFilterViewModel.validTagIds,
            tagFilterViewModel.liveTags
        ) { events, tagIds, tags ->
            tagFilterViewModel.getFilteredEvents(events, tagIds, tags)
        }

    private val liveMedicines = medicineRepository.getAllFlow()

    private val _simulatedReminders = MutableStateFlow<List<SimulatedReminder>>(emptyList())

    val medicines: StateFlow<List<Medicine>> =
        combine(liveMedicines, tagFilterViewModel.validTagIds) { medicines, tagIds ->
            tagFilterViewModel.getFiltered(medicines, tagIds ?: emptySet())
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val simulatedReminders: SharedFlow<List<SimulatedReminder>> =
        combine(_simulatedReminders, tagFilterViewModel.validTagIds) { reminders, tagIds ->
            tagFilterViewModel.getFiltered(reminders, tagIds ?: emptySet())
        }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    private val reminderEventsByDay: Flow<Map<Long, List<ReminderEvent>>> =
        reminderEvents.map { events -> events.groupBy { it.remindedTimestamp.epochDay() } }

    private val simulatedRemindersByDay: Flow<Map<Long, List<SimulatedReminder>>> =
        simulatedReminders.map { reminders -> reminders.groupBy { it.scheduledReminder.timestamp.epochDay() } }

    val overviewEvents: SharedFlow<List<OverviewEvent>> =
        combine(reminderEventsByDay, simulatedRemindersByDay, filterState) { eventsByDay, remindersByDay, fs ->
            val day = fs.day.toEpochDay()
            getFiltered(eventsByDay[day].orEmpty(), remindersByDay[day].orEmpty(), fs)
        }.flowOn(defaultDispatcher)
            .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    init {
        selection.bind(viewModelScope, overviewEvents)
        overviewEvents.onEach { _state.events = it.toPersistentList() }.launchIn(viewModelScope)
        simulatedRemindersRepository.simulatedThrough.onEach { _state.simulatedThrough = it }.launchIn(viewModelScope)
        preferencesDataSource.preferences.onEach { _state.combineNotifications = it.combineNotifications }
            .launchIn(viewModelScope)
        combine(preferencesDataSource.preferences, persistentDataDataSource.data) { _, _ -> }
            .onEach { refreshWarnings() }
            .launchIn(viewModelScope)

        setFilters(persistentDataDataSource.data.value.checkedFilters)

        viewModelScope.launch {
            simulatedRemindersRepository.simulatedReminders.collect { reminders ->
                _simulatedReminders.value = reminders
            }
        }

        viewModelScope.launch {
            filterState.collect { fs ->
                // Expand past query when user scrolls beyond the default 6-day window
                if (fs.day < LocalDate.now().minusDays(6) && queryStart.value > Instant.EPOCH) {
                    queryStart.value = Instant.EPOCH
                }
                // Request a wider simulation window when scrolling far into the future
                val dayOffset = ChronoUnit.DAYS.between(LocalDate.now(), fs.day)
                if (dayOffset >= 21) {
                    simulatedRemindersRepository.requestWindow("overview", dayOffset + 28)
                } else {
                    simulatedRemindersRepository.releaseWindow("overview")
                }
            }
        }

        if (preferencesDataSource.preferences.value.useRelativeDateTime) {
            viewModelScope.launch {
                while (true) {
                    delay(60.seconds)
                    update()
                }
            }
        }
    }

    override fun onCleared() {
        simulatedRemindersRepository.releaseWindow("overview")
    }

    fun update() {
        filterState.update { it.copy(tick = it.tick + 1) }
    }


    fun toggleFilter(f: OverviewFilter) {
        setFilters(if (f in _state.activeFilters) _state.activeFilters - f else _state.activeFilters + f)
        persistentDataDataSource.setCheckedFilters(_state.activeFilters)
    }

    fun setFilters(filters: Set<OverviewFilter>) {
        _state.activeFilters = filters.toPersistentSet()
        filterState.update { it.copy(activeFilters = filters) }
    }

    /** Long-pressing one event selects every event scheduled for the same time. */
    fun selectSameTimeEvents(event: OverviewEvent) {
        selection.items.filter { it.timestamp == event.timestamp }.forEach(selection::toggleSelection)
    }

    fun refreshWarnings() {
        val persistentData = persistentDataDataSource.data.value
        _state.warnings.showBatteryOptimizationWarning = showBatteryOptimizationWarning(
            warningsSuppressed = isDebugBuild,
            isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(context.packageName),
            batteryWarningShown = persistentData.batteryWarningShown,
        )
        _state.warnings.showExactRemindersWarning = showExactRemindersWarning(
            warningsSuppressed = isDebugBuild,
            exactReminders = preferencesDataSource.preferences.value.exactReminders,
            exactRemindersWarningShown = persistentData.exactRemindersWarningShown,
        )
    }

    fun dismissBatteryWarning() {
        persistentDataDataSource.setBatteryWarningShown(true)
        refreshWarnings()
    }

    fun dismissExactRemindersWarning() {
        persistentDataDataSource.setExactRemindersWarningShown(true)
        refreshWarnings()
    }

    private fun getFiltered(
        events: List<ReminderEvent>,
        reminders: List<SimulatedReminder>,
        filterState: FilterState
    ): List<OverviewEvent> {
        val filteredOverviewEvents = mutableListOf<OverviewEvent>()

        for (reminderEvent in events) {
            val overviewEvent = reminderEventFactory.create(reminderEvent)
            if (isOverviewEventVisible(overviewEvent, filterState)) {
                filteredOverviewEvents.add(overviewEvent)
            }
        }

        val coveredSlots = events.map { it.reminderId to it.remindedTimestamp.epochSecond }.toSet()

        for (simulatedReminder in reminders) {
            val scheduledReminder = simulatedReminder.scheduledReminder
            val slot = scheduledReminder.reminder.id to scheduledReminder.timestamp.epochSecond
            val overviewEvent = simulatedReminderEventFactory.create(simulatedReminder)
            if (slot !in coveredSlots && isOverviewEventVisible(overviewEvent, filterState)) {
                filteredOverviewEvents.add(overviewEvent)
            }
        }

        return filteredOverviewEvents.sortedWith(compareBy<OverviewEvent> { it.timestamp }.thenBy { it.id })
    }

    private fun isOverviewEventVisible(
        overviewEvent: OverviewEvent,
        filterState: FilterState
    ): Boolean {
        return filterState.activeFilters.isEmpty() || when (overviewEvent.state) {
            OverviewState.PENDING -> OverviewFilter.SCHEDULED
            OverviewState.RAISED, OverviewState.LOCATION -> OverviewFilter.RAISED
            OverviewState.TAKEN -> OverviewFilter.TAKEN
            OverviewState.SKIPPED -> OverviewFilter.SKIPPED
        } in filterState.activeFilters
    }

    private fun Instant.epochDay(): Long = atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
}
