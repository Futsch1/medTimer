package com.futsch1.medtimer.widgets

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.R
import com.futsch1.medtimer.ScheduledReminder
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.ZoneId

fun interface WidgetLineProvider {
    fun getWidgetLine(
        line: Int
    ): String
}

class NextRemindersLineProvider(val context: Context) : WidgetLineProvider {
    lateinit var scheduledReminders: List<ScheduledReminder>
    private val job: Job = CoroutineScope(SupervisorJob()).launch {
        val medicineRepository = MedicineRepository(context.applicationContext as Application?)
        val medicinesWithReminders = medicineRepository.medicines
        val reminderEvents = medicineRepository.allReminderEventsWithoutDeleted
        val reminderScheduler = ReminderScheduler(object : ReminderScheduler.TimeAccess {
            override fun systemZone(): ZoneId {
                return ZoneId.systemDefault()
            }

            override fun localDate(): LocalDate {
                return LocalDate.now()
            }
        })

        scheduledReminders = reminderScheduler.schedule(medicinesWithReminders, reminderEvents)
    }
    val sharedPreferences: SharedPreferences? =
        PreferenceManager.getDefaultSharedPreferences(context)

    override fun getWidgetLine(
        line: Int
    ): String {
        runBlocking {
            job.join()
        }

        val scheduledReminder = scheduledReminders.getOrNull(line)

        return if (scheduledReminder != null) scheduledReminderToString(
            scheduledReminder
        ) else ""
    }

    private fun scheduledReminderToString(
        scheduledReminder: ScheduledReminder
    ): String {
        return TimeHelper.toConfigurableDateTimeString(
            context.applicationContext as Application?,
            sharedPreferences,
            scheduledReminder.timestamp.epochSecond
        ) +
                ": " + scheduledReminder.reminder.amount + " " + scheduledReminder.medicine.medicine.name
    }
}

class LatestRemindersLineProvider(val context: Context) : WidgetLineProvider {
    lateinit var reminderEvents: List<ReminderEvent>
    private val job: Job = CoroutineScope(SupervisorJob()).launch {
        val medicineRepository = MedicineRepository(context.applicationContext as Application?)
        reminderEvents = medicineRepository.getLastDaysReminderEvents(7).reversed()
    }
    val sharedPreferences: SharedPreferences? =
        PreferenceManager.getDefaultSharedPreferences(context)

    override fun getWidgetLine(
        line: Int
    ): String {
        runBlocking {
            job.join()
        }
        val reminderEvent = reminderEvents.getOrNull(line)

        return if (reminderEvent != null) reminderEventToString(
            reminderEvent
        ) else ""
    }

    private fun reminderEventToString(
        reminderEvent: ReminderEvent
    ): String {
        return TimeHelper.toConfigurableDateTimeString(
            context.applicationContext as Application?,
            sharedPreferences,
            reminderEvent.remindedTimestamp
        ) + ": " + statusToString(reminderEvent.status) +
                reminderEvent.amount + " " + reminderEvent.medicineName
    }

    private fun statusToString(status: ReminderEvent.ReminderStatus?): String {
        return when (status) {
            ReminderEvent.ReminderStatus.TAKEN -> context.getString(R.string.taken) + " "
            ReminderEvent.ReminderStatus.SKIPPED -> context.getString(R.string.skipped) + " "
            else -> ""
        }
    }
}