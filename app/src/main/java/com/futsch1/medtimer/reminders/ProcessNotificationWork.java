package com.futsch1.medtimer.reminders;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.reminders.notifications.ProcessedNotification;

public class ProcessNotificationWork extends Worker {

    private final ReminderEvent.ReminderStatus status;

    public ProcessNotificationWork(@NonNull Context context, @NonNull WorkerParameters workerParams, ReminderEvent.ReminderStatus status) {
        super(context, workerParams);
        this.status = status;
    }

    @NonNull
    @Override
    public ListenableWorker.Result doWork() {
        NotificationProcessor.processNotification(getApplicationContext(), ProcessedNotification.Companion.fromData(getInputData()), status);
        return ListenableWorker.Result.success();
    }

}
