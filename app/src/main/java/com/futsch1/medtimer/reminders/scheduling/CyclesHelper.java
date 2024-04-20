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
        if (reminder.pauseDays != 0) {
            long cycleStartDay = reminder.cycleStartDay;
            long dayInCycle = LocalDate.now().toEpochDay() - cycleStartDay;
            int cycleLength = reminder.consecutiveDays + reminder.pauseDays;

            return String.format(" (%d/%d)", dayInCycle % cycleLength + 1, reminder.consecutiveDays);
        } else {
            return "";
        }
    }
}
