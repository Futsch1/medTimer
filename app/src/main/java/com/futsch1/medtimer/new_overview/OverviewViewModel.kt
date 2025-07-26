package com.futsch1.medtimer.new_overview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.formatReminderString
import java.time.Instant

class OverviewViewModel(application: Application, medicineViewModel: MedicineViewModel) : AndroidViewModel(application) {
    val overviewEvents = MediatorLiveData<List<OverviewEvent>>()

    private val reminderEvents = medicineViewModel.getLiveReminderEvents(Instant.now().toEpochMilli() / 1000 - (6 * 24 * 60 * 60), false)
    private val scheduledReminders = medicineViewModel.scheduledReminders

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)

    init {
        overviewEvents.addSource(reminderEvents) {
            overviewEvents.value = getFiltered()
        }
        overviewEvents.addSource(scheduledReminders) {
            overviewEvents.value = getFiltered()
        }
    }

    fun getFiltered(): List<OverviewEvent> {
        val filteredOverviewEvents = mutableListOf<OverviewEvent>()

        if (reminderEvents.value != null) {
            for (reminderEvent in reminderEvents.value!!) {
                filteredOverviewEvents.add(convertReminderEvent(reminderEvent))
            }
        }

        return filteredOverviewEvents.sortedBy { it.timestamp }
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