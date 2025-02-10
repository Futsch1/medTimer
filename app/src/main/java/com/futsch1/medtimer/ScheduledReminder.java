package com.futsch1.medtimer;

import com.futsch1.medtimer.database.FullMedicine;
import com.futsch1.medtimer.database.Reminder;

import java.time.Instant;

public record ScheduledReminder(FullMedicine medicine, Reminder reminder, Instant timestamp) {

}
