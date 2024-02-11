package com.futsch1.medtimer.reminders;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;

import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.ReminderEvent;

import java.time.Instant;

public class NotificationAction {
    static void processNotification(Context context, int reminderId, ReminderEvent.ReminderStatus status) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        MedicineRepository medicineRepository = new MedicineRepository((Application) context);
        ReminderEvent reminderEvent = medicineRepository.getReminderEvent(reminderId);

        if (reminderEvent.notificationId != 0) {
            Log.d("Reminder", String.format("Canceling notification %d", reminderEvent.notificationId));
            notificationManager.cancel(reminderEvent.notificationId);
        }

        reminderEvent.status = status;
        reminderEvent.processedTimestamp = Instant.now().getEpochSecond();
        medicineRepository.updateReminderEvent(reminderEvent);
        Log.i("Reminder", String.format("%s reminder %d for %s",
                status == ReminderEvent.ReminderStatus.TAKEN ? "Taken" : "Dismissed",
                reminderEvent.reminderEventId,
                reminderEvent.medicineName));

    }
}
