package com.futsch1.medtimer.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class ReminderEvent {
    @PrimaryKey(autoGenerate = true)
    public int reminderEventId;
    public String medicineName;
    public String amount;
    public ReminderStatus status;
    public long remindedTimestamp;
    public long processedTimestamp;
    public int reminderId;
    @ColumnInfo(defaultValue = "0")
    public int notificationId;

    public enum ReminderStatus {
        RAISED,
        TAKEN,
        SKIPPED
    }
}
