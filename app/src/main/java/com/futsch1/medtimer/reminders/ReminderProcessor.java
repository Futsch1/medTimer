package com.futsch1.medtimer.reminders;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.futsch1.medtimer.ActivityCodes.ALL_TAKEN_ACTION;
import static com.futsch1.medtimer.ActivityCodes.DISMISSED_ACTION;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_AMOUNT;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_MEDICINE_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_NOTIFICATION_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMAINING_REPEATS;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_DATE;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_EVENT_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_TIME;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REPEAT_TIME_SECONDS;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_SNOOZE_TIME;
import static com.futsch1.medtimer.ActivityCodes.REMINDER_ACTION;
import static com.futsch1.medtimer.ActivityCodes.SNOOZE_ACTION;
import static com.futsch1.medtimer.ActivityCodes.TAKEN_ACTION;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.futsch1.medtimer.MainActivity;
import com.futsch1.medtimer.WorkManagerAccess;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class ReminderProcessor extends BroadcastReceiver {

    public static void requestReschedule(@NonNull Context context) {
        WorkManager workManager = WorkManagerAccess.getWorkManager(context);
        OneTimeWorkRequest rescheduleWork =
                new OneTimeWorkRequest.Builder(RescheduleWork.class)
                        .setInitialDelay(Duration.of(1, ChronoUnit.SECONDS))
                        .build();
        workManager.enqueueUniqueWork("reschedule", ExistingWorkPolicy.KEEP, rescheduleWork);
    }

    public static void requestRepeat(@NonNull Context context, int reminderId, int reminderEventId, int repeatTimeSeconds, int remainingRepeats) {
        WorkManager workManager = WorkManagerAccess.getWorkManager(context);
        OneTimeWorkRequest repeatWork =
                new OneTimeWorkRequest.Builder(RepeatReminderWork.class)
                        .setInputData(new Data.Builder()
                                .putInt(EXTRA_REMINDER_ID, reminderId)
                                .putInt(EXTRA_REMINDER_EVENT_ID, reminderEventId)
                                .putInt(EXTRA_REPEAT_TIME_SECONDS, repeatTimeSeconds)
                                .putInt(EXTRA_REMAINING_REPEATS, remainingRepeats)
                                .build())
                        .build();
        workManager.enqueue(repeatWork);
    }

    public static Intent getReminderAction(@NonNull Context context, int reminderId, int reminderEventId, LocalDateTime reminderDateTime) {
        Intent reminderIntent = new Intent(REMINDER_ACTION);
        reminderIntent.putExtra(EXTRA_REMINDER_ID, reminderId);
        reminderIntent.putExtra(EXTRA_REMINDER_EVENT_ID, reminderEventId);
        reminderIntent.putExtra(EXTRA_REMINDER_DATE, reminderDateTime != null ? reminderDateTime.toLocalDate().toEpochDay() : 0);
        reminderIntent.putExtra(EXTRA_REMINDER_TIME, reminderDateTime != null ? reminderDateTime.toLocalTime().toSecondOfDay() : 0);
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

    public static void requestStockHandling(Context context, String amount, int medicineId) {
        WorkManager workManager = WorkManagerAccess.getWorkManager(context);
        OneTimeWorkRequest stockHandlingWork =
                new OneTimeWorkRequest.Builder(StockHandlingWork.class)
                        .setInputData(new Data.Builder()
                                .putString(EXTRA_AMOUNT, amount)
                                .putInt(EXTRA_MEDICINE_ID, medicineId)
                                .build())
                        .build();
        workManager.enqueue(stockHandlingWork);
    }

    public static void requestActionIntent(Context context, int reminderEventId, boolean taken) {
        Intent actionIntent = taken ? getTakenActionIntent(context, reminderEventId) : getSkippedActionIntent(context, reminderEventId);
        if (taken) {
            WorkManagerAccess.getWorkManager(context).enqueue(buildActionWorkRequest(actionIntent, TakenWork.class));
        } else {
            WorkManagerAccess.getWorkManager(context).enqueue(buildActionWorkRequest(actionIntent, SkippedWork.class));
        }
    }

    public static Intent getTakenActionIntent(@NonNull Context context, int reminderEventId) {
        return buildActionIntent(context, reminderEventId, TAKEN_ACTION);
    }

    public static Intent getSkippedActionIntent(@NonNull Context context, int reminderEventId) {
        return buildActionIntent(context, reminderEventId, DISMISSED_ACTION);
    }

    private static <T extends ListenableWorker> WorkRequest buildActionWorkRequest(Intent intent, Class<T> workerClass) {
        return new OneTimeWorkRequest.Builder(workerClass)
                .setInputData(new Data.Builder()
                        .putInt(EXTRA_REMINDER_EVENT_ID, intent.getIntExtra(EXTRA_REMINDER_EVENT_ID, 0))
                        .build())
                .build();
    }

    private static Intent buildActionIntent(@NonNull Context context, int reminderEventId, String actionName) {
        Intent actionIntent = new Intent(context, ReminderProcessor.class);
        actionIntent.setAction(actionName);
        actionIntent.putExtra(EXTRA_REMINDER_EVENT_ID, reminderEventId);
        return actionIntent;
    }

    public static Intent getAllTakenActionIntent(@NonNull Context context, int reminderEventId) {
        return buildActionIntent(context, reminderEventId, ALL_TAKEN_ACTION);
    }

    public static Intent getVariableAmountActionIntent(Context context, int reminderEventId, String amount) {
        Intent actionIntent = new Intent(context, MainActivity.class);
        actionIntent.setAction("VARIABLE_AMOUNT");
        actionIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        actionIntent.putExtra(EXTRA_REMINDER_EVENT_ID, reminderEventId);
        actionIntent.putExtra(EXTRA_AMOUNT, amount);
        return actionIntent;
    }

    public static Intent getCustomSnoozeActionIntent(Context context, int reminderId, int reminderEventId, int notificationId) {
        Intent actionIntent = new Intent(context, MainActivity.class);
        actionIntent.setAction("CUSTOM_SNOOZE");
        actionIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        actionIntent.putExtra(EXTRA_REMINDER_ID, reminderId);
        actionIntent.putExtra(EXTRA_REMINDER_EVENT_ID, reminderEventId);
        actionIntent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
        return actionIntent;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        WorkManager workManager = WorkManagerAccess.getWorkManager(context);
        if (DISMISSED_ACTION.equals(intent.getAction())) {
            workManager.enqueue(buildActionWorkRequest(intent, SkippedWork.class));
        } else if (TAKEN_ACTION.equals(intent.getAction())) {
            workManager.enqueue(buildActionWorkRequest(intent, TakenWork.class));
        } else if (ALL_TAKEN_ACTION.equals(intent.getAction())) {
            workManager.enqueue(buildActionWorkRequest(intent, AllTakenWork.class));
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
                                    .putLong(EXTRA_REMINDER_DATE, intent.getLongExtra(EXTRA_REMINDER_DATE, 0))
                                    .putInt(EXTRA_REMINDER_TIME, intent.getIntExtra(EXTRA_REMINDER_TIME, 0))
                                    .build())
                            .build();
            workManager.enqueue(reminderWork);
        }
    }

}
