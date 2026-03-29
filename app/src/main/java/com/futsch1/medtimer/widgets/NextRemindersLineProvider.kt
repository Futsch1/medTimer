package com.futsch1.medtimer.widgets

import android.text.SpannableStringBuilder
import android.text.Spanned
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.helpers.ReminderStringFormatter
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.TimeAccess
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
import javax.inject.Inject

class NextRemindersLineProvider @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val preferencesDataSource: PreferencesDataSource,
    private val timeAccess: TimeAccess,
    private val reminderStringFormatter: ReminderStringFormatter
) : WidgetLineProvider {
    private val scheduledReminders: List<ScheduledReminder> by lazy {
        val medicinesWithReminders = medicineRepository.medicines
        val reminderEvents = medicineRepository.getReminderEventsForScheduling(medicinesWithReminders)
        val reminderScheduler = ReminderScheduler(timeAccess, preferencesDataSource)

        reminderScheduler.schedule(medicinesWithReminders, reminderEvents)
    }

    override fun getWidgetLine(
        line: Int,
        isShort: Boolean
    ): Spanned {
        val scheduledReminder = scheduledReminders.getOrNull(line)

        return if (scheduledReminder != null)
            reminderStringFormatter.formatScheduledReminderForWidget(scheduledReminder, isShort)
        else SpannableStringBuilder()
    }
}