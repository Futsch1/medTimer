package com.futsch1.medtimer.reminders;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import com.futsch1.medtimer.database.ReminderEvent;

public class TakenWork extends NotificationWork {

    public TakenWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams, ReminderEvent.ReminderStatus.TAKEN);
    }
}