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
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        MedicineRepository medicineRepository = new MedicineRepository((Application) context);
        ReminderEvent reminderEvent = medicineRepository.getReminderEvent(reminderEventId);

        if (reminderEvent != null) {
            cancelNotification(reminderEvent, notificationManager);
            cancelSnoozeAlarm(context, reminderEventId, reminderEvent);

            reminderEvent.status = status;
            reminderEvent.processedTimestamp = Instant.now().getEpochSecond();
            medicineRepository.updateReminderEvent(reminderEvent);
            Log.i(LogTags.REMINDER, String.format("%s reminder %d for %s",
                    status == ReminderEvent.ReminderStatus.TAKEN ? "Taken" : "Dismissed",
                    reminderEvent.reminderEventId,
                    reminderEvent.medicineName));
        }

    }

    private static void cancelNotification(ReminderEvent reminderEvent, NotificationManager notificationManager) {
        if (reminderEvent.notificationId != 0) {
            Log.d(LogTags.REMINDER, String.format("Canceling notification %d", reminderEvent.notificationId));
            notificationManager.cancel(reminderEvent.notificationId);
        }
    }

    private static void cancelSnoozeAlarm(Context context, int reminderEventId, ReminderEvent reminderEvent) {
        PendingIntent snoozePendingIntent = new RescheduleWork.PendingIntentBuilder(context).
                setReminderId(reminderEvent.reminderId).
                setRequestCode(reminderEvent.notificationId).
                setReminderEventId(reminderEventId).build();
        context.getSystemService(AlarmManager.class).cancel(snoozePendingIntent);
    }
}
