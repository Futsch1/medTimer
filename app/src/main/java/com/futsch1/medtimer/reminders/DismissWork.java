package com.futsch1.medtimer.reminders;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_EVENT_ID;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.futsch1.medtimer.database.ReminderEvent;

public class DismissWork extends Worker {
    public DismissWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        NotificationAction.processNotification(getApplicationContext(), getInputData().getInt(EXTRA_REMINDER_EVENT_ID, 0), ReminderEvent.ReminderStatus.SKIPPED);

        return Result.success();
    }
}