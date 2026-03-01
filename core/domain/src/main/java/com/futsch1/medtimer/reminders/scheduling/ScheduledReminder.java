package com.futsch1.medtimer.reminders.scheduling;

import com.futsch1.medtimer.database.FullMedicine;
import com.futsch1.medtimer.database.Reminder;

import java.time.Instant;

public record ScheduledReminder(FullMedicine medicine, Reminder reminder, Instant timestamp) {

}
