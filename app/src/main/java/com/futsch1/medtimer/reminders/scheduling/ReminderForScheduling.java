package com.futsch1.medtimer.reminders.scheduling;

import androidx.annotation.Nullable;

import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class ReminderForScheduling {
    private final Reminder reminder;
    private final ReminderScheduler.TimeAccess timeAccess;
    private final List<ReminderEvent> reminderEventList;

    public ReminderForScheduling(Reminder reminder, List<ReminderEvent> reminderEventList, ReminderScheduler.TimeAccess timeAccess) {
        this.reminder = reminder;
        this.reminderEventList = reminderEventList;
        this.timeAccess = timeAccess;
    }

    public @Nullable Instant getNextScheduledTime() {
        LocalDate nextScheduledDate = getNextScheduledDate();
        if (nextScheduledDate != null) {
            return nextScheduledDate.atTime(LocalTime.ofSecondOfDay(reminder.timeInMinutes * 60L)).atZone(timeAccess.systemZone()).toInstant();
        } else {
            return null;
        }
    }

    private LocalDate getNextScheduledDate() {
        // Bit map of possible days in the future on where the reminder may be raised
        boolean[] possibleDays = new boolean[31];
        // First, check if today it can be raised
        long day = getCycleStart();
        long today = today();
        while (day < today + 31) {
            if (day >= today) {
                possibleDays[(int) (day - today)] = true;
            }
            day += reminder.daysBetweenReminders;
        }
        // Find earliest flag set
        for (int i = 0; i < possibleDays.length; i++) {
            if (possibleDays[i]) {
                return timeAccess.localDate().plusDays(i);
            }
        }
        return null;
    }

    private long getCycleStart() {
        if (createdToday()) {
            return tomorrow();
        } else if (neverRaised()) {
            return today();
        } else {
            return localDateFromEpochSeconds(lastRemindedTimestamp()).plusDays(reminder.daysBetweenReminders).toEpochDay();
        }
    }

    private long today() {
        return timeAccess.localDate().toEpochDay();
    }

    private boolean createdToday() {
        return isToday(reminder.createdTimestamp);
    }

    private long tomorrow() {
        return timeAccess.localDate().plusDays(1).toEpochDay();
    }

    private boolean neverRaised() {
        return reminderEventList.isEmpty();
    }

    private LocalDate localDateFromEpochSeconds(long epochSeconds) {
        return Instant.ofEpochSecond(epochSeconds).atZone(timeAccess.systemZone()).toLocalDate();
    }

    private long lastRemindedTimestamp() {
        return reminderEventList.get(reminderEventList.size() - 1).remindedTimestamp;
    }

    private boolean isToday(long epochSeconds) {
        return localDateFromEpochSeconds(epochSeconds).toEpochDay() == today();
    }
}
