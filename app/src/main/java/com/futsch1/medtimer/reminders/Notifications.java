package com.futsch1.medtimer.reminders;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.futsch1.medtimer.LogTags;
import com.futsch1.medtimer.database.FullMedicine;
import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.reminders.notificationFactory.OutOfStockNotificationFactory;
import com.futsch1.medtimer.reminders.notificationFactory.ReminderNotificationFactory;
import com.futsch1.medtimer.reminders.notificationFactory.ReminderNotificationFactoryKt;

@SuppressLint("DefaultLocale")
public class Notifications {
    private final Context context;
    private final SharedPreferences sharedPreferences;

    @SuppressWarnings("java:S6300") // No sensitive data is stored in the shared preferences
    public Notifications(@NonNull Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences("medtimer.data", Context.MODE_PRIVATE);
    }

    @SuppressWarnings("java:S107")
    public int showNotification(String remindTime, FullMedicine medicine, Reminder reminder, ReminderEvent reminderEvent, boolean hasSameTimeReminders) {
        int notificationId = getNextNotificationId();

        ReminderNotificationFactory factory = ReminderNotificationFactoryKt.getReminderNotificationFactory(context, notificationId, remindTime, medicine, reminder, reminderEvent, hasSameTimeReminders);

        notify(notificationId, factory.create());
        Log.d(LogTags.REMINDER, String.format("Created notification %d", notificationId));

        return notificationId;
    }

    private int getNextNotificationId() {
        sharedPreferences.edit().apply();
        int notificationId = sharedPreferences.getInt("notificationId", 1);
        sharedPreferences.edit().putInt("notificationId", notificationId + 1).apply();

        return notificationId;
    }


    private void notify(int notificationId, Notification notification) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        NotificationSoundManager soundManager = new NotificationSoundManager(context);

        notificationManager.notify(notificationId, notification);

        soundManager.restore();
    }


    public void showOutOfStockNotification(Medicine medicine) {
        int notificationId = getNextNotificationId();

        OutOfStockNotificationFactory factory = new OutOfStockNotificationFactory(context, notificationId, medicine);

        notify(notificationId, factory.create());
        Log.d(LogTags.REMINDER, String.format("Created notification %d", notificationId));
    }
}
