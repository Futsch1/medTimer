package com.futsch1.medtimer;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_DATE;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_EVENT_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_ID;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkRequest;

import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.reminders.ReminderProcessor;
import com.futsch1.medtimer.reminders.ReminderWork;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

public class Autostart extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && (intent.getAction().equals("android.intent.action.BOOT_COMPLETED") || intent.getAction().equals("android.intent.action.MY_PACKAGE_REPLACED"))) {
            Log.i("Autostart", "Requesting reschedule");
            ReminderProcessor.requestReschedule(context);
            Log.i("Autostart", "Restore reminders");
            restoreReminders(context);
        }
    }

    private void restoreReminders(Context context) {
        MedicineRepository repo = new MedicineRepository((Application) context.getApplicationContext());
        HandlerThread thread = new HandlerThread("RestoreReminders");
        thread.start();
        new Handler(thread.getLooper()).post(() -> {
            //noinspection SimplifyStreamApiCallChains
            @SuppressWarnings("java:S6204") // Using SDK 33
            List<ReminderEvent> reminderEventList = repo.getLastDaysReminderEvents(1).stream().filter((reminderEvent -> reminderEvent.status == ReminderEvent.ReminderStatus.RAISED)).collect(Collectors.toUnmodifiableList());
            for (ReminderEvent reminderEvent : reminderEventList) {
                long raiseDays = Instant.ofEpochSecond(reminderEvent.remindedTimestamp).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay();
                WorkRequest reminderWork =
                        new OneTimeWorkRequest.Builder(ReminderWork.class)
                                .setInputData(new Data.Builder()
                                        .putInt(EXTRA_REMINDER_ID, reminderEvent.reminderId)
                                        .putInt(EXTRA_REMINDER_EVENT_ID, reminderEvent.reminderEventId)
                                        .putLong(EXTRA_REMINDER_DATE, raiseDays)
                                        .build())
                                .build();
                WorkManagerAccess.getWorkManager(context).enqueue(reminderWork);
            }
        });
    }
}
