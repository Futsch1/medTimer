package com.futsch1.medtimer.reminders.scheduling;

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

    public Instant getNextScheduledTime() {
        LocalDate nextScheduledDate = getNextScheduledDate();
        return nextScheduledDate.atTime(LocalTime.ofSecondOfDay(reminder.timeInMinutes * 60L)).atZone(timeAccess.systemZone()).toInstant();
    }

    private LocalDate getNextScheduledDate() {
        if (neverRaised()) {
            return createdToday() ? tomorrow() : today();
        }
        if (reminder.daysBetweenReminders == 1) {
            // Every day
            if (raisedToday() || createdToday()) {
                return tomorrow();
            }
            return today();
        } else {
            // Days between reminders
            if (createdToday()) {
                return tomorrow();
            }
            LocalDate lastRaised = localDateFromEpochSeconds(lastRemindedTimestamp());
            return lastRaised.plusDays(reminder.daysBetweenReminders);
        }
    }

    private LocalDate localDateFromEpochSeconds(long epochSeconds) {
        return Instant.ofEpochSecond(epochSeconds).atZone(timeAccess.systemZone()).toLocalDate();
    }

    private LocalDate today() {
        return timeAccess.localDate();
    }

    private LocalDate tomorrow() {
        return timeAccess.localDate().plusDays(1);
    }

    private boolean createdToday() {
        return isToday(reminder.createdTimestamp);
    }

    private boolean neverRaised() {
        return reminderEventList.isEmpty();
    }

    private boolean raisedToday() {
        // Get last event instant
        return isToday(lastRemindedTimestamp());
    }

    private long lastRemindedTimestamp() {
        return reminderEventList.get(reminderEventList.size() - 1).remindedTimestamp;
    }

    private boolean isToday(long epochSeconds) {
        return localDateFromEpochSeconds(epochSeconds).isEqual(today());
    }
}
