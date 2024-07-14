package com.futsch1.medtimer.statistics

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.R
import com.futsch1.medtimer.ScheduledReminder
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.MedicineWithReminders
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler.TimeAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class CalendarEventsViewModel(
    application: Application
) :
    AndroidViewModel(application) {

    val medicineRepository = MedicineRepository(application)
    private var dispatcher = Dispatchers.IO
    private var liveReminderEvents: List<ReminderEvent> = listOf()
    private var medicineWithReminders: List<MedicineWithReminders> = listOf()
    private var medicine: Medicine? = null
    private val liveData: MutableLiveData<Map<LocalDate, String>> = MutableLiveData()

    fun getEventForDays(
        medicineId: Int,
        pastDays: Long,
        futureDays: Long
    ): LiveData<Map<LocalDate, String>> {
        viewModelScope.launch(dispatcher) {
            liveReminderEvents = medicineRepository.getLastDaysReminderEvents(pastDays.toInt())
            medicineWithReminders = medicineRepository.medicines
            if (medicineId > 0) {
                medicine = medicineRepository.getMedicine(medicineId)
            }
            val dayStrings: MutableMap<LocalDate, String> = mutableMapOf()
            for (deltaDay in -pastDays..futureDays) {
                val day = LocalDate.now().plusDays(deltaDay)
                val eventStrings: MutableList<String> = mutableListOf()
                if (deltaDay <= 0 && pastDays > 0) {
                    eventStrings += getPastEvents(day)
                }
                if (deltaDay >= 0) {
                    eventStrings += getUpcomingEvents(day, medicineId)
                }
                dayStrings[day] = buildDayString(day, eventStrings)
            }
            viewModelScope.launch { liveData.setValue(dayStrings) }
        }
        return liveData
    }

    private fun buildDayString(day: LocalDate, eventStrings: MutableList<String>): String {
        if (eventStrings.isNotEmpty()) {
            eventStrings.addAll(
                0,
                listOf(day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)), "\n")
            )
        }

        return eventStrings.joinToString(separator = "\n")
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
            .filter {
                (medicineId <= 0 || it.medicine.medicineId == medicineId) && isOnDay(
                    day,
                    it.timestamp
                )
            }
            .map { scheduledReminderToString(it) }
    }

    private fun isOnDay(day: LocalDate, timestamp: Instant): Boolean {
        return day == secondsSinceEpochToLocalDate(timestamp.toEpochMilli() / 1000)

    }

    private fun scheduledReminderToString(scheduledReminder: ScheduledReminder): String {
        return TimeHelper.minutesToTimeString(
            getApplication<Application>().applicationContext,
            scheduledReminder.reminder.timeInMinutes.toLong()
        ) +
                ": " + scheduledReminder.reminder.amount + " " + scheduledReminder.medicine.name
    }

    private fun getPastEvents(day: LocalDate): List<String> {
        return liveReminderEvents.filter {
            secondsSinceEpochToLocalDate(it.remindedTimestamp) == day
                    && (medicine == null || medicine?.name == MedicineHelper.normalizeMedicineName(
                it.medicineName
            ))
        }
            .map { reminderEventToString(it, day) }
    }

    private fun reminderEventToString(reminderEvent: ReminderEvent, day: LocalDate): String {
        val timeString = getTimeString(reminderEvent, day)
        var eventString =
            timeString + ": " + reminderEvent.amount + " " + reminderEvent.medicineName
        if (reminderEvent.status == ReminderEvent.ReminderStatus.SKIPPED) {
            eventString += " (" + getApplication<Application>().getString(R.string.skipped) + ")"
        }
        if (reminderEvent.status == ReminderEvent.ReminderStatus.RAISED) {
            eventString += " (?)"
        }
        return eventString
    }

    private fun getTimeString(
        reminderEvent: ReminderEvent,
        day: LocalDate
    ): String {
        val timeStamp =
            if (reminderEvent.processedTimestamp != 0L) reminderEvent.processedTimestamp else reminderEvent.remindedTimestamp
        val timeFunc: (Context, Long) -> String =
            if (isSameDay(timeStamp, day)) {
                TimeHelper::toLocalizedTimeString
            } else {
                TimeHelper::toLocalizedDatetimeString
            }
        return timeFunc(
            getApplication<Application>().applicationContext,
            timeStamp
        )
    }

    private fun isSameDay(secondsSinceEpoch: Long, day: LocalDate): Boolean {
        return secondsSinceEpochToLocalDate(secondsSinceEpoch) == day
    }

    private fun secondsSinceEpochToLocalDate(secondsSinceEpoch: Long): LocalDate {
        return Instant.ofEpochSecond(secondsSinceEpoch).atZone(ZoneId.systemDefault()).toLocalDate()
    }
}