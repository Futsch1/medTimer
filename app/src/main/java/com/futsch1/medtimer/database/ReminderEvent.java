package com.futsch1.medtimer.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;

import java.util.List;

@Entity
public class ReminderEvent {
    @PrimaryKey(autoGenerate = true)
    public int reminderEventId;
    @Expose
    public String medicineName;
    @Expose
    public String amount;
    @ColumnInfo(defaultValue = "0")
    @Expose
    public int color;
    @ColumnInfo(defaultValue = "false")
    @Expose
    public boolean useColor;
    @Expose
    public ReminderStatus status;
    @Expose
    public long remindedTimestamp;
    @Expose
    public long processedTimestamp;
    @Expose
    public int reminderId;
    @ColumnInfo(defaultValue = "0")
    public int notificationId;
    @ColumnInfo(defaultValue = "0")
    @Expose
    public int iconId;
    @ColumnInfo(defaultValue = "0")
    public int remainingRepeats;
    @ColumnInfo(defaultValue = "false")
    public boolean stockHandled;
    @ColumnInfo(defaultValue = "false")
    public boolean askForAmount;
    @ColumnInfo(defaultValue = "[]")
    @Expose
    public List<String> tags;

    public enum ReminderStatus {
        RAISED,
        TAKEN,
        SKIPPED,
        DELETED
    }
}
