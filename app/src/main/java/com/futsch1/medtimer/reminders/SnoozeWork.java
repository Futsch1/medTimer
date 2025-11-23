package com.futsch1.medtimer.reminders;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_NOTIFICATION_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_SNOOZE_TIME;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

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

        ScheduledNotification scheduledNotification = ScheduledNotification.Companion.fromInputData(inputData);
        scheduledNotification.delayBy(snoozeTime * 60);

        int notificationId = inputData.getInt(EXTRA_NOTIFICATION_ID, 0);

        // Cancel a potential repeat alarm
        NotificationAction.cancelPendingAlarms(context, scheduledNotification.getReminderEventIds().get(0));

        enqueueNotification(scheduledNotification);

        NotificationAction.cancelNotification(context, notificationId);

        return Result.success();
    }
}
