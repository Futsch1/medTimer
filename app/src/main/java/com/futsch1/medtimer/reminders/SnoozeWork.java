package com.futsch1.medtimer.reminders;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_NOTIFICATION_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_EVENT_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_SNOOZE_TIME;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import java.time.Instant;

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
        Instant remindTime = Instant.now().plusSeconds(snoozeTime * 60L);

        int reminderId = inputData.getInt(EXTRA_REMINDER_ID, 0);
        int reminderEventId = inputData.getInt(EXTRA_REMINDER_EVENT_ID, 0);
        int notificationId = inputData.getInt(EXTRA_NOTIFICATION_ID, 0);

        // Cancel a potential repeat alarm
        NotificationAction.cancelPendingAlarms(context, reminderEventId);

        ReminderNotificationData reminderNotificationData = new ReminderNotificationData(
                remindTime,
                reminderId,
                "Snooze",
                reminderEventId);
        enqueueNotification(reminderNotificationData);

        NotificationAction.cancelNotification(context, notificationId);

        return Result.success();
    }
}
