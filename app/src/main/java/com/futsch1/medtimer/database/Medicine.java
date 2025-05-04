package com.futsch1.medtimer.database;

import android.graphics.Color;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.futsch1.medtimer.ReminderNotificationChannelManager;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.Objects;

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
    public double amount;
    @ColumnInfo(defaultValue = "0")
    @Expose
    public double outOfStockReminderThreshold;
    @ColumnInfo(defaultValue = "[]")
    @Expose
    public ArrayList<Double> refillSizes;
    @ColumnInfo(defaultValue = "")
    @Expose
    public String unit;
    @ColumnInfo(defaultValue = "1.0")
    @Expose
    public double sortOrder;

    public Medicine(String name) {
        this.name = name;
        this.useColor = false;
        this.color = Color.DKGRAY;
        this.notificationImportance = ReminderNotificationChannelManager.Importance.DEFAULT.getValue();
        this.iconId = 0;
        this.outOfStockReminder = OutOfStockReminderType.OFF;
        this.refillSizes = new ArrayList<>();
        this.unit = "";
        this.sortOrder = 1.0;
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
        this.unit = "";
        this.sortOrder = 1.0;
    }

    public boolean isOutOfStock() {
        return isStockManagementActive() && amount <= outOfStockReminderThreshold;
    }

    public boolean isStockManagementActive() {
        return (amount != 0 || outOfStockReminder != OutOfStockReminderType.OFF || outOfStockReminderThreshold != 0);
    }

    @Override
    public int hashCode() {
        return Objects.hash(medicineId, name, useColor, color, notificationImportance, iconId, outOfStockReminder, amount, outOfStockReminderThreshold, refillSizes, unit);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Medicine that = (Medicine) o;
        return medicineId == that.medicineId &&
                Objects.equals(name, that.name) &&
                useColor == that.useColor &&
                color == that.color &&
                notificationImportance == that.notificationImportance &&
                iconId == that.iconId &&
                outOfStockReminder == that.outOfStockReminder &&
                amount == that.amount &&
                outOfStockReminderThreshold == that.outOfStockReminderThreshold &&
                Objects.equals(refillSizes, that.refillSizes) &&
                Objects.equals(unit, that.unit);
    }

    public enum OutOfStockReminderType {
        OFF,
        ONCE,
        ALWAYS
    }
}
