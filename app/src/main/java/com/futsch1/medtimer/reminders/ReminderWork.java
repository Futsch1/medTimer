package com.futsch1.medtimer.reminders;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_ID;
import static com.futsch1.medtimer.helpers.TimeHelper.minutesToTime;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.futsch1.medtimer.Notifications;
import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

public class ReminderWork extends Worker {
    public ReminderWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Result r;
        Log.i("Reminder", "Do reminder work");
        Data inputData = getInputData();

        MedicineRepository medicineRepository = new MedicineRepository((Application) getApplicationContext());
        Reminder reminder = medicineRepository.getReminder(inputData.getInt(EXTRA_REMINDER_ID, 0));
        if (reminder != null) {
            Medicine medicine = medicineRepository.getMedicine(reminder.medicineRelId);
            ReminderEvent reminderEvent = new ReminderEvent();
            reminderEvent.reminderId = reminder.reminderId;
            reminderEvent.remindedTimestamp = LocalDateTime.of(LocalDate.now(), LocalTime.of(reminder.timeInMinutes / 60, reminder.timeInMinutes % 60))
                    .toEpochSecond(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
            reminderEvent.amount = reminder.amount;
            reminderEvent.medicineName = medicine.name;
            reminderEvent.status = ReminderEvent.ReminderStatus.RAISED;

            reminderEvent.reminderEventId = (int) medicineRepository.insertReminderEvent(reminderEvent);

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            if (sharedPref.getBoolean("show_notification", true)) {
                reminderEvent.notificationId = Notifications.showNotification(getApplicationContext(), minutesToTime(reminder.timeInMinutes), medicine.name, reminder.amount, reminderEvent.reminderEventId);
                medicineRepository.updateReminderEvent(reminderEvent);
            }

            Log.i("Reminder", String.format("Show reminder %d for %s", reminderEvent.reminderEventId, reminderEvent.medicineName));
            r = Result.success();

        } else {
            Log.e("Reminder", "Could not find reminder in database");
            r = Result.failure();
        }

        // Reminder shown, now schedule next reminder
        ReminderProcessor.requestReschedule(getApplicationContext());

        return r;
    }
}