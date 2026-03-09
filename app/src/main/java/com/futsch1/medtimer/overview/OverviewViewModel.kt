package com.futsch1.medtimer.overview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.database.statusValuesWithoutDelete
import com.futsch1.medtimer.preferences.PreferencesNames.USE_RELATIVE_DATE_TIME
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

enum class OverviewFilterToggles {
    TAKEN, SKIPPED, SCHEDULED, RAISED
}

data class FilterState(val activeFilters: Set<OverviewFilterToggles>, val day: LocalDate, val tick: Long)

class OverviewViewModel(application: Application, medicineViewModel: MedicineViewModel) : AndroidViewModel(application) {
    private var _initialized = false
    val initialized get() = _initialized

    private val _filterState = MutableStateFlow(FilterState(emptySet(), LocalDate.now(), 0L))

    private val reminderEvents =
        medicineViewModel.getLiveReminderEvents(Instant.now().toEpochMilli() / 1000 - (6 * 24 * 60 * 60), statusValuesWithoutDelete)
    private val scheduledReminders = medicineViewModel.scheduledReminders

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)

    var day: LocalDate
        get() = _filterState.value.day
        set(value) {
            _filterState.update { it.copy(day = value) }
        }

    val overviewEvents: SharedFlow<List<OverviewEvent>> =
        combine(reminderEvents, scheduledReminders, _filterState) { events, reminders, fs ->
            getFiltered(events, reminders, fs)
    }.onEach { _initialized = true }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    init {
        if (PreferenceManager.getDefaultSharedPreferences(application).getBoolean(USE_RELATIVE_DATE_TIME, false)) {
            viewModelScope.launch {
                while (true) {
                    delay(60_000)
                    update()
                }
            }
        }
    }

    fun update() {
        _filterState.update { it.copy(tick = it.tick + 1) }
    }

    fun addFilter(f: OverviewFilterToggles) {
        _filterState.update { it.copy(activeFilters = it.activeFilters + f) }
    }

    fun removeFilter(f: OverviewFilterToggles) {
        _filterState.update { it.copy(activeFilters = it.activeFilters - f) }
    }

    fun setFilters(filters: Set<OverviewFilterToggles>) {
        _filterState.update { it.copy(activeFilters = filters) }
    }

    private fun getFiltered(events: List<ReminderEvent>, reminders: List<ScheduledReminder>, filterState: FilterState): List<OverviewEvent> {
        val filteredOverviewEvents = mutableListOf<OverviewEvent>()

        for (reminderEvent in events) {
            if (isReminderEventVisible(reminderEvent, filterState)) {
                filteredOverviewEvents.add(create(getApplication(), sharedPreferences, reminderEvent))
            }
        }

        for (scheduledReminder in reminders) {
            if (isScheduledReminderVisible(scheduledReminder, filterState)) {
                filteredOverviewEvents.add(create(getApplication(), sharedPreferences, scheduledReminder))
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
        val scheduledRemindersVisible = filterState.activeFilters.isEmpty() || filterState.activeFilters.contains(OverviewFilterToggles.SCHEDULED)
        return isSameDay(scheduledReminder.timestamp.epochSecond, filterState.day) && scheduledRemindersVisible
    }

    private fun isReminderEventVisible(reminderEvent: ReminderEvent, filterState: FilterState): Boolean {
        val reminderEventVisible = filterState.activeFilters.isEmpty() ||
                (reminderEvent.status == ReminderEvent.ReminderStatus.TAKEN && filterState.activeFilters.contains(OverviewFilterToggles.TAKEN)) ||
                (reminderEvent.status == ReminderEvent.ReminderStatus.SKIPPED && filterState.activeFilters.contains(OverviewFilterToggles.SKIPPED)) ||
                (reminderEvent.status == ReminderEvent.ReminderStatus.RAISED && filterState.activeFilters.contains(OverviewFilterToggles.RAISED))
        return isSameDay(reminderEvent.remindedTimestamp, filterState.day) && reminderEventVisible
    }

    private fun isSameDay(timestamp: Long, day: LocalDate): Boolean {
        val reminderDate = Instant.ofEpochSecond(timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        return reminderDate.isEqual(day)
    }
}
