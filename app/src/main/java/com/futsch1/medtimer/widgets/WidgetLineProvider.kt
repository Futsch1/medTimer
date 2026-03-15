package com.futsch1.medtimer.widgets

import android.app.Application
import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.formatReminderStringForWidget
import com.futsch1.medtimer.helpers.formatScheduledReminderStringForWidget
import com.futsch1.medtimer.preferences.MedTimerPreferencesDataSource
import com.futsch1.medtimer.reminders.ReminderContext
import com.futsch1.medtimer.reminders.TimeAccess
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
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
    val scheduledReminders: List<ScheduledReminder>
    val reminderContext = ReminderContext(context)

    init {
        val medicineRepository = MedicineRepository(context.applicationContext as Application)
        val medicinesWithReminders = medicineRepository.medicines
        val reminderEvents = medicineRepository.getReminderEventsForScheduling(medicinesWithReminders)
        val reminderScheduler = ReminderScheduler(object : TimeAccess {
            override fun systemZone(): ZoneId = ZoneId.systemDefault()
            override fun localDate(): LocalDate = LocalDate.now()
            override fun now(): Instant = Instant.now()
        }, reminderContext.preferencesDataSource)

        scheduledReminders = reminderScheduler.schedule(medicinesWithReminders, reminderEvents)
    }

    override fun getWidgetLine(
        line: Int,
        isShort: Boolean
    ): Spanned {
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
            reminderContext.preferencesDataSource,
            isSmall
        )
    }
}

class LatestRemindersLineProvider(val context: Context) : WidgetLineProvider {
    val reminderEvents: List<ReminderEvent>
    val reminderContext = ReminderContext(context)

    init {
        val medicineRepository = MedicineRepository(context)
        reminderEvents = medicineRepository.getLastDaysReminderEvents(7).reversed()
    }

    override fun getWidgetLine(
        line: Int,
        isShort: Boolean
    ): Spanned {
        val reminderEvent = reminderEvents.getOrNull(line)

        return if (reminderEvent != null) reminderEventToString(
            reminderEvent,
            reminderContext.preferencesDataSource,
            isShort
        ) else SpannableStringBuilder()
    }

    private fun reminderEventToString(
        reminderEvent: ReminderEvent,
        preferencesDataSource: MedTimerPreferencesDataSource,
        isSmall: Boolean
    ): Spanned {
        return formatReminderStringForWidget(context, reminderEvent, preferencesDataSource, isSmall)
    }
}