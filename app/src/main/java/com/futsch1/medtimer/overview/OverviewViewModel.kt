package com.futsch1.medtimer.overview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.ScheduledReminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.preferences.PreferencesNames.USE_RELATIVE_DATE_TIME
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

enum class OverviewFilterToggles {
    TAKEN, SKIPPED, SCHEDULED, RAISED
}

class OverviewViewModel(application: Application, medicineViewModel: MedicineViewModel) : AndroidViewModel(application) {
    var initialized = false
    val overviewEvents = MediatorLiveData<List<OverviewEvent>>()

    private val reminderEvents = medicineViewModel.getLiveReminderEvents(Instant.now().toEpochMilli() / 1000 - (6 * 24 * 60 * 60), false)
    private val scheduledReminders = medicineViewModel.scheduledReminders

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)

    val activeFilters = mutableSetOf<OverviewFilterToggles>()

    var day: LocalDate = LocalDate.now()
        set(value) {
            field = value
            update()
        }

    init {
        overviewEvents.addSource(reminderEvents) {
            overviewEvents.value = getFiltered()
        }
        overviewEvents.addSource(scheduledReminders) {
            overviewEvents.value = getFiltered()
        }

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
        overviewEvents.value = getFiltered()
    }

    private fun getFiltered(): List<OverviewEvent> {
        val filteredOverviewEvents = mutableListOf<OverviewEvent>()

        if (reminderEvents.value != null) {
            for (reminderEvent in reminderEvents.value!!) {
                if (isReminderEventVisible(reminderEvent)) {
                    filteredOverviewEvents.add(create(getApplication(), sharedPreferences, reminderEvent))
                }
            }
        }

        if (scheduledReminders.value != null) {
            for (scheduledReminder in scheduledReminders.value!!) {
                if (isScheduledReminderVisible(scheduledReminder)) {
                    filteredOverviewEvents.add(create(getApplication(), sharedPreferences, scheduledReminder))
                }
            }
        }

        initialized = reminderEvents.value != null && scheduledReminders.value != null

        return filteredOverviewEvents.sortedWith(compareBy<OverviewEvent> { it.timestamp }.thenBy { it.id })
    }

    private fun isScheduledReminderVisible(scheduledReminder: ScheduledReminder): Boolean {
        val scheduledRemindersVisible = activeFilters.isEmpty() || activeFilters.contains(OverviewFilterToggles.SCHEDULED)
        return isSameDayOrNull(scheduledReminder.timestamp.epochSecond, day) && scheduledRemindersVisible
    }

    private fun isReminderEventVisible(reminderEvent: ReminderEvent): Boolean {
        val reminderEventVisible = activeFilters.isEmpty() ||
                (reminderEvent.status == ReminderEvent.ReminderStatus.TAKEN && activeFilters.contains(OverviewFilterToggles.TAKEN)) ||
                (reminderEvent.status == ReminderEvent.ReminderStatus.SKIPPED && activeFilters.contains(OverviewFilterToggles.SKIPPED)) ||
                (reminderEvent.status == ReminderEvent.ReminderStatus.RAISED && activeFilters.contains(OverviewFilterToggles.RAISED))
        return isSameDayOrNull(reminderEvent.remindedTimestamp, day) && reminderEventVisible
    }

    private fun isSameDayOrNull(timestamp: Long, day: LocalDate): Boolean {
        val reminderDate = Instant.ofEpochSecond(timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        return reminderDate.isEqual(day)
    }
}