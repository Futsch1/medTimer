package com.futsch1.medtimer.reminders.scheduling

import android.annotation.SuppressLint
import com.futsch1.medtimer.database.Reminder
import java.time.LocalDate

object CyclesHelper {
    @SuppressLint("DefaultLocale")
    fun getCycleCountString(reminder: Reminder): String {
        if (reminder.pauseDays == 0 || reminder.consecutiveDays == 1) {
            return ""
        }

        val cycleStartDay = reminder.cycleStartDay
        val dayInCycle = LocalDate.now().toEpochDay() - cycleStartDay
        val cycleLength = reminder.consecutiveDays + reminder.pauseDays

        return String.format(" (%d/%d)", dayInCycle % cycleLength + 1, reminder.consecutiveDays)
    }
}
