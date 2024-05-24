package com.futsch1.medtimer.reminders;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_NOTIFICATION_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_EVENT_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_SNOOZE_TIME;

import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.futsch1.medtimer.LogTags;

import java.time.Instant;

public class SnoozeWork extends RescheduleWork {

    public SnoozeWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Data inputData = getInputData();

        int reminderId = inputData.getInt(EXTRA_REMINDER_ID, 0);
        int reminderEventId = inputData.getInt(EXTRA_REMINDER_EVENT_ID, 0);
        int snoozeTime = inputData.getInt(EXTRA_SNOOZE_TIME, 15);
        int notificationId = inputData.getInt(EXTRA_NOTIFICATION_ID, 0);
        Instant remindTime = Instant.now().plusSeconds(snoozeTime * 60L);

        schedule(remindTime, reminderId, "snooze", notificationId, reminderEventId);

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

        if (notificationId != 0) {
            Log.d(LogTags.REMINDER, String.format("Snoozing notification %d", notificationId));
            notificationManager.cancel(notificationId);
        }

        return Result.success();
    }
}
