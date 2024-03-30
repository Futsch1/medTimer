package com.futsch1.medtimer.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;

@Entity
@SuppressWarnings("java:S1104")
public class Reminder {
    public final int medicineRelId;
    @PrimaryKey(autoGenerate = true)
    public int reminderId;
    @Expose
    public int timeInMinutes;
    @ColumnInfo(defaultValue = "0")
    @Expose
    public long createdTimestamp;
    @ColumnInfo(defaultValue = "1")
    @Expose
    public int daysBetweenReminders;
    @ColumnInfo(defaultValue = "")
    @Expose
    public String instructions;

    @Expose
    public String amount;

    public Reminder(int medicineRelId) {
        timeInMinutes = 480;
        amount = "?";
        daysBetweenReminders = 1;
        this.medicineRelId = medicineRelId;
    }
}
