package com.futsch1.medtimer.reminders;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_DATE;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_TIME;

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

import com.futsch1.medtimer.CoroutineCallback;
import com.futsch1.medtimer.LogTags;
import com.futsch1.medtimer.PhoneWearModule;
import com.futsch1.medtimer.ScheduledReminder;
import com.futsch1.medtimer.WorkManagerAccess;
import com.futsch1.medtimer.database.FullMedicine;
import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.preferences.PreferencesNames;
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler;
import com.futsch1.medtimer.widgets.WidgetUpdateReceiver;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Worker that schedules the next reminder.
 */
public class RescheduleWork extends Worker {
    protected final Context context;
    private final AlarmManager alarmManager;
    private final WeekendMode weekendMode;
    private final PhoneWearModule phoneWearModule;

    public RescheduleWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        this.weekendMode = new WeekendMode(PreferenceManager.getDefaultSharedPreferences(context));
        alarmManager = context.getSystemService(AlarmManager.class);
        phoneWearModule = new PhoneWearModule(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i(LogTags.REMINDER, "Received scheduler request");
        MedicineRepository medicineRepository = new MedicineRepository((Application) getApplicationContext());
        ReminderScheduler reminderScheduler = getReminderScheduler();
        List<FullMedicine> fullMedicines = medicineRepository.getMedicines();
        List<ScheduledReminder> scheduledReminders = reminderScheduler.schedule(fullMedicines, medicineRepository.getLastDaysReminderEvents(33));
        if (!scheduledReminders.isEmpty()) {
            ScheduledReminder scheduledReminder = scheduledReminders.get(0);
            ReminderNotificationData reminderNotificationData = new ReminderNotificationData(
                    scheduledReminder.timestamp(),
                    scheduledReminder.reminder().reminderId,
                    scheduledReminder.medicine().medicine.name);
            phoneWearModule.addReminderNotificationData(reminderNotificationData,
                    CoroutineCallback.Companion.call((wearData, error) -> {
                        //do something with result or error
                        Log.i(LogTags.SCHEDULER, "addReminderData " + wearData);
                    }));
            this.enqueueNotification(
                    reminderNotificationData);
        } else {
            phoneWearModule.addReminderNotificationData(null,
                    CoroutineCallback.Companion.call((wearData, error) -> {
                        Log.i(LogTags.SCHEDULER, "addReminderData " + wearData);
                    }));
            this.cancelNextReminder();
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

    protected void enqueueNotification(ReminderNotificationData reminderNotificationData) {
        // Apply weekend mode shift
        Instant timestamp = weekendMode.adjustInstant(reminderNotificationData.timestamp());
        // If the alarm is in the future, schedule with alarm manager
        if (timestamp.isAfter(Instant.now())) {
            PendingIntent pendingIntent = new PendingIntentBuilder(context).
                    setReminderId(reminderNotificationData.reminderId).
                    setReminderEventId(reminderNotificationData.reminderEventId).
                    setReminderDateTime(timestamp.atZone(ZoneId.systemDefault()).toLocalDateTime()).build();

            // Cancel potentially already running alarm and set new
            alarmManager.cancel(pendingIntent);

            if (canScheduleExactAlarms(alarmManager)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timestamp.toEpochMilli(), pendingIntent);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timestamp.toEpochMilli(), pendingIntent);
            }

            Log.i(LogTags.SCHEDULER,
                    String.format("Scheduled reminder for %s/%d to %s",
                            reminderNotificationData.medicineName,
                            reminderNotificationData.reminderId,
                            timestamp));

            updateNextReminderWidget();

        } else {
            // Immediately remind
            ZonedDateTime reminderDateTime = timestamp.atZone(ZoneId.systemDefault());
            WorkRequest reminderWork =
                    new OneTimeWorkRequest.Builder(ReminderWork.class)
                            .setInputData(new Data.Builder()
                                    .putInt(EXTRA_REMINDER_ID, reminderNotificationData.reminderId)
                                    .putLong(EXTRA_REMINDER_DATE, reminderDateTime.toLocalDate().toEpochDay())
                                    .putInt(EXTRA_REMINDER_TIME, reminderDateTime.toLocalTime().toSecondOfDay())
                                    .build())
                            .build();
            WorkManagerAccess.getWorkManager(context).enqueue(reminderWork);
        }
    }

    private void cancelNextReminder() {
        // Pending reminders are distinguished by their request code, which is the reminder event id.
        // So if we cancel the reminderEventId 0, we cancel all the next reminder that was not yet raised.
        Intent intent = ReminderProcessor.getReminderAction(context, 0, 0, null);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
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

    public record ReminderNotificationData(Instant timestamp,
                                           int reminderId,
                                           String medicineName,
                                           int reminderEventId) {

        public ReminderNotificationData(Instant timestamp, int reminderId, String medicineName) {
            this(timestamp, reminderId, medicineName, 0);
        }
    }
}
