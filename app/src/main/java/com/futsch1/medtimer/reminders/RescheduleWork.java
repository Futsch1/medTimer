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
import com.futsch1.medtimer.PreferencesFragment;
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
        reminderScheduler.schedule(medicineWithReminders, medicineRepository.getLastDaysReminderEvents(2));

        return Result.success();
    }

    @NonNull
    private ReminderScheduler getReminderScheduler() {

        return new ReminderScheduler((timestamp, medicine, reminder) -> this.schedule(timestamp, reminder.reminderId, medicine.name, -1, 0), new ReminderScheduler.TimeAccess() {
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
            PendingIntent pendingIntent = getPendingIntent(context, reminderId, requestCode, reminderEventId);

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

    public static PendingIntent getPendingIntent(Context context, int reminderId, int requestCode, int reminderEventId) {
        Intent reminderIntent = ReminderProcessor.getReminderAction(context, reminderId, reminderEventId);
        return PendingIntent.getBroadcast(context, requestCode, reminderIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private boolean canScheduleExactAlarms(AlarmManager alarmManager) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean exactReminders = sharedPref.getBoolean(PreferencesFragment.EXACT_REMINDERS, true);

        return exactReminders && alarmManager.canScheduleExactAlarms();
    }
}
