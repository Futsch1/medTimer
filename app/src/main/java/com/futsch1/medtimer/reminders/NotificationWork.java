package com.futsch1.medtimer.reminders;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_EVENT_ID_LIST;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.futsch1.medtimer.database.ReminderEvent;

public class NotificationWork extends Worker {

    private final ReminderEvent.ReminderStatus status;

    public NotificationWork(@NonNull Context context, @NonNull WorkerParameters workerParams, ReminderEvent.ReminderStatus status) {
        super(context, workerParams);
        this.status = status;
    }

    @NonNull
    @Override
    public ListenableWorker.Result doWork() {
        NotificationAction.processNotification(getApplicationContext(), getInputData().getIntArray(EXTRA_REMINDER_EVENT_ID_LIST), status);
        return ListenableWorker.Result.success();
    }

}
