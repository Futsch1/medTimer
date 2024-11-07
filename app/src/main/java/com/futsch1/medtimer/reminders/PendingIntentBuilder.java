package com.futsch1.medtimer.reminders;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.time.LocalDate;

public class PendingIntentBuilder {
    private final Context context;
    private int reminderId;
    private int reminderEventId;
    private LocalDate reminderDate = null;

    public PendingIntentBuilder(Context context) {
        this.context = context;
    }

    public PendingIntentBuilder setReminderId(int reminderId) {
        this.reminderId = reminderId;
        return this;
    }

    public PendingIntentBuilder setReminderEventId(int reminderEventId) {
        this.reminderEventId = reminderEventId;
        return this;
    }

    public PendingIntentBuilder setReminderDate(LocalDate reminderDate) {
        this.reminderDate = reminderDate;
        return this;
    }

    public PendingIntent build() {
        Intent reminderIntent = ReminderProcessor.getReminderAction(context, reminderId, reminderEventId, reminderDate);
        // Use the reminderEventId as request code to ensure unique PendingIntent for each reminder event
        return PendingIntent.getBroadcast(context, reminderEventId, reminderIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
