package com.futsch1.medtimer.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;

import java.util.List;
import java.util.Objects;

@Entity(indices = {@Index(value = "reminderId"), @Index("remindedTimestamp")})
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
    @ColumnInfo(defaultValue = "0")
    @Expose
    public int lastIntervalReminderTimeInMinutes;
    @ColumnInfo(defaultValue = "")
    @Expose
    public String notes;

    public ReminderEvent() {
        medicineName = "";
        amount = "";
        color = 0;
        useColor = false;
        status = ReminderStatus.RAISED;
        remindedTimestamp = 0;
        processedTimestamp = 0;
        reminderId = 0;
        notificationId = 0;
        iconId = 0;
        remainingRepeats = 0;
        stockHandled = false;
        askForAmount = false;
        tags = List.of();
        lastIntervalReminderTimeInMinutes = 0;
        notes = "";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        return membersEqual((ReminderEvent) o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reminderEventId, medicineName, amount, color, useColor, status, remindedTimestamp, processedTimestamp, reminderId, notificationId, iconId, remainingRepeats, stockHandled, askForAmount, tags, lastIntervalReminderTimeInMinutes, notes);
    }

    private boolean membersEqual(ReminderEvent o) {
        return reminderEventId == o.reminderEventId &&
                Objects.equals(medicineName, o.medicineName) &&
                Objects.equals(amount, o.amount) &&
                color == o.color &&
                useColor == o.useColor &&
                status == o.status &&
                remindedTimestamp == o.remindedTimestamp &&
                processedTimestamp == o.processedTimestamp &&
                reminderId == o.reminderId &&
                notificationId == o.notificationId &&
                iconId == o.iconId &&
                remainingRepeats == o.remainingRepeats &&
                stockHandled == o.stockHandled &&
                askForAmount == o.askForAmount &&
                Objects.equals(tags, o.tags) &&
                lastIntervalReminderTimeInMinutes == o.lastIntervalReminderTimeInMinutes &&
                Objects.equals(notes, o.notes);
    }

    public enum ReminderStatus {
        RAISED,
        TAKEN,
        SKIPPED,
        DELETED
    }
}
