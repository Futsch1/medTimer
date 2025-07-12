package com.futsch1.medtimer.statistics

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.R
import com.futsch1.medtimer.ScheduledReminder
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.helpers.TimeHelper.secondsSinceEpochToLocalDate
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler.TimeAccess
import com.futsch1.medtimer.reminders.scheduling.SchedulingSimulator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class CalendarEventsViewModel(
    application: Application
) : AndroidViewModel(application) {

    val medicineRepository = MedicineRepository(application)
    private var dispatcher = Dispatchers.IO
    private var reminderEvents: List<ReminderEvent> = listOf()
    private var allMedicines: List<FullMedicine> = listOf()
    private var medicine: Medicine? = null
    private val eventsByDay: MutableLiveData<Map<LocalDate, String>> = MutableLiveData()
    private var eventListByDay: MutableMap<LocalDate, MutableList<String>> = mutableMapOf()

    fun getEventForDays(
        medicineId: Int, pastDays: Long, futureDays: Long
    ): LiveData<Map<LocalDate, String>> {
        eventListByDay.clear()

        viewModelScope.launch(dispatcher) {
            reminderEvents = medicineRepository.getLastDaysReminderEvents(pastDays.toInt())
            allMedicines = medicineRepository.medicines
            if (medicineId > 0) {
                medicine = medicineRepository.getOnlyMedicine(medicineId)
                allMedicines = allMedicines.filter { medicine -> medicine.medicine.medicineId == medicineId }
            }
            addPastEvents(pastDays)
            addFutureEvents(futureDays)
            viewModelScope.launch { eventsByDay.value = buildEventsByDay() }
        }
        return eventsByDay
    }

    private fun buildEventsByDay(): Map<LocalDate, String> {
        val eventsByDayStrings: MutableMap<LocalDate, String> = mutableMapOf()
        for (day in eventListByDay.keys) {
            eventListByDay[day]?.let { eventsByDayStrings[day] = buildDayEvents(day, it) }
        }
        return eventsByDayStrings
    }

    private fun buildDayEvents(day: LocalDate, eventStrings: MutableList<String>): String {
        if (eventStrings.isNotEmpty()) {
            eventStrings.addAll(
                0, listOf(day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)), "\n")
            )
        }

        return eventStrings.joinToString(separator = "\n")
    }

    private fun addFutureEvents(futureDays: Long) {
        val timeProvider = object : TimeAccess {
            override fun systemZone(): ZoneId {
                return ZoneId.systemDefault()
            }

            override fun localDate(): LocalDate {
                return LocalDate.now()
            }
        }
        val endDay = LocalDate.now().plusDays(futureDays)

        val schedulingSimulator = SchedulingSimulator(allMedicines, reminderEvents, timeProvider)

        schedulingSimulator.simulate { scheduledReminder: ScheduledReminder, scheduledDate: LocalDate, _: Double ->
            if (scheduledDate < endDay) {
                if (!eventListByDay.containsKey(scheduledDate)) {
                    eventListByDay[scheduledDate] = mutableListOf()
                }
                eventListByDay[scheduledDate]?.add(scheduledReminderToString(scheduledReminder))
            }
            scheduledDate < endDay
        }
    }

    private fun scheduledReminderToString(scheduledReminder: ScheduledReminder): String {
        return TimeHelper.toLocalizedTimeString(
            getApplication<Application>().applicationContext, scheduledReminder.timestamp.epochSecond
        ) + ": " + scheduledReminder.reminder.amount + " " + scheduledReminder.medicine.medicine.name
    }

    private fun addPastEvents(pastDays: Long) {
        val startDay = LocalDate.now().minusDays(pastDays)
        for (reminderEvent: ReminderEvent in reminderEvents) {
            val day = secondsSinceEpochToLocalDate(reminderEvent.remindedTimestamp, ZoneId.systemDefault())
            if ((day >= startDay) && (medicine == null || medicine?.name == MedicineHelper.normalizeMedicineName(reminderEvent.medicineName))) {
                if (!eventListByDay.containsKey(day)) {
                    eventListByDay[day] = mutableListOf()
                }
                eventListByDay[day]?.add(reminderEventToString(reminderEvent, day))
            }
        }
    }

    private fun reminderEventToString(reminderEvent: ReminderEvent, day: LocalDate): String {
        val timeString = getTimeString(reminderEvent, day)
        var reminderEventFormatted = timeString + ": " + reminderEvent.amount + " " + reminderEvent.medicineName
        if (reminderEvent.status == ReminderEvent.ReminderStatus.SKIPPED) {
            reminderEventFormatted += " (" + getApplication<Application>().getString(R.string.skipped) + ")"
        }
        if (reminderEvent.status == ReminderEvent.ReminderStatus.RAISED) {
            reminderEventFormatted += " (?)"
        }
        return reminderEventFormatted
    }

    private fun getTimeString(
        reminderEvent: ReminderEvent, day: LocalDate
    ): String {
        val timeStamp = if (reminderEvent.processedTimestamp != 0L) reminderEvent.processedTimestamp else reminderEvent.remindedTimestamp
        val formatTimestamp: (Context, Long) -> String = if (isSameDay(timeStamp, day)) {
            TimeHelper::toLocalizedTimeString
        } else {
            TimeHelper::toLocalizedDatetimeString
        }
        return formatTimestamp(
            getApplication<Application>().applicationContext, timeStamp
        )
    }

    private fun isSameDay(secondsSinceEpoch: Long, day: LocalDate): Boolean {
        return secondsSinceEpochToLocalDate(secondsSinceEpoch, ZoneId.systemDefault()) == day
    }
}