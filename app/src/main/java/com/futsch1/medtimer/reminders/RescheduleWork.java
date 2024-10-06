package com.futsch1.medtimer.reminders;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_ID;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.futsch1.medtimer.LogTags;
import com.futsch1.medtimer.PreferencesNames;
import com.futsch1.medtimer.ScheduledReminder;
import com.futsch1.medtimer.WorkManagerAccess;
import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class RescheduleWork extends Worker {
    protected final Context context;
    private final AlarmManager alarmManager;
    private final WeekendMode weekendMode;

    public RescheduleWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        this.weekendMode = new WeekendMode(PreferenceManager.getDefaultSharedPreferences(context));
        alarmManager = context.getSystemService(AlarmManager.class);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i(LogTags.REMINDER, "Received scheduler request");

        MedicineRepository medicineRepository = new MedicineRepository((Application) getApplicationContext());
        ReminderScheduler reminderScheduler = getReminderScheduler();
        List<MedicineWithReminders> medicineWithReminders = medicineRepository.getMedicines();
        List<ScheduledReminder> scheduledReminders = reminderScheduler.schedule(medicineWithReminders, medicineRepository.getLastDaysReminderEvents(2));
        if (!scheduledReminders.isEmpty()) {
            ScheduledReminder scheduledReminder = scheduledReminders.get(0);
            this.schedule(scheduledReminder.timestamp(), scheduledReminder.reminder().reminderId, scheduledReminder.medicine().name, -1, 0);
        } else {
            this.clearAlarms();
        }

        return Result.success();
    }

    @NonNull
    private ReminderScheduler getReminderScheduler() {

        return new ReminderScheduler(new ReminderScheduler.TimeAccess() {
            @Override
            public ZoneId systemZone() {
                return ZoneId.systemDefault();
            }

            @Override
            public LocalDate localDate() {
                return LocalDate.now();
            }
        });
    }

    protected void schedule(Instant timestamp, int reminderId, String medicineName, int requestCode, int reminderEventId) {
        // Apply weekend mode shift
        timestamp = weekendMode.adjustInstant(timestamp);
        // If the alarm is in the future, schedule with alarm manager
        if (timestamp.isAfter(Instant.now())) {
            PendingIntent pendingIntent = new PendingIntentBuilder(context).
                    setReminderId(reminderId).
                    setRequestCode(requestCode).
                    setReminderEventId(reminderEventId).
                    setReminderDate(timestamp.atZone(ZoneId.systemDefault()).toLocalDate()).build();

            // Cancel potentially already running alarm and set new
            alarmManager.cancel(pendingIntent);

            if (canScheduleExactAlarms(alarmManager)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timestamp.toEpochMilli(), pendingIntent);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timestamp.toEpochMilli(), pendingIntent);
            }

            Log.i(LogTags.SCHEDULER, String.format("Scheduled reminder for %s/%d to %s", medicineName, reminderId, timestamp));
        } else {
            // Immediately schedule
            WorkRequest reminderWork =
                    new OneTimeWorkRequest.Builder(ReminderWork.class)
                            .setInputData(new Data.Builder().putInt(EXTRA_REMINDER_ID, reminderId).build())
                            .build();
            WorkManagerAccess.getWorkManager(context).enqueue(reminderWork);
        }
    }

    private void clearAlarms() {
        Intent intent = ReminderProcessor.getReminderAction(context, 0, 0, LocalDate.now());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, -1, intent, PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
    }

    private boolean canScheduleExactAlarms(AlarmManager alarmManager) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean exactReminders = sharedPref.getBoolean(PreferencesNames.EXACT_REMINDERS, true);

        return exactReminders && alarmManager.canScheduleExactAlarms();
    }

    public static class PendingIntentBuilder {
        private final Context context;
        private int reminderId;
        private int requestCode;
        private int reminderEventId;
        private LocalDate reminderDate = null;

        public PendingIntentBuilder(Context context) {
            this.context = context;
        }

        public PendingIntentBuilder setReminderId(int reminderId) {
            this.reminderId = reminderId;
            return this;
        }

        public PendingIntentBuilder setRequestCode(int requestCode) {
            this.requestCode = requestCode;
            return this;
        }

        public PendingIntentBuilder setReminderEventId(int reminderEventId) {
            this.reminderEventId = reminderEventId;
            return this;
        }

        public PendingIntentBuilder setReminderDate(LocalDate reminderDate) {
            this.reminderDate = reminderDate;
            return this;
        }

        public PendingIntent build() {
            Intent reminderIntent = ReminderProcessor.getReminderAction(context, reminderId, reminderEventId, reminderDate);
            return PendingIntent.getBroadcast(context, requestCode, reminderIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }
}
