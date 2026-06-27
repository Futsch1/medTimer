package com.futsch1.medtimer.feature.reminders.widgets

import android.text.SpannableStringBuilder
import android.text.Spanned
import com.futsch1.medtimer.core.ui.ReminderStringFormatter
import com.futsch1.medtimer.feature.reminders.FutureRemindersRepository
import java.time.LocalDate
import javax.inject.Inject

class NextRemindersLineProvider @Inject constructor(
    private val futureRemindersRepository: FutureRemindersRepository,
    private val reminderStringFormatter: ReminderStringFormatter,
) : WidgetLineProvider {

    override fun getWidgetLine(line: Int, isShort: Boolean): Spanned {
        if (futureRemindersRepository.simulatedThrough.value == LocalDate.MIN) {
            futureRemindersRepository.triggerCalculation()
        }
        val scheduledReminder =
            futureRemindersRepository.simulatedReminders.value.getOrNull(line)

        return if (scheduledReminder != null)
            reminderStringFormatter.formatProcessedReminderForWidget(scheduledReminder, isShort)
        else SpannableStringBuilder()
    }
}
