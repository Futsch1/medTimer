package com.futsch1.medtimer.schedulertests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.futsch1.medtimer.database.FullMedicine;
import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class TestHelper {
    private TestHelper() {
        // Hide implicit public constructor
    }

    public static Reminder buildReminder(int medicineId, int reminderId, String amount, int timeInMinutes, int daysBetweenReminders) {
        Reminder reminder = new Reminder(medicineId);
        reminder.setReminderId(reminderId);
        reminder.setAmount(amount);
        reminder.setTimeInMinutes(timeInMinutes);
        reminder.setPauseDays(daysBetweenReminders - 1);
        reminder.setConsecutiveDays(1);
        reminder.setCreatedTimestamp(0);
        reminder.setCycleStartDay(0);
        reminder.setDays(new ArrayList<>(List.of(true, true, true, true, true, true, true)));
        return reminder;
    }

    public static FullMedicine buildFullMedicine(int medicineId, String medicineName) {
        FullMedicine medicineWithReminders = new FullMedicine();
        medicineWithReminders.setMedicine(new Medicine(medicineName));
        medicineWithReminders.getMedicine().setMedicineId(medicineId);
        medicineWithReminders.setReminders(new ArrayList<>());
        return medicineWithReminders;
    }

    public static LocalDate on(long day) {
        return LocalDate.ofEpochDay(day - 1);
    }

    public static Instant on(long day, long minutes) {
        return Instant.ofEpochSecond((day - 1) * 86400 + minutes * 60);
    }

    public static Instant onTZ(long day, long minutes, String zoneId) {
        LocalDate localDate = LocalDate.ofEpochDay(day - 1);
        LocalTime localTime = LocalTime.of((int) minutes / 60, (int) minutes % 60);
        return Instant.ofEpochSecond(localDate.atTime(localTime).atZone(ZoneId.of(zoneId)).toEpochSecond());
    }

    public static ReminderEvent buildReminderEvent(int reminderId, long remindedTimestamp) {
        ReminderEvent reminderEvent = new ReminderEvent();
        reminderEvent.setReminderId(reminderId);
        reminderEvent.setRemindedTimestamp(remindedTimestamp);
        return reminderEvent;
    }

    public static ReminderEvent buildReminderEvent(int reminderId, long remindedTimestamp, int reminderEventId) {
        ReminderEvent reminderEvent = new ReminderEvent();
        reminderEvent.setReminderId(reminderId);
        reminderEvent.setRemindedTimestamp(remindedTimestamp);
        reminderEvent.setReminderEventId(reminderEventId);
        return reminderEvent;
    }

    public static void assertReminded(List<ScheduledReminder> scheduledReminders, Instant timestamp, Medicine medicine, Reminder reminder) {
        assertRemindedAtIndex(scheduledReminders, timestamp, medicine, reminder, 0);
    }

    public static void assertRemindedAtIndex(List<ScheduledReminder> scheduledReminders, Instant timestamp, Medicine medicine, Reminder reminder, int index) {
        assertTrue(scheduledReminders.size() > index);
        assertEquals(timestamp, scheduledReminders.get(index).timestamp());
        assertEquals(medicine, scheduledReminders.get(index).medicine().getMedicine());
        assertEquals(reminder, scheduledReminders.get(index).reminder());
    }

}
