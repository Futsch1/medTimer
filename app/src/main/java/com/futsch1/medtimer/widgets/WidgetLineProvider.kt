package com.futsch1.medtimer.widgets

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.text.SpannableStringBuilder
import android.text.Spanned
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.formatReminderStringForWidget
import com.futsch1.medtimer.helpers.formatScheduledReminderStringForWidget
import com.futsch1.medtimer.reminders.TimeAccess
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
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
        line: Int,
        isShort: Boolean
    ): Spanned
}

class NextRemindersLineProvider(val context: Context) : WidgetLineProvider {
    lateinit var scheduledReminders: List<ScheduledReminder>
    private val job: Job = CoroutineScope(SupervisorJob()).launch {
        val medicineRepository = MedicineRepository(context.applicationContext as Application?)
        val medicinesWithReminders = medicineRepository.medicines
        val reminderEvents = medicineRepository.getReminderEventsForScheduling(medicinesWithReminders)
        val reminderScheduler = ReminderScheduler(object : TimeAccess {
            override fun systemZone(): ZoneId = ZoneId.systemDefault()
            override fun localDate(): LocalDate = LocalDate.now()
            override fun now(): Instant = Instant.now()
        }, PreferenceManager.getDefaultSharedPreferences(context))

        scheduledReminders = reminderScheduler.schedule(medicinesWithReminders, reminderEvents)
    }
    val sharedPreferences: SharedPreferences? =
        PreferenceManager.getDefaultSharedPreferences(context)

    override fun getWidgetLine(
        line: Int,
        isShort: Boolean
    ): Spanned {
        runBlocking {
            job.join()
        }

        val scheduledReminder = scheduledReminders.getOrNull(line)

        return if (scheduledReminder != null) scheduledReminderToString(
            scheduledReminder,
            isShort
        ) else SpannableStringBuilder()
    }

    private fun scheduledReminderToString(
        scheduledReminder: ScheduledReminder,
        isSmall: Boolean
    ): Spanned {
        return formatScheduledReminderStringForWidget(
            context,
            scheduledReminder,
            sharedPreferences!!,
            isSmall
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
        line: Int,
        isShort: Boolean
    ): Spanned {
        runBlocking {
            job.join()
        }
        val reminderEvent = reminderEvents.getOrNull(line)

        return if (reminderEvent != null) reminderEventToString(
            reminderEvent,
            isShort
        ) else SpannableStringBuilder()
    }

    private fun reminderEventToString(
        reminderEvent: ReminderEvent,
        isSmall: Boolean
    ): Spanned {
        return formatReminderStringForWidget(context, reminderEvent, sharedPreferences!!, isSmall)
    }
}