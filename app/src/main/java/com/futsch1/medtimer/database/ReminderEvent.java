package com.futsch1.medtimer.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class ReminderEvent {
    @PrimaryKey(autoGenerate = true)
    public int reminderEventId;
    public String medicineName;
    public String amount;
    public ReminderStatus status;
    public long raisedTimestamp;
    public long processedTimestamp;
    public int reminderId;

    enum ReminderStatus {
        RAISED,
        TAKEN,
        SKIPPED
    }
}
