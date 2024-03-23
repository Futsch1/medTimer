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
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.futsch1.medtimer.LogTags;
import com.futsch1.medtimer.NextReminderListener;
import com.futsch1.medtimer.PreferencesFragment;
import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.helpers.MedicineHelper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class RescheduleWork extends Worker {
    public RescheduleWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i(LogTags.REMINDER, "Received scheduler request");
        AlarmManager alarmManager = getApplicationContext().getSystemService(AlarmManager.class);
        MedicineRepository medicineRepository = new MedicineRepository((Application) getApplicationContext());
        ReminderScheduler reminderScheduler = new ReminderScheduler((timestamp, medicine, reminder) -> this.schedule(getApplicationContext(), alarmManager, timestamp, reminder, medicine), new ReminderScheduler.TimeAccess() {
            @Override
            public ZoneId systemZone() {
                return ZoneId.systemDefault();
            }

            @Override
            public LocalDate localDate() {
                return LocalDate.now();
            }
        });

        List<MedicineWithReminders> medicineWithReminders = medicineRepository.getMedicines();
        reminderScheduler.schedule(medicineWithReminders, medicineRepository.getLastDaysReminderEvents(MedicineHelper.getMaxDaysBetweenReminders(medicineWithReminders) + 1));

        return Result.success();
    }

    private void schedule(Context context, AlarmManager alarmManager, Instant timestamp, Reminder reminder, Medicine medicine) {
        // If the alarm is in the future, schedule with alarm manager
        if (timestamp.isAfter(Instant.now())) {
            Intent reminderIntent = ReminderProcessor.getReminderAction(context, reminder.reminderId);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 100, reminderIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            // Cancel potentially already running alarm and set new
            alarmManager.cancel(pendingIntent);

            if (canScheduleExactAlarms(alarmManager)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timestamp.toEpochMilli(), pendingIntent);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timestamp.toEpochMilli(), pendingIntent);
            }

            // Notify GUI listener
            NextReminderListener.sendNextReminder(context, reminder.reminderId, timestamp);

            Log.i(LogTags.SCHEDULER, String.format("Scheduled reminder for %s to %s", medicine.name, timestamp));
        } else {
            // Immediately schedule
            WorkRequest reminderWork =
                    new OneTimeWorkRequest.Builder(ReminderWork.class)
                            .setInputData(new Data.Builder().putInt(EXTRA_REMINDER_ID, reminder.reminderId).build())
                            .build();
            WorkManager.getInstance(getApplicationContext()).enqueue(reminderWork);
        }
    }

    private boolean canScheduleExactAlarms(AlarmManager alarmManager) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean exactReminders = sharedPref.getBoolean(PreferencesFragment.EXACT_REMINDERS, true);

        return exactReminders && alarmManager.canScheduleExactAlarms();
    }
}
