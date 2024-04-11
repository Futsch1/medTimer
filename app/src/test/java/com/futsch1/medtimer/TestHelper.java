package com.futsch1.medtimer;

import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TestHelper {
    public static Reminder buildReminder(int medicineId, int reminderId, String amount, int timeInMinutes, int daysBetweenReminders) {
        Reminder reminder = new Reminder(medicineId);
        reminder.reminderId = reminderId;
        reminder.amount = amount;
        reminder.timeInMinutes = timeInMinutes;
        reminder.pauseDays = daysBetweenReminders - 1;
        reminder.consecutiveDays = 1;
        reminder.createdTimestamp = 0;
        reminder.cycleStartDay = 0;
        reminder.days = new ArrayList<>(List.of(true, true, true, true, true, true, true));
        return reminder;
    }

    public static MedicineWithReminders buildMedicineWithReminders(int medicineId, String medicineName) {
        MedicineWithReminders medicineWithReminders = new MedicineWithReminders();
        medicineWithReminders.medicine = new Medicine(medicineName);
        medicineWithReminders.medicine.medicineId = medicineId;
        medicineWithReminders.reminders = new ArrayList<>();
        return medicineWithReminders;
    }

    public static Instant on(long day, long minutes) {
        return Instant.ofEpochSecond((day - 1) * 86400 + minutes * 60);
    }

    public static ReminderEvent buildReminderEvent(int reminderId, long raisedTimestamp) {
        ReminderEvent reminderEvent = new ReminderEvent();
        reminderEvent.reminderId = reminderId;
        reminderEvent.remindedTimestamp = raisedTimestamp;
        return reminderEvent;
    }
}
