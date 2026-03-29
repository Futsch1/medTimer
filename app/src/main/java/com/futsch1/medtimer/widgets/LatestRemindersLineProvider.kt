package com.futsch1.medtimer.widgets

import android.text.SpannableStringBuilder
import android.text.Spanned
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.ReminderStringFormatter
import javax.inject.Inject

class LatestRemindersLineProvider @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val reminderStringFormatter: ReminderStringFormatter
) : WidgetLineProvider {
    private val reminderEvents: List<ReminderEvent> by lazy {
        medicineRepository.getLastDaysReminderEvents(7).reversed()
    }

    override fun getWidgetLine(
        line: Int,
        isShort: Boolean
    ): Spanned {
        val reminderEvent = reminderEvents.getOrNull(line)

        return if (reminderEvent != null)
            reminderStringFormatter.formatReminderForWidget(reminderEvent, isShort)
        else SpannableStringBuilder()
    }
}