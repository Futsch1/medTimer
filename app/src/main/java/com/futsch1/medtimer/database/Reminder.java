package com.futsch1.medtimer.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
@SuppressWarnings("java:S1104")
public class Reminder {
    public final int medicineRelId;
    @PrimaryKey(autoGenerate = true)
    public int reminderId;
    public int timeInMinutes;
    @ColumnInfo(defaultValue = "0")
    public long createdTimestamp;
    @ColumnInfo(defaultValue = "1")
    public int daysBetweenReminders;
    @ColumnInfo(defaultValue = "")
    public String instructions;

    public String amount;

    public Reminder(int medicineRelId) {
        timeInMinutes = 480;
        amount = "?";
        daysBetweenReminders = 1;
        this.medicineRelId = medicineRelId;
    }
}
