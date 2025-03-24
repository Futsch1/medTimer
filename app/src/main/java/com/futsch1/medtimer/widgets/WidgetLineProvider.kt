package com.futsch1.medtimer.widgets

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.text.SpannableStringBuilder
import android.text.Spanned
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.ScheduledReminder
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.formatReminderStringForWidget
import com.futsch1.medtimer.helpers.formatScheduledReminderStringForWidget
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
    ): Spanned
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
    ): Spanned {
        runBlocking {
            job.join()
        }

        val scheduledReminder = scheduledReminders.getOrNull(line)

        return if (scheduledReminder != null) scheduledReminderToString(
            scheduledReminder
        ) else SpannableStringBuilder()
    }

    private fun scheduledReminderToString(
        scheduledReminder: ScheduledReminder
    ): Spanned {
        return formatScheduledReminderStringForWidget(
            context,
            scheduledReminder,
            sharedPreferences!!
        )
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
    ): Spanned {
        runBlocking {
            job.join()
        }
        val reminderEvent = reminderEvents.getOrNull(line)

        return if (reminderEvent != null) reminderEventToString(
            reminderEvent
        ) else SpannableStringBuilder()
    }

    private fun reminderEventToString(
        reminderEvent: ReminderEvent
    ): Spanned {
        return formatReminderStringForWidget(context, reminderEvent, sharedPreferences!!)
    }
}