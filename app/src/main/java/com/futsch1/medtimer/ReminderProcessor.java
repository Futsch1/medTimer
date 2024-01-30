package com.futsch1.medtimer;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_ID;
import static com.futsch1.medtimer.ActivityCodes.RESCHEDULE_ACTION;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

public class ReminderProcessor extends BroadcastReceiver {

    public ReminderProcessor() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        WorkManager workManager = WorkManager.getInstance(context);
        if (RESCHEDULE_ACTION.equals(intent.getAction())) {
            WorkRequest rescheduleWork =
                    new OneTimeWorkRequest.Builder(RescheduleWork.class)
                            .build();
            workManager.enqueue(rescheduleWork);

        } else {
            WorkRequest reminderWork =
                    new OneTimeWorkRequest.Builder(ReminderWork.class)
                            .setInputData(new Data.Builder().putInt(EXTRA_REMINDER_ID, intent.getIntExtra(EXTRA_REMINDER_ID, 0)).build())
                            .build();
            workManager.enqueue(reminderWork);
        }
    }

}
