package com.futsch1.medtimer.widgets

import android.app.Application
import android.content.Context
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
import java.time.Instant
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
        val dayString = getDayString(context, scheduledReminder.timestamp.epochSecond)
        return dayString + TimeHelper.toLocalizedTimeString(
            context.applicationContext as Application?,
            scheduledReminder.timestamp.epochSecond
        ) +
                ": " + scheduledReminder.reminder.amount + " " + scheduledReminder.medicine.name
    }
}

class LatestRemindersLineProvider(val context: Context) : WidgetLineProvider {
    lateinit var reminderEvents: List<ReminderEvent>
    private val job: Job = CoroutineScope(SupervisorJob()).launch {
        val medicineRepository = MedicineRepository(context.applicationContext as Application?)
        reminderEvents = medicineRepository.getLastDaysReminderEvents(7).reversed()
    }

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
        val dayString = getDayString(context, reminderEvent.remindedTimestamp)
        return dayString + TimeHelper.toLocalizedTimeString(
            context.applicationContext as Application?,
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

fun getDayString(context: Context, timestamp: Long): String {
    val reminderDate = Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    return if (reminderDate == LocalDate.now()) {
        ""
    } else {
        TimeHelper.toLocalizedDateString(
            context,
            timestamp
        ) + " "
    }
}