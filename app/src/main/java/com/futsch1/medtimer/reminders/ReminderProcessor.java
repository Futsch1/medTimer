package com.futsch1.medtimer.reminders;

import static com.futsch1.medtimer.ActivityCodes.DISMISSED_ACTION;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_EVENT_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_ID;
import static com.futsch1.medtimer.ActivityCodes.REMINDER_ACTION;
import static com.futsch1.medtimer.ActivityCodes.RESCHEDULE_ACTION;
import static com.futsch1.medtimer.ActivityCodes.TAKEN_ACTION;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import java.time.Duration;

public class ReminderProcessor extends BroadcastReceiver {

    public ReminderProcessor() {
    }

    public static void requestReschedule(@NonNull Context context) {
        Intent intent = new Intent(RESCHEDULE_ACTION);
        intent.setClass(context, ReminderProcessor.class);
        context.sendBroadcast(intent);
    }

    public static Intent getDismissedActionIntent(@NonNull Context context, int reminderEventId) {
        Intent notifyDismissed = new Intent(context, ReminderProcessor.class);
        notifyDismissed.setAction(DISMISSED_ACTION);
        notifyDismissed.putExtra(EXTRA_REMINDER_EVENT_ID, reminderEventId);
        return notifyDismissed;
    }

    public static Intent getTakenActionIntent(@NonNull Context context, int reminderEventId) {
        Intent notifyTaken = new Intent(context, ReminderProcessor.class);
        notifyTaken.setAction(TAKEN_ACTION);
        notifyTaken.putExtra(EXTRA_REMINDER_EVENT_ID, reminderEventId);
        return notifyTaken;
    }

    public static Intent getReminderAction(@NonNull Context context, int reminderId) {
        Intent reminderIntent = new Intent(REMINDER_ACTION);
        reminderIntent.putExtra(EXTRA_REMINDER_ID, reminderId);
        reminderIntent.setClass(context, ReminderProcessor.class);
        return reminderIntent;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        WorkManager workManager = WorkManager.getInstance(context);
        if (RESCHEDULE_ACTION.equals(intent.getAction())) {
            WorkRequest rescheduleWork =
                    new OneTimeWorkRequest.Builder(RescheduleWork.class)
                            .setInitialDelay(Duration.ofSeconds(1))
                            .build();
            workManager.enqueue(rescheduleWork);
        } else if (DISMISSED_ACTION.equals(intent.getAction())) {
            WorkRequest dismissWork =
                    new OneTimeWorkRequest.Builder(DismissWork.class)
                            .setInputData(new Data.Builder()
                                    .putInt(EXTRA_REMINDER_EVENT_ID, intent.getIntExtra(EXTRA_REMINDER_EVENT_ID, 0))
                                    .build())

                            .build();
            workManager.enqueue(dismissWork);
        } else if (TAKEN_ACTION.equals(intent.getAction())) {
            WorkRequest takenWork =
                    new OneTimeWorkRequest.Builder(TakenWork.class)
                            .setInputData(new Data.Builder()
                                    .putInt(EXTRA_REMINDER_EVENT_ID, intent.getIntExtra(EXTRA_REMINDER_EVENT_ID, 0))
                                    .build())
                            .build();
            workManager.enqueue(takenWork);
        } else {
            WorkRequest reminderWork =
                    new OneTimeWorkRequest.Builder(ReminderWork.class)
                            .setInputData(new Data.Builder().putInt(EXTRA_REMINDER_ID, intent.getIntExtra(EXTRA_REMINDER_ID, 0)).build())
                            .build();
            workManager.enqueue(reminderWork);
        }
    }

}