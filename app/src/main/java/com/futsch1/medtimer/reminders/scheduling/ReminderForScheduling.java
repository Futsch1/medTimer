package com.futsch1.medtimer.reminders.scheduling;

import static java.lang.Math.abs;

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
    private final boolean raisedToday;
    private final boolean[] possibleDays;

    public ReminderForScheduling(Reminder reminder, List<ReminderEvent> reminderEventList, ReminderScheduler.TimeAccess timeAccess) {
        this.timeAccess = timeAccess;
        this.reminder = reminder;
        this.raisedToday = isRaisedToday(reminderEventList);
        // Bit map of possible days in the future on where the reminder may be raised
        this.possibleDays = new boolean[31];
    }

    private boolean isRaisedToday(List<ReminderEvent> reminderEventList) {
        for (ReminderEvent reminderEvent : reminderEventList) {
            if (isToday(reminderEvent.remindedTimestamp)) {
                return true;
            }
        }
        return false;
    }

    private boolean isToday(long epochSeconds) {
        return localDateFromEpochSeconds(epochSeconds).toEpochDay() == today();
    }

    private LocalDate localDateFromEpochSeconds(long epochSeconds) {
        return Instant.ofEpochSecond(epochSeconds).atZone(timeAccess.systemZone()).toLocalDate();
    }

    private long today() {
        return timeAccess.localDate().toEpochDay();
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

    private boolean isCyclic() {
        return reminder.pauseDays != 0;
    }

    private void setPossibleDaysByCycle() {
        long cycleStartDay = reminder.cycleStartDay;
        long dayInCycle = today() - cycleStartDay;
        int cycleLength = reminder.consecutiveDays + reminder.pauseDays;
        for (int x = 0; x < possibleDays.length; x++) {
            possibleDays[x] = abs(dayInCycle % cycleLength) < reminder.consecutiveDays && dayInCycle + x >= 0;
            dayInCycle++;
        }
        // Only schedule today if it's not already raised
        possibleDays[0] &= !raisedToday;
    }

    private void canScheduleEveryDay() {
        Arrays.fill(possibleDays, true);
        possibleDays[0] = !createdToday() && !raisedToday;
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

    private boolean createdToday() {
        return isToday(reminder.createdTimestamp);
    }
}
