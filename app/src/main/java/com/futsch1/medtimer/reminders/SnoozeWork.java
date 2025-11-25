package com.futsch1.medtimer.reminders;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_NOTIFICATION_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_SNOOZE_TIME;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData;

/*
 * Worker that snoozes a reminder and re-raises it once the snooze time has expired.
 */
public class SnoozeWork extends RescheduleWork {

    public SnoozeWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Data inputData = getInputData();
        int snoozeTime = inputData.getInt(EXTRA_SNOOZE_TIME, 15);

        ReminderNotificationData reminderNotificationData = ReminderNotificationData.Companion.fromInputData(inputData, null);
        reminderNotificationData.delayBy(snoozeTime * 60);

        int notificationId = inputData.getInt(EXTRA_NOTIFICATION_ID, 0);

        // Cancel a potential repeat alarm
        NotificationProcessor.cancelPendingAlarms(context, reminderNotificationData.getReminderEventIds()[0]);

        enqueueNotification(reminderNotificationData);

        NotificationProcessor.cancelNotification(context, notificationId);

        return Result.success();
    }
}
