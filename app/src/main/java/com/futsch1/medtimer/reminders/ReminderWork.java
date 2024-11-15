package com.futsch1.medtimer.reminders;

import static android.Manifest.permission.POST_NOTIFICATIONS;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_DATE;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_EVENT_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_TIME;
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
import com.futsch1.medtimer.PreferencesNames;
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
        Log.i(LogTags.REMINDER, "Do reminder work");
        Data inputData = getInputData();

        medicineRepository = new MedicineRepository((Application) getApplicationContext());
        Reminder reminder = getReminder(medicineRepository, inputData);

        Result r = (reminder != null) ? processReminder(inputData, reminder) : Result.failure();

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

    private Result processReminder(Data inputData, Reminder reminder) {
        Result r = Result.failure();
        int reminderEventId = inputData.getInt(EXTRA_REMINDER_EVENT_ID, 0);
        LocalDate reminderDate = LocalDate.ofEpochDay(inputData.getLong(EXTRA_REMINDER_DATE, LocalDate.now().toEpochDay()));
        LocalTime reminderTime = reminder.linkedReminderId == 0 ? LocalTime.now() : LocalTime.ofSecondOfDay(inputData.getInt(EXTRA_REMINDER_TIME, LocalTime.now().toSecondOfDay()));
        LocalDateTime reminderDateTime = LocalDateTime.of(reminderDate, reminderTime);
        Medicine medicine = medicineRepository.getMedicine(reminder.medicineRelId);
        ReminderEvent reminderEvent =
                reminderEventId == 0 ?
                        buildAndInsertReminderEvent(reminderDateTime, medicine, reminder) :
                        medicineRepository.getReminderEvent(reminderEventId);

        if (reminderEvent != null && medicine != null) {
            performActionsOfReminder(reminder, reminderEvent, medicine, reminderDateTime);
            r = Result.success();
        }
        return r;
    }

    private ReminderEvent buildAndInsertReminderEvent(LocalDateTime remindedDateTime, Medicine medicine, Reminder reminder) {
        ReminderEvent reminderEvent = buildReminderEvent(remindedDateTime, medicine, reminder);
        if (reminderEvent != null) {
            reminderEvent.remainingRepeats = getNumberOfRepeats();
            reminderEvent.reminderEventId = (int) medicineRepository.insertReminderEvent(reminderEvent);
        }
        return reminderEvent;
    }

    private void performActionsOfReminder(Reminder reminder, ReminderEvent reminderEvent, Medicine medicine, LocalDateTime reminderDateTime) {
        NotificationAction.cancelNotification(context, reminderEvent.notificationId);

        showNotification(medicine, reminderEvent, reminder, reminderDateTime);

        if (reminderEvent.remainingRepeats > 0 && isRepeatReminders()) {
            ReminderProcessor.requestRepeat(context, reminder.reminderId, reminderEvent.reminderEventId, getRepeatTimeSeconds(), reminderEvent.remainingRepeats);
        }

        Log.i(LogTags.REMINDER, String.format("Show reminder event %d for %s", reminderEvent.reminderEventId, reminderEvent.medicineName));
    }

    public static ReminderEvent buildReminderEvent(LocalDateTime remindedDateTime, Medicine medicine, Reminder reminder) {
        if (medicine != null && reminder != null) {
            ReminderEvent reminderEvent = new ReminderEvent();
            reminderEvent.reminderId = reminder.reminderId;
            reminderEvent.remindedTimestamp = remindedDateTime.toEpochSecond(ZoneId.systemDefault().getRules().getOffset(Instant.now()));
            reminderEvent.amount = reminder.amount;
            reminderEvent.medicineName = medicine.name + CyclesHelper.getCycleCountString(reminder);
            reminderEvent.color = medicine.color;
            reminderEvent.useColor = medicine.useColor;
            reminderEvent.status = ReminderEvent.ReminderStatus.RAISED;
            reminderEvent.iconId = medicine.iconId;

            return reminderEvent;
        } else {
            return null;
        }
    }

    private int getNumberOfRepeats() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return Integer.parseInt(sharedPref.getString(PreferencesNames.NUMBER_OF_REPETITIONS, "3"));
    }

    private void showNotification(Medicine medicine, ReminderEvent reminderEvent, Reminder reminder, LocalDateTime reminderDateTime) {
        if (canShowNotifications()) {
            Color color = medicine.useColor ? Color.valueOf(medicine.color) : null;
            Notifications notifications = new Notifications(context);
            reminderEvent.notificationId =
                    notifications.showNotification(minutesToTimeString(context, reminderDateTime.getHour() * 60L + reminderDateTime.getMinute()),
                            reminderEvent.medicineName,
                            reminder.amount,
                            reminder.instructions,
                            reminder.reminderId,
                            reminderEvent.reminderEventId,
                            color,
                            medicine.iconId,
                            medicine.notificationImportance == ReminderNotificationChannelManager.Importance.HIGH.getValue() ? ReminderNotificationChannelManager.Importance.HIGH : ReminderNotificationChannelManager.Importance.DEFAULT);
            medicineRepository.updateReminderEvent(reminderEvent);
        }
    }

    private boolean isRepeatReminders() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return sharedPref.getBoolean(PreferencesNames.REPEAT_REMINDERS, false);
    }

    private int getRepeatTimeSeconds() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return Integer.parseInt(sharedPref.getString(PreferencesNames.REPEAT_DELAY, "10")) * 60;
    }

    private boolean canShowNotifications() {
        return context.checkSelfPermission(POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }
}
