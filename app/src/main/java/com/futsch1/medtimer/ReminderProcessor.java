package com.futsch1.medtimer;

import static com.futsch1.medtimer.ActivityCodes.REMINDER_ACTION;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;

import java.time.Instant;

public class ReminderProcessor extends BroadcastReceiver {
    private final AlarmManager alarmManager;
    private final Context context;
    private final MedicineRepository medicineRepository;
    private final Notifications notifications;
    private Medicine pendingMedicine;
    private Reminder pendingReminder;
    private PendingIntent pendingIntent;

    public ReminderProcessor(Context context, MedicineRepository medicineRepository, Notifications notifications) {
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        this.context = context;
        this.medicineRepository = medicineRepository;
        this.notifications = notifications;
    }

    public void schedule(Instant timestamp, Medicine medicine, Reminder reminder) {
        if (timestamp.isAfter(Instant.now())) {
            if (pendingReminder == null || (pendingReminder.timeInMinutes != reminder.timeInMinutes) || (pendingReminder.reminderId != reminder.reminderId)) {
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent);
                }
                pendingMedicine = medicine;
                pendingReminder = reminder;

                Intent reminderIntent = new Intent(REMINDER_ACTION);
                pendingIntent = PendingIntent.getBroadcast(context, 100, reminderIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timestamp.toEpochMilli(), pendingIntent);

                Log.i("Scheduler", String.format("Scheduled reminder for %s to %s", pendingMedicine.name, timestamp));
            }
        } else {
            processReminder(reminder, medicine);
        }
    }

    private void processReminder(Reminder reminder, Medicine medicine) {
        ReminderEvent reminderEvent = new ReminderEvent();
        reminderEvent.reminderId = reminder.reminderId;
        reminderEvent.raisedTimestamp = Instant.now().getEpochSecond();
        reminderEvent.amount = reminder.amount;
        reminderEvent.medicineName = medicine.name;
        reminderEvent.status = ReminderEvent.ReminderStatus.RAISED;

        reminderEvent.reminderEventId = (int) medicineRepository.insertReminderEvent(reminderEvent);

        notifications.showNotification(medicine.name, reminder.amount, reminderEvent.reminderEventId);
        Log.i("Reminder", String.format("Show reminder for %s", reminderEvent.medicineName));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("Reminder", "Received reminder intent");
        Reminder reminder = pendingReminder;
        Medicine medicine = pendingMedicine;
        pendingReminder = null;
        pendingMedicine = null;
        pendingIntent = null;
        processReminder(reminder, medicine);
    }
}
