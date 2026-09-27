package com.futsch1.medtimer.feature.reminders.widgets

import android.text.SpannableStringBuilder
import android.text.Spanned
import com.futsch1.medtimer.core.ui.ReminderStringFormatter
import com.futsch1.medtimer.feature.reminders.SimulatedRemindersRepository
import javax.inject.Inject

class NextRemindersLineProvider @Inject constructor(
    private val simulatedRemindersRepository: SimulatedRemindersRepository,
    private val reminderStringFormatter: ReminderStringFormatter,
) : WidgetLineProvider {

    override suspend fun prepareForUpdate() {
        simulatedRemindersRepository.awaitCalculation()
    }

    override fun getWidgetLine(line: Int, isShort: Boolean): Spanned {
        val scheduledReminder =
            simulatedRemindersRepository.simulatedReminders.value.getOrNull(line)

        return if (scheduledReminder != null)
            reminderStringFormatter.formatSimulatedReminderForWidget(scheduledReminder, isShort)
        else SpannableStringBuilder()
    }
}
