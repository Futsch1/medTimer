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
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.futsch1.medtimer.LogTags;
import com.futsch1.medtimer.database.FullMedicine;
import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.preferences.PreferencesNames;
import com.futsch1.medtimer.reminders.scheduling.CyclesHelper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.stream.Collectors;

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
        Data inputData = getInputData();

        medicineRepository = new MedicineRepository((Application) getApplicationContext());
        Reminder reminder = getReminder(medicineRepository, inputData);

        Result r = (reminder != null) ? processReminder(inputData, reminder) : Result.failure();

        medicineRepository.flushDatabase();

        // Reminder shown, now schedule next reminder
        ReminderProcessor.requestReschedule(context);

        return r;
    }

    private Reminder getReminder(MedicineRepository medicineRepository, Data inputData) {
        int reminderId = inputData.getInt(EXTRA_REMINDER_ID, 0);
        Reminder reminder = medicineRepository.getReminder(reminderId);
        if (reminder == null) {
            Log.e(LogTags.REMINDER, String.format("Could not find reminder rID %d in database", reminderId));
        }
        return reminder;
    }

    private Result processReminder(Data inputData, Reminder reminder) {
        Result r = Result.failure();
        int reminderEventId = inputData.getInt(EXTRA_REMINDER_EVENT_ID, 0);
        LocalDate reminderDate = LocalDate.ofEpochDay(inputData.getLong(EXTRA_REMINDER_DATE, LocalDate.now().toEpochDay()));
        LocalTime reminderTime = reminder.getReminderType() == Reminder.ReminderType.TIME_BASED ? LocalTime.of(reminder.timeInMinutes / 60, reminder.timeInMinutes % 60) : LocalTime.ofSecondOfDay(inputData.getInt(EXTRA_REMINDER_TIME, LocalTime.now().toSecondOfDay()));
        LocalDateTime reminderDateTime = LocalDateTime.of(reminderDate, reminderTime);
        FullMedicine medicine = medicineRepository.getMedicine(reminder.medicineRelId);
        long remindedTimeStamp = reminderDateTime.toEpochSecond(ZoneId.systemDefault().getRules().getOffset(reminderDateTime));
        ReminderEvent reminderEvent = getOrCreateEvent(reminderEventId, remindedTimeStamp, reminder, medicine);

        if (reminderEvent != null && medicine != null) {
            Log.i(LogTags.REMINDER, String.format("Process reID %d for rID %d", reminderEvent.reminderEventId, reminderEvent.reminderId));
            performActionsOfReminder(reminder, reminderEvent, medicine, reminderDateTime);
            r = Result.success();
        }
        return r;
    }

    private ReminderEvent getOrCreateEvent(int reminderEventId, long remindedTimeStamp, Reminder reminder, FullMedicine medicine) {
        if (reminderEventId != 0) {
            return medicineRepository.getReminderEvent(reminderEventId);
        } else {
            ReminderEvent event = medicineRepository.getReminderEvent(reminder.reminderId, remindedTimeStamp);
            if (event != null) {
                return event;
            } else {
                return buildAndInsertReminderEvent(remindedTimeStamp, medicine, reminder);
            }
        }
    }

    private void performActionsOfReminder(Reminder reminder, ReminderEvent reminderEvent, FullMedicine medicine, LocalDateTime reminderDateTime) {
        if (reminder.automaticallyTaken) {
            NotificationAction.processReminderEvent(context, ReminderEvent.ReminderStatus.TAKEN, reminderEvent, medicineRepository);

            Log.i(LogTags.REMINDER, String.format("Mark reminder reID %d as automatically taken for %s", reminderEvent.reminderEventId, reminderEvent.medicineName));
        } else {
            notificationAction(reminder, reminderEvent, medicine, reminderDateTime);
        }
    }

    private ReminderEvent buildAndInsertReminderEvent(long remindedTimeStamp, FullMedicine medicine, Reminder reminder) {
        ReminderEvent reminderEvent = buildReminderEvent(remindedTimeStamp, medicine, reminder, medicineRepository);
        if (reminderEvent != null) {
            reminderEvent.remainingRepeats = getNumberOfRepeats();
            reminderEvent.reminderEventId = (int) medicineRepository.insertReminderEvent(reminderEvent);
        }
        return reminderEvent;
    }

    private void notificationAction(Reminder reminder, ReminderEvent reminderEvent, FullMedicine medicine, LocalDateTime reminderDateTime) {
        NotificationAction.cancelNotification(context, reminderEvent.notificationId);

        showNotification(medicine, reminderEvent, reminder, reminderDateTime);

        if (reminderEvent.remainingRepeats != 0 && isRepeatReminders()) {
            ReminderProcessor.requestRepeat(context, reminder.reminderId, reminderEvent.reminderEventId, getRepeatTimeSeconds(), reminderEvent.remainingRepeats);
        }

        Log.i(LogTags.REMINDER, String.format("Show reminder event %d for %s", reminderEvent.reminderEventId, reminderEvent.medicineName));
    }

    public static ReminderEvent buildReminderEvent(long remindedTimeStamp, FullMedicine medicine, Reminder reminder, MedicineRepository medicineRepository) {
        if (medicine != null) {
            ReminderEvent reminderEvent = new ReminderEvent();
            reminderEvent.reminderId = reminder.reminderId;
            reminderEvent.remindedTimestamp = remindedTimeStamp;
            reminderEvent.amount = reminder.amount;
            reminderEvent.medicineName = medicine.medicine.name + CyclesHelper.getCycleCountString(reminder);
            reminderEvent.color = medicine.medicine.color;
            reminderEvent.useColor = medicine.medicine.useColor;
            reminderEvent.status = ReminderEvent.ReminderStatus.RAISED;
            reminderEvent.iconId = medicine.medicine.iconId;
            reminderEvent.askForAmount = reminder.variableAmount;
            reminderEvent.tags = medicine.tags.stream().map(t -> t.name).collect((Collectors.toList()));
            if (reminder.isInterval()) {
                reminderEvent.lastIntervalReminderTimeInMinutes = getLastReminderEventTimeInMinutes(medicineRepository, reminderEvent);
            } else {
                reminderEvent.lastIntervalReminderTimeInMinutes = 0;
            }

            return reminderEvent;
        } else {
            return null;
        }
    }

    private int getNumberOfRepeats() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return Integer.parseInt(sharedPref.getString(PreferencesNames.NUMBER_OF_REPETITIONS, "3"));
    }

    private void notificationAction(Reminder reminder, ReminderEvent reminderEvent, FullMedicine medicine, LocalDateTime reminderDateTime) {
        NotificationAction.cancelNotification(context, reminderEvent.notificationId);

        showNotification(medicine, reminderEvent, reminder, reminderDateTime);

        if (reminderEvent.remainingRepeats != 0 && isRepeatReminders()) {
            ReminderProcessor.requestRepeat(context, reminder.reminderId, reminderEvent.reminderEventId, getRepeatTimeSeconds(), reminderEvent.remainingRepeats);
        }

        Log.i(LogTags.REMINDER, String.format("Show reminder event reID %d for %s", reminderEvent.reminderEventId, reminderEvent.medicineName));
    }

    private static int getLastReminderEventTimeInMinutes(MedicineRepository medicineRepository, ReminderEvent reminderEvent) {
        ReminderEvent lastReminderEvent = medicineRepository.getLastReminderEvent(reminderEvent.reminderId);
        if (lastReminderEvent != null && lastReminderEvent.status == ReminderEvent.ReminderStatus.TAKEN) {
            return (int) (lastReminderEvent.processedTimestamp / 60);
        } else {
            return 0;
        }
    }

    private void showNotification(FullMedicine medicine, ReminderEvent reminderEvent, Reminder reminder, LocalDateTime reminderDateTime) {
        if (canShowNotifications()) {
            boolean hasSameTimeReminders = !medicineRepository.getSameTimeReminders(reminder.reminderId).isEmpty();
            Notifications notifications = new Notifications(context);
            reminderEvent.notificationId =
                    notifications.showNotification(minutesToTimeString(context, reminderDateTime.getHour() * 60L + reminderDateTime.getMinute()),
                            medicine, reminder, reminderEvent, hasSameTimeReminders);
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

    private static int getLastReminderEventTimeInMinutes(MedicineRepository medicineRepository, ReminderEvent reminderEvent) {
        ReminderEvent lastReminderEvent = medicineRepository.getLastReminderEvent(reminderEvent.reminderId);
        if (lastReminderEvent != null && lastReminderEvent.status == ReminderEvent.ReminderStatus.TAKEN) {
            return (int) (lastReminderEvent.processedTimestamp / 60);
        } else {
            return 0;
        }
    }

    private boolean canShowNotifications() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || context.checkSelfPermission(POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }
}
