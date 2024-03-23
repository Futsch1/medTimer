package com.futsch1.medtimer.helpers;

import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.database.Reminder;

import java.util.Comparator;
import java.util.List;

public class MedicineHelper {
    private MedicineHelper() {
        // Intentionally empty
    }

    public static int getMaxDaysBetweenReminders(List<MedicineWithReminders> medicineWithReminders) {
        int maxDaysBetweenReminders = 1;
        for (MedicineWithReminders medicineWithReminder : medicineWithReminders) {
            int daysBetweenReminders = medicineWithReminder.reminders.stream().max(Comparator.comparingInt(v -> v.daysBetweenReminders)).orElse(new Reminder(0)).daysBetweenReminders;
            if (daysBetweenReminders > maxDaysBetweenReminders) {
                maxDaysBetweenReminders = daysBetweenReminders;
            }
        }
        return maxDaysBetweenReminders;
    }
}
