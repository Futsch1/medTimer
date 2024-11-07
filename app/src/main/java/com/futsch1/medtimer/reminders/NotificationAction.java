package com.futsch1.medtimer.reminders;

import android.app.AlarmManager;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.util.Log;

import com.futsch1.medtimer.LogTags;
import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.ReminderEvent;

import java.time.Instant;

public class NotificationAction {
    private NotificationAction() {
        // Intentionally empty
    }

    static void processNotification(Context context, int reminderEventId, ReminderEvent.ReminderStatus status) {
        MedicineRepository medicineRepository = new MedicineRepository((Application) context);
        ReminderEvent reminderEvent = medicineRepository.getReminderEvent(reminderEventId);

        if (reminderEvent != null) {
            cancelNotification(context, reminderEvent.notificationId);
            cancelPendingAlarms(context, reminderEventId);

            reminderEvent.status = status;
            reminderEvent.processedTimestamp = Instant.now().getEpochSecond();
            medicineRepository.updateReminderEvent(reminderEvent);
            Log.i(LogTags.REMINDER, String.format("%s reminder %d for %s",
                    status == ReminderEvent.ReminderStatus.TAKEN ? "Taken" : "Dismissed",
                    reminderEvent.reminderEventId,
                    reminderEvent.medicineName));
        }

    }

    public static void cancelNotification(Context context, int notificationId) {
        if (notificationId != 0) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            Log.d(LogTags.REMINDER, String.format("Cancel notification %d", notificationId));
            notificationManager.cancel(notificationId);
        }
    }

    public static void cancelPendingAlarms(Context context, int reminderEventId) {
        PendingIntent snoozePendingIntent = new PendingIntentBuilder(context).
                setReminderEventId(reminderEventId).
                build();
        Log.d(LogTags.REMINDER, String.format("Cancel all pending alarms for %d", reminderEventId));
        context.getSystemService(AlarmManager.class).cancel(snoozePendingIntent);
    }
}
