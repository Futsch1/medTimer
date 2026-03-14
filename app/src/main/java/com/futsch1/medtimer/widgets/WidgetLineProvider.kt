package com.futsch1.medtimer.widgets

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.formatReminderStringForWidget
import com.futsch1.medtimer.helpers.formatScheduledReminderStringForWidget
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.TimeAccess
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

fun interface WidgetLineProvider {
    fun getWidgetLine(
        line: Int,
        isShort: Boolean
    ): Spanned
}

class NextRemindersLineProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val medicineRepository: MedicineRepository,
    private val preferencesDataSource: PreferencesDataSource
) : WidgetLineProvider {
    private val scheduledReminders: List<ScheduledReminder> by lazy {
        val medicinesWithReminders = medicineRepository.medicines
        val reminderEvents = medicineRepository.getReminderEventsForScheduling(medicinesWithReminders)
        val reminderScheduler = ReminderScheduler(object : TimeAccess {
            override fun systemZone(): ZoneId = ZoneId.systemDefault()
            override fun localDate(): LocalDate = LocalDate.now()
            override fun now(): Instant = Instant.now()
        }, preferencesDataSource)

        reminderScheduler.schedule(medicinesWithReminders, reminderEvents)
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
            preferencesDataSource,
            isSmall
        )
    }
}

class LatestRemindersLineProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesDataSource: PreferencesDataSource,
    private val medicineRepository: MedicineRepository
) : WidgetLineProvider {
    private val reminderEvents: List<ReminderEvent> by lazy {
        medicineRepository.getLastDaysReminderEvents(7).reversed()
    }

    override fun getWidgetLine(
        line: Int,
        isShort: Boolean
    ): Spanned {
        val reminderEvent = reminderEvents.getOrNull(line)

        return if (reminderEvent != null) reminderEventToString(
            reminderEvent,
            preferencesDataSource,
            isShort
        ) else SpannableStringBuilder()
    }

    private fun reminderEventToString(
        reminderEvent: ReminderEvent,
        preferencesDataSource: PreferencesDataSource,
        isSmall: Boolean
    ): Spanned {
        return formatReminderStringForWidget(context, reminderEvent, preferencesDataSource, isSmall)
    }
}
