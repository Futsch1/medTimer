package com.futsch1.medtimer.reminders;

import static com.futsch1.medtimer.ActivityCodes.DISMISSED_ACTION;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_NOTIFICATION_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_EVENT_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_SNOOZE_TIME;
import static com.futsch1.medtimer.ActivityCodes.REMINDER_ACTION;
import static com.futsch1.medtimer.ActivityCodes.RESCHEDULE_ACTION;
import static com.futsch1.medtimer.ActivityCodes.SNOOZE_ACTION;
import static com.futsch1.medtimer.ActivityCodes.TAKEN_ACTION;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.futsch1.medtimer.LogTags;
import com.futsch1.medtimer.WorkManagerAccess;

public class ReminderProcessor extends BroadcastReceiver {

    public static void requestReschedule(@NonNull Context context) {
        Log.i(LogTags.SCHEDULER, "Requesting reschedule");
        Intent intent = new Intent(RESCHEDULE_ACTION);
        intent.setClass(context, ReminderProcessor.class);
        context.sendBroadcast(intent);
    }

    public static Intent getDismissedActionIntent(@NonNull Context context, int reminderEventId) {
        return buildActionIntent(context, reminderEventId, DISMISSED_ACTION);
    }

    private static Intent buildActionIntent(@NonNull Context context, int reminderEventId, String actionName) {
        Intent notifyDismissed = new Intent(context, ReminderProcessor.class);
        notifyDismissed.setAction(actionName);
        notifyDismissed.putExtra(EXTRA_REMINDER_EVENT_ID, reminderEventId);
        return notifyDismissed;
    }

    public static Intent getTakenActionIntent(@NonNull Context context, int reminderEventId) {
        return buildActionIntent(context, reminderEventId, TAKEN_ACTION);
    }

    public static Intent getReminderAction(@NonNull Context context, int reminderId, int reminderEventId) {
        Intent reminderIntent = new Intent(REMINDER_ACTION);
        reminderIntent.putExtra(EXTRA_REMINDER_ID, reminderId);
        reminderIntent.putExtra(EXTRA_REMINDER_EVENT_ID, reminderEventId);
        reminderIntent.setClass(context, ReminderProcessor.class);
        return reminderIntent;
    }

    public static Intent getSnoozeIntent(@NonNull Context context, int reminderId, int reminderEventId, int notificationId, int snoozeTime) {
        Intent snoozeIntent = new Intent(SNOOZE_ACTION);
        snoozeIntent.putExtra(EXTRA_REMINDER_ID, reminderId);
        snoozeIntent.putExtra(EXTRA_REMINDER_EVENT_ID, reminderEventId);
        snoozeIntent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
        snoozeIntent.putExtra(EXTRA_SNOOZE_TIME, snoozeTime);
        snoozeIntent.setClass(context, ReminderProcessor.class);
        return snoozeIntent;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        WorkManager workManager = WorkManagerAccess.getWorkManager(context);
        if (RESCHEDULE_ACTION.equals(intent.getAction())) {
            OneTimeWorkRequest rescheduleWork =
                    new OneTimeWorkRequest.Builder(RescheduleWork.class)
                            .build();
            workManager.enqueueUniqueWork("reschedule", ExistingWorkPolicy.KEEP, rescheduleWork);
        } else if (DISMISSED_ACTION.equals(intent.getAction())) {
            workManager.enqueue(buildActionWorkRequest(intent, DismissWork.class));
        } else if (TAKEN_ACTION.equals(intent.getAction())) {
            workManager.enqueue(buildActionWorkRequest(intent, TakenWork.class));
        } else if (SNOOZE_ACTION.equals(intent.getAction())) {
            WorkRequest snoozeWork =
                    new OneTimeWorkRequest.Builder(SnoozeWork.class)
                            .setInputData(new Data.Builder()
                                    .putInt(EXTRA_REMINDER_ID, intent.getIntExtra(EXTRA_REMINDER_ID, 0))
                                    .putInt(EXTRA_REMINDER_EVENT_ID, intent.getIntExtra(EXTRA_REMINDER_EVENT_ID, 0))
                                    .putInt(EXTRA_NOTIFICATION_ID, intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0))
                                    .putInt(EXTRA_SNOOZE_TIME, intent.getIntExtra(EXTRA_SNOOZE_TIME, 15))
                                    .build())
                            .build();
            workManager.enqueue(snoozeWork);
        } else {
            WorkRequest reminderWork =
                    new OneTimeWorkRequest.Builder(ReminderWork.class)
                            .setInputData(new Data.Builder()
                                    .putInt(EXTRA_REMINDER_ID, intent.getIntExtra(EXTRA_REMINDER_ID, 0))
                                    .putInt(EXTRA_REMINDER_EVENT_ID, intent.getIntExtra(EXTRA_REMINDER_EVENT_ID, 0))
                                    .build())
                            .build();
            workManager.enqueue(reminderWork);
        }
    }

    private <T extends ListenableWorker> WorkRequest buildActionWorkRequest(Intent intent, Class<T> workerClass) {
        return new OneTimeWorkRequest.Builder(workerClass)
                .setInputData(new Data.Builder()
                        .putInt(EXTRA_REMINDER_EVENT_ID, intent.getIntExtra(EXTRA_REMINDER_EVENT_ID, 0))
                        .build())
                .build();
    }

}
