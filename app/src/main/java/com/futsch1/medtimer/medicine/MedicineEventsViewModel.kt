package com.futsch1.medtimer.medicine

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.ScheduledReminder
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.MedicineWithReminders
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler.TimeAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MedicineEventsViewModel(
    application: Application
) :
    AndroidViewModel(application) {

    val medicineRepository = MedicineRepository(application)
    private var dispatcher = Dispatchers.IO
    private var liveReminderEvents: MutableList<ReminderEvent> = mutableListOf()
    private var medicineWithReminders: MutableList<MedicineWithReminders> = mutableListOf()
    private val liveData: MutableLiveData<Map<LocalDate, String>> = MutableLiveData()

    fun getEventForDays(medicineId: Int, deltaDays: Long): LiveData<Map<LocalDate, String>> {
        viewModelScope.launch(dispatcher) {
            liveReminderEvents = medicineRepository.getLastDaysReminderEvents(deltaDays.toInt())
            medicineWithReminders = medicineRepository.medicines
            val dayStrings: MutableMap<LocalDate, String> = mutableMapOf()
            for (deltaDay in -deltaDays..deltaDays) {
                val day = LocalDate.now().plusDays(deltaDay)
                val eventStrings = getPastEvents(day) + getUpcomingEvents(day, medicineId)
                dayStrings[day] = eventStrings.joinToString(separator = "\n")
            }
            viewModelScope.launch { liveData.setValue(dayStrings) }
        }
        return liveData
    }

    private fun getUpcomingEvents(day: LocalDate, medicineId: Int): List<String> {
        val scheduler = ReminderScheduler(object : TimeAccess {
            override fun systemZone(): ZoneId {
                return ZoneId.systemDefault()
            }

            override fun localDate(): LocalDate {
                return day
            }
        }
        )
        return scheduler.schedule(medicineWithReminders, liveReminderEvents)
            .filter { it.medicine.medicineId == medicineId && isOnDay(day, it.timestamp) }
            .map { scheduledReminderToString(it) }
    }

    private fun isOnDay(day: LocalDate, timestamp: Instant): Boolean {
        return day == secondsSinceEpochToLocalDate(timestamp.toEpochMilli() / 1000)

    }

    private fun scheduledReminderToString(scheduledReminder: ScheduledReminder): String {
        return scheduledReminder.medicine.name
    }

    private fun getPastEvents(day: LocalDate): List<String> {
        return liveReminderEvents.filter { secondsSinceEpochToLocalDate(it.remindedTimestamp) == day }
            .map { reminderEventToString(it) }
    }

    private fun reminderEventToString(reminderEvent: ReminderEvent): String {
        return reminderEvent.medicineName
    }

    private fun secondsSinceEpochToLocalDate(secondsSinceEpoch: Long): LocalDate {
        return Instant.ofEpochSecond(secondsSinceEpoch).atZone(ZoneId.systemDefault()).toLocalDate()
    }
}