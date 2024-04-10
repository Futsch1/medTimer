package com.futsch1.medtimer.reminders.scheduling;

import androidx.annotation.Nullable;

import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

public class ReminderForScheduling {
    private final Reminder reminder;
    private final ReminderScheduler.TimeAccess timeAccess;
    private final List<ReminderEvent> reminderEventList;
    private final boolean[] possibleDays;

    public ReminderForScheduling(Reminder reminder, List<ReminderEvent> reminderEventList, ReminderScheduler.TimeAccess timeAccess) {
        this.reminder = reminder;
        this.reminderEventList = reminderEventList;
        this.timeAccess = timeAccess;
        // Bit map of possible days in the future on where the reminder may be raised
        this.possibleDays = new boolean[31];
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
        if (isCyclic()) {
            setPossibleDaysByCycle();
        } else {
            canScheduleEveryDay();
        }

        clearPossibleDaysByWeekday();

        return getEarliestPossibleDate();
    }

    private void canScheduleEveryDay() {
        Arrays.fill(possibleDays, true);
        possibleDays[0] = !createdToday() && notRaisedToday();
    }

    private void setPossibleDaysByCycle() {
        long cycleStartDay = reminder.cycleStartDay;
        long dayInCycle = today() - cycleStartDay;
        int cycleLength = reminder.consecutiveDays + reminder.pauseDays;
        for (int x = 0; x < possibleDays.length; x++) {
            possibleDays[x] = dayInCycle % cycleLength < reminder.consecutiveDays;
            dayInCycle++;
        }
        // Only schedule today if it's not already raised
        possibleDays[0] &= notRaisedToday();
    }

    private void clearPossibleDaysByWeekday() {
        DayOfWeek dayOfWeek = timeAccess.localDate().getDayOfWeek();
        for (int i = 0; i < possibleDays.length; i++) {
            if (Boolean.FALSE.equals(reminder.days.get(dayOfWeek.getValue() - 1))) {
                possibleDays[i] = false;
            }
            dayOfWeek = dayOfWeek.plus(1);
        }
    }

    @Nullable
    private LocalDate getEarliestPossibleDate() {
        for (int i = 0; i < possibleDays.length; i++) {
            if (possibleDays[i]) {
                return timeAccess.localDate().plusDays(i);
            }
        }
        return null;
    }

    private boolean isCyclic() {
        return reminder.pauseDays != 0;
    }

    private long today() {
        return timeAccess.localDate().toEpochDay();
    }

    private boolean notRaisedToday() {
        return neverRaised() || today() != localDateFromEpochSeconds(lastRemindedTimestamp()).toEpochDay();
    }

    private boolean createdToday() {
        return isToday(reminder.createdTimestamp);
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
