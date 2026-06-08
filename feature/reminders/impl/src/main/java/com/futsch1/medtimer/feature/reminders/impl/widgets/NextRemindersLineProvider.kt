package com.futsch1.medtimer.feature.reminders.impl.widgets

import android.text.SpannableStringBuilder
import android.text.Spanned
import com.futsch1.medtimer.core.ui.ReminderStringFormatter
import com.futsch1.medtimer.feature.reminders.impl.SimulatedRemindersRepository
import java.time.LocalDate
import javax.inject.Inject

class NextRemindersLineProvider @Inject constructor(
    private val simulatedRemindersRepository: SimulatedRemindersRepository,
    private val reminderStringFormatter: ReminderStringFormatter,
) : WidgetLineProvider {

    override fun getWidgetLine(line: Int, isShort: Boolean): Spanned {
        if (simulatedRemindersRepository.simulatedThrough.value == LocalDate.MIN) {
            simulatedRemindersRepository.triggerCalculation()
        }
        val scheduledReminder =
            simulatedRemindersRepository.simulatedReminders.value.getOrNull(line)

        return if (scheduledReminder != null)
            reminderStringFormatter.formatSimulatedReminderForWidget(scheduledReminder, isShort)
        else SpannableStringBuilder()
    }
}
