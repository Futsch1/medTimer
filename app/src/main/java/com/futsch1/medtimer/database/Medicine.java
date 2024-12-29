package com.futsch1.medtimer.database;

import android.graphics.Color;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.futsch1.medtimer.ReminderNotificationChannelManager;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;

@SuppressWarnings("java:S1319")
@Entity
public class Medicine {

    @ColumnInfo(name = "medicineName")
    @Expose
    public String name;
    @PrimaryKey(autoGenerate = true)
    public int medicineId;

    @ColumnInfo(defaultValue = "0xFFFF0000")
    @Expose
    public int color;
    @ColumnInfo(defaultValue = "false")
    @Expose
    public boolean useColor;
    @ColumnInfo(defaultValue = "3")
    @Expose
    public int notificationImportance;
    @ColumnInfo(defaultValue = "0")
    @Expose
    public int iconId;
    @ColumnInfo(defaultValue = "OFF")
    @Expose
    public OutOfStockReminderType outOfStockReminder;
    @ColumnInfo(defaultValue = "0")
    @Expose
    public int amount;
    @ColumnInfo(defaultValue = "0")
    @Expose
    public int outOfStockReminderThreshold;
    @ColumnInfo(defaultValue = "[]")
    @Expose
    public ArrayList<Integer> refillSizes;

    public Medicine(String name) {
        this.name = name;
        this.useColor = false;
        this.color = Color.DKGRAY;
        this.notificationImportance = ReminderNotificationChannelManager.Importance.DEFAULT.getValue();
        this.iconId = 0;
        this.outOfStockReminder = OutOfStockReminderType.OFF;
        this.refillSizes = new ArrayList<>();
    }

    public Medicine(String name, int id) {
        this.name = name;
        this.medicineId = id;
        this.useColor = false;
        this.color = Color.DKGRAY;
        this.notificationImportance = ReminderNotificationChannelManager.Importance.DEFAULT.getValue();
        this.iconId = 0;
        this.outOfStockReminder = OutOfStockReminderType.OFF;
        this.refillSizes = new ArrayList<>();
    }

    public boolean isStockManagementActive() {
        return (amount != 0 || outOfStockReminder != OutOfStockReminderType.OFF || outOfStockReminderThreshold != 0);
    }

    public enum OutOfStockReminderType {
        OFF,
        ONCE,
        ALWAYS
    }
}
