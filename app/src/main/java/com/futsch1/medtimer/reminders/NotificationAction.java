package com.futsch1.medtimer.reminders;

import android.app.AlarmManager;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.util.Log;

import com.futsch1.medtimer.LogTags;
import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;

import java.time.Instant;

public class NotificationAction {
    private NotificationAction() {
        // Intentionally empty
    }

    static void processNotification(Context context, int[] reminderEventIds, ReminderEvent.ReminderStatus status) {
        MedicineRepository medicineRepository = new MedicineRepository((Application) context);

        for (int reminderEventId : reminderEventIds) {
            ReminderEvent reminderEvent = medicineRepository.getReminderEvent(reminderEventId);

            if (reminderEvent != null) {
                if (reminderEvent.askForAmount && status == ReminderEvent.ReminderStatus.TAKEN) {
                    context.startActivity(ReminderProcessor.getVariableAmountActionIntent(context, reminderEventId, reminderEvent.amount));
                } else {
                    processReminderEvent(context, status, reminderEvent, medicineRepository);
                }
            }
        }
    }

    public static void processReminderEvent(Context context, ReminderEvent.ReminderStatus status, ReminderEvent reminderEvent, MedicineRepository medicineRepository) {
        cancelNotification(context, reminderEvent.notificationId);
        cancelPendingAlarms(context, reminderEvent.reminderEventId);

        reminderEvent.status = status;
        doStockHandling(context, reminderEvent, medicineRepository);
        reminderEvent.processedTimestamp = Instant.now().getEpochSecond();

        medicineRepository.updateReminderEvent(reminderEvent);
        Log.i(LogTags.REMINDER, String.format("%s reminder reID %d for %s",
                status == ReminderEvent.ReminderStatus.TAKEN ? "Taken" : "Dismissed",
                reminderEvent.reminderEventId,
                reminderEvent.medicineName));

        // Reschedule since the trigger condition for a linked reminder might have changed
        ReminderProcessor.requestReschedule(context);
    }

    public static void cancelNotification(Context context, int notificationId) {
        if (notificationId != 0) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            Log.d(LogTags.REMINDER, String.format("Cancel notification nID %d", notificationId));
            notificationManager.cancel(notificationId);
        }
    }

    public static void cancelPendingAlarms(Context context, int reminderEventId) {
        PendingIntent snoozePendingIntent = new PendingIntentBuilder(context).
                setReminderEventId(reminderEventId).
                build();
        Log.d(LogTags.REMINDER, String.format("Cancel all pending alarms for reID %d", reminderEventId));
        context.getSystemService(AlarmManager.class).cancel(snoozePendingIntent);
    }

    private static void doStockHandling(Context context, ReminderEvent reminderEvent, MedicineRepository medicineRepository) {
        if (!reminderEvent.stockHandled && reminderEvent.status == ReminderEvent.ReminderStatus.TAKEN) {
            reminderEvent.stockHandled = true;
            Reminder reminder = medicineRepository.getReminder(reminderEvent.reminderId);
            if (reminder != null) {
                ReminderProcessor.requestStockHandling(context, reminder.amount, reminder.medicineRelId);
            }
        }
    }
}
