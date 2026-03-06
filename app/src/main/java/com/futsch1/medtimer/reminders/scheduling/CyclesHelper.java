package com.futsch1.medtimer.reminders.scheduling;

import android.annotation.SuppressLint;

import com.futsch1.medtimer.database.Reminder;

import java.time.LocalDate;

public class CyclesHelper {

    private CyclesHelper() {
        // Intentionally empty
    }

    @SuppressLint("DefaultLocale")
    public static String getCycleCountString(Reminder reminder) {
        if (reminder.getPauseDays() != 0 && reminder.getConsecutiveDays() != 1) {
            long cycleStartDay = reminder.getCycleStartDay();
            long dayInCycle = LocalDate.now().toEpochDay() - cycleStartDay;
            int cycleLength = reminder.getConsecutiveDays() + reminder.getPauseDays();

            return String.format(" (%d/%d)", dayInCycle % cycleLength + 1, reminder.getConsecutiveDays());
        } else {
            return "";
        }
    }
}
