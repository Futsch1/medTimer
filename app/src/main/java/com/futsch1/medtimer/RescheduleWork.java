package com.futsch1.medtimer;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_TIME;
import static com.futsch1.medtimer.ActivityCodes.NEXT_REMINDER_ACTION;
import static com.futsch1.medtimer.ActivityCodes.REMINDER_ACTION;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.Reminder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class RescheduleWork extends Worker {
    public RescheduleWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i("Reminder", "Received scheduler request");
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

        reminderScheduler.updateMedicine(medicineRepository.getMedicines());
        reminderScheduler.updateReminderEvents(medicineRepository.getLastDaysReminderEvents());

        return Result.success();
    }

    private void schedule(Context context, AlarmManager alarmManager, Instant timestamp, Reminder reminder, Medicine medicine) {
        if (timestamp.isAfter(Instant.now())) {
            Intent reminderIntent = new Intent(REMINDER_ACTION);
            reminderIntent.putExtra(EXTRA_REMINDER_ID, reminder.reminderId);
            reminderIntent.setClass(getApplicationContext(), ReminderProcessor.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 100, reminderIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            alarmManager.cancel(pendingIntent);

            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timestamp.toEpochMilli(), pendingIntent);

            Intent nextReminderIntent = new Intent(NEXT_REMINDER_ACTION);
            nextReminderIntent.putExtra(EXTRA_REMINDER_ID, reminder.reminderId);
            nextReminderIntent.putExtra(EXTRA_REMINDER_TIME, timestamp);
            context.sendBroadcast(nextReminderIntent);

            Log.i("Scheduler", String.format("Scheduled reminder for %s to %s", medicine.name, timestamp));
        } else {
            WorkRequest reminderWork =
                    new OneTimeWorkRequest.Builder(ReminderWork.class)
                            .setInputData(new Data.Builder().putInt(EXTRA_REMINDER_ID, reminder.reminderId).build())
                            .build();
            WorkManager.getInstance(getApplicationContext()).enqueue(reminderWork);
        }
    }
}
