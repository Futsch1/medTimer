package com.futsch1.medtimer;

import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.Reminder;

import java.time.Instant;

public record ScheduledReminder(Medicine medicine, Reminder reminder, Instant timestamp) {

}
