package com.futsch1.medtimer.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

@Entity
@SuppressWarnings("java:S1104")
public class Reminder {
    public static final int DEFAULT_TIME = 480;
    public int medicineRelId;
    @PrimaryKey(autoGenerate = true)
    public int reminderId;
    @Expose
    public int timeInMinutes;
    @ColumnInfo(defaultValue = "0")
    public long createdTimestamp;
    @ColumnInfo(defaultValue = "1")
    @Expose
    public int consecutiveDays;
    @ColumnInfo(defaultValue = "0")
    @Expose
    public int pauseDays;
    @ColumnInfo(defaultValue = "")
    @Expose
    public String instructions;
    @ColumnInfo(defaultValue = "19823") // 10.4.24
    @Expose
    public long cycleStartDay;
    @Expose
    public String amount;
    @ColumnInfo(defaultValue = "[true, true, true, true, true, true, true]")
    @Expose
    public List<Boolean> days;
    @ColumnInfo(defaultValue = "3")
    @Expose
    public int notificationImportance;

    public Reminder(int medicineRelId) {
        timeInMinutes = DEFAULT_TIME;
        amount = "?";
        consecutiveDays = 1;
        pauseDays = 0;
        days = new ArrayList<>(List.of(true, true, true, true, true, true, true));
        this.medicineRelId = medicineRelId;
    }
}
