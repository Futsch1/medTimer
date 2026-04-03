package com.futsch1.medtimer.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.database.ReminderEventEntity
import com.futsch1.medtimer.database.toModel
import com.futsch1.medtimer.model.OverviewFilter
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.model.ScheduledReminder
import com.futsch1.medtimer.model.reminderevent.ReminderEvent
import com.futsch1.medtimer.overview.model.EventPosition
import com.futsch1.medtimer.overview.model.OverviewEvent
import com.futsch1.medtimer.overview.model.PastReminderEvent
import com.futsch1.medtimer.overview.model.ScheduledReminderEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

@HiltViewModel(assistedFactory = OverviewViewModel.Factory::class)
class OverviewViewModel @AssistedInject constructor(
    preferencesDataSource: PreferencesDataSource,
    private val reminderEventFactory: PastReminderEvent.Factory,
    private val scheduledReminderEventFactory: ScheduledReminderEvent.Factory,
    @Assisted private val reminderEvents: Flow<List<ReminderEventEntity>>,
    @Assisted private val scheduledReminders: SharedFlow<List<ScheduledReminder>>
) : ViewModel() {
    private data class FilterState(val activeFilters: Set<OverviewFilter>, val day: LocalDate, val tick: Long)

    @AssistedFactory
    interface Factory {
        fun create(
            reminderEvents: Flow<List<ReminderEventEntity>>,
            scheduledReminders: SharedFlow<List<ScheduledReminder>>
        ): OverviewViewModel
    }

    private var _initialized = false
    val initialized get() = _initialized

    private val filterState = MutableStateFlow(FilterState(emptySet(), LocalDate.now(), 0L))

    var day: LocalDate
        get() = filterState.value.day
        set(value) {
            filterState.update { it.copy(day = value) }
        }

    val overviewEvents: SharedFlow<List<OverviewEvent>> =
        combine(reminderEvents.map { it.map { entity -> entity.toModel() } }, scheduledReminders, filterState) { events, reminders, fs ->
            getFiltered(events, reminders, fs)
        }.onEach { _initialized = true }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    init {
        if (preferencesDataSource.preferences.value.useRelativeDateTime) {
            viewModelScope.launch {
                while (true) {
                    delay(60_000)
                    update()
                }
            }
        }
    }

    fun update() {
        filterState.update { it.copy(tick = it.tick + 1) }
    }

    fun addFilter(f: OverviewFilter) {
        filterState.update { it.copy(activeFilters = it.activeFilters + f) }
    }

    fun removeFilter(f: OverviewFilter) {
        filterState.update { it.copy(activeFilters = it.activeFilters - f) }
    }

    fun setFilters(filters: Set<OverviewFilter>) {
        filterState.update { it.copy(activeFilters = filters) }
    }

    private fun getFiltered(events: List<ReminderEvent>, reminders: List<ScheduledReminder>, filterState: FilterState): List<OverviewEvent> {
        val filteredOverviewEvents = mutableListOf<OverviewEvent>()

        for (reminderEvent in events) {
            if (isReminderEventVisible(reminderEvent, filterState)) {
                filteredOverviewEvents.add(reminderEventFactory.create(reminderEvent))
            }
        }

        for (scheduledReminder in reminders) {
            if (isScheduledReminderVisible(scheduledReminder, filterState)) {
                filteredOverviewEvents.add(scheduledReminderEventFactory.create(scheduledReminder))
            }
        }

        return assignPositions(filteredOverviewEvents.sortedWith(compareBy<OverviewEvent> { it.timestamp }.thenBy { it.id }))
    }

    private fun assignPositions(overviewEvents: List<OverviewEvent>): List<OverviewEvent> {
        overviewEvents.forEach { overviewEvent -> overviewEvent.eventPosition = EventPosition.MIDDLE }
        if (overviewEvents.size == 1) {
            overviewEvents[0].eventPosition = EventPosition.ONLY
        } else {
            overviewEvents.firstOrNull()?.eventPosition = EventPosition.FIRST
            overviewEvents.lastOrNull()?.eventPosition = EventPosition.LAST
        }
        return overviewEvents
    }

    private fun isScheduledReminderVisible(scheduledReminder: ScheduledReminder, filterState: FilterState): Boolean {
        val scheduledRemindersVisible = filterState.activeFilters.isEmpty() || filterState.activeFilters.contains(OverviewFilter.SCHEDULED)
        return isSameDay(scheduledReminder.timestamp.epochSecond, filterState.day) && scheduledRemindersVisible
    }

    private fun isReminderEventVisible(reminderEvent: ReminderEvent, filterState: FilterState): Boolean {
        val reminderEventVisible = filterState.activeFilters.isEmpty() ||
                (reminderEvent.status == ReminderEvent.ReminderStatus.TAKEN && filterState.activeFilters.contains(OverviewFilter.TAKEN)) ||
                (reminderEvent.status == ReminderEvent.ReminderStatus.SKIPPED && filterState.activeFilters.contains(OverviewFilter.SKIPPED)) ||
                (reminderEvent.status == ReminderEvent.ReminderStatus.RAISED && filterState.activeFilters.contains(OverviewFilter.RAISED))
        return isSameDay(reminderEvent.remindedTimestamp.epochSecond, filterState.day) && reminderEventVisible
    }

    private fun isSameDay(timestamp: Long, day: LocalDate): Boolean {
        val reminderDate = Instant.ofEpochSecond(timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        return reminderDate.isEqual(day)
    }
}
