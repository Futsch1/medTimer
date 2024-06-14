package com.futsch1.medtimer.reminders;

import static android.Manifest.permission.POST_NOTIFICATIONS;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_EVENT_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_ID;
import static com.futsch1.medtimer.helpers.TimeHelper.minutesToTimeString;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.futsch1.medtimer.LogTags;
import com.futsch1.medtimer.ReminderNotificationChannelManager;
import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.reminders.scheduling.CyclesHelper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public class ReminderWork extends Worker {
    private final Context context;
    private MedicineRepository medicineRepository;

    public ReminderWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        Result r = Result.failure();
        Log.i(LogTags.REMINDER, "Do reminder work");
        Data inputData = getInputData();

        medicineRepository = new MedicineRepository((Application) getApplicationContext());
        Reminder reminder = getReminder(medicineRepository, inputData);

        if (reminder != null) {
            int reminderEventId = inputData.getInt(EXTRA_REMINDER_EVENT_ID, 0);
            Medicine medicine = medicineRepository.getMedicine(reminder.medicineRelId);
            ReminderEvent reminderEvent = reminderEventId == 0 ? buildAndInsertReminderEvent(medicine, reminder) : medicineRepository.getReminderEvent(reminderEventId);

            if (reminderEvent != null && medicine != null) {
                showNotification(medicine, reminderEvent, reminder);

                Log.i(LogTags.REMINDER, String.format("Show reminder event %d for %s", reminderEvent.reminderEventId, reminderEvent.medicineName));
                r = Result.success();
            }
        }

        // Reminder shown, now schedule next reminder
        ReminderProcessor.requestReschedule(context);

        return r;
    }

    private Reminder getReminder(MedicineRepository medicineRepository, Data inputData) {
        int reminderId = inputData.getInt(EXTRA_REMINDER_ID, 0);
        Reminder reminder = medicineRepository.getReminder(reminderId);
        if (reminder == null) {
            Log.e(LogTags.REMINDER, String.format("Could not find reminder %d in database", reminderId));
        }
        return reminder;
    }

    private ReminderEvent buildAndInsertReminderEvent(Medicine medicine, Reminder reminder) {
        ReminderEvent reminderEvent = buildReminderEvent(medicine, reminder);
        if (reminderEvent != null) {
            reminderEvent.reminderEventId = (int) medicineRepository.insertReminderEvent(reminderEvent);
        }
        return reminderEvent;
    }

    private void showNotification(Medicine medicine, ReminderEvent reminderEvent, Reminder reminder) {
        if (canShowNotifications()) {
            Color color = medicine.useColor ? Color.valueOf(medicine.color) : null;
            Notifications notifications = new Notifications(context);
            reminderEvent.notificationId =
                    notifications.showNotification(minutesToTimeString(reminder.timeInMinutes),
                            reminderEvent.medicineName,
                            reminder.amount,
                            reminder.instructions,
                            reminder.reminderId,
                            reminderEvent.reminderEventId,
                            color,
                            reminder.notificationImportance == ReminderNotificationChannelManager.Importance.HIGH.getValue() ? ReminderNotificationChannelManager.Importance.HIGH : ReminderNotificationChannelManager.Importance.DEFAULT);
            medicineRepository.updateReminderEvent(reminderEvent);
        }
    }

    public static ReminderEvent buildReminderEvent(Medicine medicine, Reminder reminder) {
        if (medicine != null && reminder != null) {
            ReminderEvent reminderEvent = new ReminderEvent();
            reminderEvent.reminderId = reminder.reminderId;
            reminderEvent.remindedTimestamp = LocalDateTime.of(LocalDate.now(), LocalTime.of(reminder.timeInMinutes / 60, reminder.timeInMinutes % 60))
                    .toEpochSecond(ZoneId.systemDefault().getRules().getOffset(Instant.now()));
            reminderEvent.amount = reminder.amount;
            reminderEvent.medicineName = medicine.name + CyclesHelper.getCycleCountString(reminder);
            reminderEvent.color = medicine.color;
            reminderEvent.useColor = medicine.useColor;
            reminderEvent.status = ReminderEvent.ReminderStatus.RAISED;

            return reminderEvent;
        } else {
            return null;
        }
    }

    private boolean canShowNotifications() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean showNotificationSetInPreferences = sharedPref.getBoolean("show_notification", true);
        boolean hasPermission = context.checkSelfPermission(POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;

        return showNotificationSetInPreferences && hasPermission;
    }
}
