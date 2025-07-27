package com.futsch1.medtimer.new_overview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.ScheduledReminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.formatReminderString
import com.futsch1.medtimer.helpers.formatScheduledReminderString
import java.time.Instant
import java.time.LocalDate

enum class OverviewFilterToggles {
    TAKEN, SKIPPED, SCHEDULED, RAISED
}

class OverviewViewModel(application: Application, medicineViewModel: MedicineViewModel) : AndroidViewModel(application) {
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
    }

    fun update() {
        overviewEvents.value = getFiltered()
    }

    private fun getFiltered(): List<OverviewEvent> {
        val filteredOverviewEvents = mutableListOf<OverviewEvent>()

        if (reminderEvents.value != null) {
            for (reminderEvent in reminderEvents.value!!) {
                if (isReminderEventVisible(reminderEvent)) {
                    filteredOverviewEvents.add(convertReminderEvent(reminderEvent))
                }
            }
        }

        if (scheduledReminders.value != null) {
            for (scheduledReminder in scheduledReminders.value!!) {
                if (isScheduledReminderVisible(scheduledReminder)) {
                    filteredOverviewEvents.add(convertScheduledReminder(scheduledReminder))
                }
            }
        }

        return filteredOverviewEvents.sortedBy { it.timestamp }
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

    private fun convertScheduledReminder(scheduledReminder: ScheduledReminder): OverviewEvent {
        val text = formatScheduledReminderString(getApplication(), scheduledReminder, sharedPreferences)
        return OverviewEvent(
            scheduledReminder.reminder.reminderId + 1_000_000,
            scheduledReminder.timestamp.epochSecond,
            text,
            scheduledReminder.medicine.medicine.iconId,
            if (scheduledReminder.medicine.medicine.useColor) scheduledReminder.medicine.medicine.color else null,
            OverviewState.PENDING
        )
    }

    private fun convertReminderEvent(reminderEvent: ReminderEvent): OverviewEvent {
        val text = formatReminderString(getApplication(), reminderEvent, sharedPreferences)
        return OverviewEvent(
            reminderEvent.reminderEventId,
            reminderEvent.remindedTimestamp,
            text,
            reminderEvent.iconId,
            if (reminderEvent.useColor) reminderEvent.color else null,
            mapReminderEventState(reminderEvent.status)
        )
    }

    private fun mapReminderEventState(status: ReminderEvent.ReminderStatus): OverviewState {
        return when (status) {
            ReminderEvent.ReminderStatus.RAISED -> OverviewState.RAISED
            ReminderEvent.ReminderStatus.TAKEN -> OverviewState.TAKEN
            ReminderEvent.ReminderStatus.SKIPPED -> OverviewState.SKIPPED
            else -> OverviewState.PENDING
        }
    }
}