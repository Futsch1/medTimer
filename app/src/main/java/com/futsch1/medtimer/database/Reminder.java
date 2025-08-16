package com.futsch1.medtimer.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@SuppressWarnings("java:S1104")
public class Reminder {
    public static final int DEFAULT_TIME = 480;
    public int medicineRelId;
    @PrimaryKey(autoGenerate = true)
    @Expose
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
    @ColumnInfo(defaultValue = "true")
    @Expose
    public boolean active;
    @ColumnInfo(defaultValue = "0")
    @Expose
    public long periodStart;
    @ColumnInfo(defaultValue = "0")
    @Expose
    public long periodEnd;
    @ColumnInfo(defaultValue = "0xFFFFFFFF")
    @Expose
    public int activeDaysOfMonth;
    @ColumnInfo(defaultValue = "0")
    @Expose
    public int linkedReminderId;
    @ColumnInfo(defaultValue = "0")
    @Expose
    public long intervalStart;
    @ColumnInfo(defaultValue = "false")
    @Expose
    public boolean intervalStartsFromProcessed;
    @ColumnInfo(defaultValue = "false")
    @Expose
    public boolean variableAmount;
    @ColumnInfo(defaultValue = "false")
    @Expose
    public boolean automaticallyTaken;

    public Reminder(int medicineRelId) {
        timeInMinutes = DEFAULT_TIME;
        amount = "?";
        consecutiveDays = 1;
        pauseDays = 0;
        days = new ArrayList<>(List.of(true, true, true, true, true, true, true));
        active = true;
        this.medicineRelId = medicineRelId;
        activeDaysOfMonth = 0xFFFFFFFF;
        periodStart = 0;
        periodEnd = 0;
        linkedReminderId = 0;
        intervalStart = 0;
        intervalStartsFromProcessed = false;
        variableAmount = false;
    }

    public ReminderType getReminderType() {
        if (linkedReminderId != 0) {
            return ReminderType.LINKED;
        } else if (intervalStart != 0) {
            return ReminderType.INTERVAL_BASED;
        } else {
            return ReminderType.TIME_BASED;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        return membersEqual((Reminder) o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reminderId, medicineRelId, timeInMinutes, createdTimestamp, consecutiveDays, pauseDays, instructions, cycleStartDay, amount, days, active, periodStart, periodEnd, activeDaysOfMonth, linkedReminderId, intervalStart, intervalStartsFromProcessed, variableAmount);
    }

    private boolean membersEqual(Reminder that) {
        return reminderId == that.reminderId &&
                medicineRelId == that.medicineRelId &&
                timeInMinutes == that.timeInMinutes &&
                createdTimestamp == that.createdTimestamp &&
                consecutiveDays == that.consecutiveDays &&
                pauseDays == that.pauseDays &&
                Objects.equals(instructions, that.instructions) &&
                cycleStartDay == that.cycleStartDay &&
                Objects.equals(amount, that.amount) &&
                Objects.equals(days, that.days) &&
                active == that.active &&
                periodStart == that.periodStart &&
                periodEnd == that.periodEnd &&
                activeDaysOfMonth == that.activeDaysOfMonth &&
                linkedReminderId == that.linkedReminderId &&
                intervalStart == that.intervalStart &&
                intervalStartsFromProcessed == that.intervalStartsFromProcessed &&
                variableAmount == that.variableAmount;
    }

    public enum ReminderType {
        TIME_BASED,
        INTERVAL_BASED,
        LINKED
    }
}
