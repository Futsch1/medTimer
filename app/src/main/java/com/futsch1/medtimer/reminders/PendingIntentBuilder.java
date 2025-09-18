package com.futsch1.medtimer.reminders;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.time.LocalDateTime;

public class PendingIntentBuilder {
    private final Context context;
    private int reminderId = 0;
    private int reminderEventId = 0;
    private LocalDateTime reminderDateTime = null;

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

    public PendingIntentBuilder setReminderDateTime(LocalDateTime reminderDateTime) {
        this.reminderDateTime = reminderDateTime;

        return this;
    }

    public PendingIntent build() {
        Intent reminderIntent = ReminderProcessor.getReminderAction(context, reminderId, reminderEventId, reminderDateTime);
        // Use the reminderEventId as request code to ensure unique PendingIntent for each reminder event
        return PendingIntent.getBroadcast(context, reminderEventId, reminderIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
