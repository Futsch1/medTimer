package com.futsch1.medtimer.reminders;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMAINING_REPEATS;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_SCHEDULE_FOR_TESTS;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.futsch1.medtimer.LogTags;
import com.futsch1.medtimer.ScheduledReminder;
import com.futsch1.medtimer.WorkManagerAccess;
import com.futsch1.medtimer.database.FullMedicine;
import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.preferences.PreferencesNames;
import com.futsch1.medtimer.reminders.notifications.Notification;
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler;
import com.futsch1.medtimer.widgets.WidgetUpdateReceiver;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

/**
 * Worker that schedules the next reminder.
 */
public class RescheduleWork extends Worker {
    protected final Context context;
    private final AlarmManager alarmManager;

    public RescheduleWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        alarmManager = context.getSystemService(AlarmManager.class);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i(LogTags.REMINDER, "Received scheduler request");

        MedicineRepository medicineRepository = new MedicineRepository((Application) getApplicationContext());
        ReminderScheduler reminderScheduler = getReminderScheduler();
        List<FullMedicine> fullMedicines = medicineRepository.getMedicines();
        List<ScheduledReminder> scheduledReminders = reminderScheduler.schedule(fullMedicines, medicineRepository.getLastDaysReminderEvents(33));
        Notification scheduledNotification = Notification.Companion.fromScheduledReminders(scheduledReminders);
        if (!scheduledNotification.getValid()) {
            this.enqueueNotification(scheduledNotification);
        } else {
            this.cancelNextReminder();
        }

        return Result.success();
    }

    @NonNull
    private ReminderScheduler getReminderScheduler() {

        return new ReminderScheduler(new ReminderScheduler.TimeAccess() {
            @NonNull
            @Override
            public ZoneId systemZone() {
                return ZoneId.systemDefault();
            }

            @NonNull
            @Override
            public LocalDate localDate() {
                return LocalDate.now();
            }
        }, PreferenceManager.getDefaultSharedPreferences(context));
    }

    protected void enqueueNotification(Notification scheduledNotification) {
        // Apply debug rescheduling
        Instant scheduledInstant = scheduledNotification.getRemindInstant();
        DebugReschedule debugReschedule = new DebugReschedule(context, getInputData());
        scheduledInstant = debugReschedule.adjustTimestamp(scheduledInstant);

        // Cancel potentially already running alarm and set new
        alarmManager.cancel(PendingIntent.getBroadcast(context, 0, new Intent(), FLAG_IMMUTABLE));
        for (int reminderEventId : scheduledNotification.getReminderEventIds()) {
            alarmManager.cancel(PendingIntent.getBroadcast(context, reminderEventId, new Intent(), FLAG_IMMUTABLE));
        }

        // If the alarm is in the future, schedule with alarm manager
        if (scheduledInstant.isAfter(Instant.now())) {
            PendingIntent pendingIntent = scheduledNotification.getPendingIntent(context);

            if (canScheduleExactAlarms(alarmManager)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, scheduledInstant.toEpochMilli(), pendingIntent);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, scheduledInstant.toEpochMilli(), pendingIntent);
            }

            Log.i(LogTags.SCHEDULER,
                    String.format("Scheduled reminder for %s/rIDs %s to %s",
                            scheduledNotification.getNotificationName(),
                            scheduledNotification.getReminderIds(),
                            scheduledInstant));

            updateNextReminderWidget();

        } else {
            // Immediately remind
            Log.i(LogTags.SCHEDULER,
                    String.format("Scheduling reminder for %s/rIDs %s now",
                            scheduledNotification.getNotificationName(),
                            Arrays.toString(scheduledNotification.getReminderIds())));
            Data.Builder builder = new Data.Builder();
            scheduledNotification.toBuilder(builder);
            WorkRequest reminderWork =
                    new OneTimeWorkRequest.Builder(ReminderWork.class)
                            .setInputData(builder.build())
                            .build();
            WorkManagerAccess.getWorkManager(context).enqueue(reminderWork);
        }

        debugReschedule.scheduleRepeat();
    }

    private void cancelNextReminder() {
        // Pending reminders are distinguished by their request code, which is the reminder event id.
        // So if we cancel the reminderEventId 0, we cancel all the next reminder that was not yet raised.
        Intent intent = ReminderProcessor.getReminderAction(context);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
    }

    private boolean canScheduleExactAlarms(AlarmManager alarmManager) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean exactReminders = sharedPref.getBoolean(PreferencesNames.EXACT_REMINDERS, true);

        return exactReminders && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms());
    }

    private void updateNextReminderWidget() {
        Intent intent = new Intent(context, WidgetUpdateReceiver.class);
        intent.setAction("com.futsch1.medtimer.NEXT_REMINDER_WIDGET_UPDATE");
        context.sendBroadcast(intent, "com.futsch1.medtimer.NOTIFICATION_PROCESSED");
    }

    private static class DebugReschedule {
        Context context;
        long delay;
        int repeats;

        DebugReschedule(Context context, Data inputData) {
            this.context = context;
            delay = inputData.getLong(EXTRA_SCHEDULE_FOR_TESTS, -1);
            repeats = inputData.getInt(EXTRA_REMAINING_REPEATS, -1);
        }

        public Instant adjustTimestamp(Instant instant) {
            if (delay >= 0) {
                return Instant.now().plusMillis(delay);
            } else {
                return instant;
            }
        }

        public void scheduleRepeat() {
            if (delay >= 0 && repeats >= 0) {
                ReminderProcessor.requestRescheduleNowForTests(context, delay, repeats - 1);
            }
        }
    }
}
