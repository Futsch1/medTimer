package com.futsch1.medtimer.reminders;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.futsch1.medtimer.LogTags;
import com.futsch1.medtimer.MainActivity;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.ReminderNotificationChannelManager;
import com.futsch1.medtimer.database.FullMedicine;
import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.helpers.MedicineHelper;
import com.futsch1.medtimer.helpers.MedicineIcons;

import java.util.List;
import java.util.stream.Collectors;

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
    public int showNotification(String remindTime, FullMedicine medicine, Reminder reminder, ReminderEvent reminderEvent) {
        int notificationId = getNextNotificationId();
        ReminderNotificationChannelManager.Importance importance = (medicine.medicine.notificationImportance == ReminderNotificationChannelManager.Importance.HIGH.getValue()) ? ReminderNotificationChannelManager.Importance.HIGH : ReminderNotificationChannelManager.Importance.DEFAULT;
        Color color = medicine.medicine.useColor ? Color.valueOf(medicine.medicine.color) : null;
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

        PendingIntent contentIntent = getStartAppIntent(notificationId);

        String notificationChannelId = ReminderNotificationChannelManager.Companion.getNotificationChannel(context, importance).getId();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, notificationChannelId)
                .setSmallIcon(R.drawable.capsule)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(getNotificationString(remindTime, reminder, medicine))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(contentIntent);
        if (medicine.medicine.iconId != 0) {
            MedicineIcons icons = new MedicineIcons(context);
            builder.setLargeIcon(icons.getIconBitmap(medicine.medicine.iconId));
        }
        if (color != null) {
            builder = builder.setColor(color.toArgb()).setColorized(true);
        }

        buildActions(builder, notificationId, reminderEvent.reminderEventId, reminder);

        notificationManager.notify(notificationId, builder.build());
        Log.d(LogTags.REMINDER, String.format("Created notification %d", notificationId));

        return notificationId;
    }

    private int getNextNotificationId() {
        sharedPreferences.edit().apply();
        int notificationId = sharedPreferences.getInt("notificationId", 1);
        sharedPreferences.edit().putInt("notificationId", notificationId + 1).apply();

        return notificationId;
    }

    private PendingIntent getStartAppIntent(int notificationId) {
        Intent startApp = new Intent(context, MainActivity.class);
        startApp.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, notificationId, startApp, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private String getNotificationString(String remindTime, Reminder reminder, FullMedicine medicine) {
        String instructions = reminder.instructions;
        if (instructions == null) {
            instructions = "";
        }
        if (!instructions.isEmpty()) {
            instructions = " " + instructions;
        }
        final int amountStringId = reminder.amount.isBlank() ? R.string.notification_content_blank : R.string.notification_content;
        String medicineNameString = MedicineHelper.getMedicineNameWithStockText(context, medicine.medicine);
        String notificationString = context.getString(amountStringId, remindTime, reminder.amount, medicineNameString, instructions);
        @SuppressWarnings("java:S6204")
        List<String> tagNames = medicine.tags.stream().map(t -> t.name).collect(Collectors.toList());
        if (tagNames.isEmpty()) {
            return notificationString;
        }
        return notificationString + "\n(" + String.join(", ", tagNames) + ")";
    }

    private void buildActions(NotificationCompat.Builder builder, int notificationId, int reminderEventId, Reminder reminder) {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String dismissNotificationAction = defaultSharedPreferences.getString("dismiss_notification_action", "0");
        int snoozeTime = Integer.parseInt(defaultSharedPreferences.getString("snooze_duration", "15"));

        PendingIntent pendingSnooze = getSnoozePendingIntent(context, reminder.reminderId, reminderEventId, notificationId, snoozeTime);

        Intent notifyDismissed = ReminderProcessor.getDismissedActionIntent(context, reminderEventId);
        PendingIntent pendingDismissed = PendingIntent.getBroadcast(context, notificationId, notifyDismissed, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent pendingTaken = getTakenPendingIntent(notificationId, reminderEventId, reminder);

        if (dismissNotificationAction.equals("0")) {
            builder.addAction(R.drawable.check2_circle, context.getString(R.string.taken), pendingTaken);
            builder.addAction(R.drawable.hourglass_split, context.getString(R.string.snooze), pendingSnooze);
            builder.setDeleteIntent(pendingDismissed);
        } else if (dismissNotificationAction.equals("1")) {
            builder.addAction(R.drawable.check2_circle, context.getString(R.string.taken), pendingTaken);
            builder.addAction(R.drawable.x_circle, context.getString(R.string.skipped), pendingDismissed);
            builder.setDeleteIntent(pendingSnooze);
        } else {
            builder.addAction(R.drawable.x_circle, context.getString(R.string.skipped), pendingDismissed);
            builder.addAction(R.drawable.hourglass_split, context.getString(R.string.snooze), pendingSnooze);
            builder.setDeleteIntent(pendingTaken);
        }
    }

    private PendingIntent getSnoozePendingIntent(Context context, int reminderId, int reminderEventId, int notificationId, int snoozeTime) {
        if (snoozeTime == -1) {
            Intent snooze = ReminderProcessor.getCustomSnoozeActionIntent(context, reminderId, reminderEventId, notificationId);
            return PendingIntent.getActivity(context, notificationId, snooze, PendingIntent.FLAG_IMMUTABLE);
        } else {
            Intent snooze = ReminderProcessor.getSnoozeIntent(context, reminderId, reminderEventId, notificationId, snoozeTime);
            return PendingIntent.getBroadcast(context, notificationId, snooze, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }

    private PendingIntent getTakenPendingIntent(int notificationId, int reminderEventId, Reminder reminder) {
        if (reminder.variableAmount) {
            Intent notifyTaken = ReminderProcessor.getVariableAmountActionIntent(context, reminderEventId, reminder.amount);
            return PendingIntent.getActivity(context, notificationId, notifyTaken, PendingIntent.FLAG_IMMUTABLE);
        } else {
            Intent notifyTaken = ReminderProcessor.getTakenActionIntent(context, reminderEventId);
            return PendingIntent.getBroadcast(context, notificationId, notifyTaken, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }

    public void showOutOfStockNotification(Medicine medicine) {
        int notificationId = getNextNotificationId();
        ReminderNotificationChannelManager.Importance importance = (medicine.notificationImportance == ReminderNotificationChannelManager.Importance.HIGH.getValue()) ? ReminderNotificationChannelManager.Importance.HIGH : ReminderNotificationChannelManager.Importance.DEFAULT;
        Color color = medicine.useColor ? Color.valueOf(medicine.color) : null;
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

        PendingIntent contentIntent = getStartAppIntent(notificationId);

        String notificationChannelId = ReminderNotificationChannelManager.Companion.getNotificationChannel(context, importance).getId();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, notificationChannelId)
                .setSmallIcon(R.drawable.box_seam)
                .setContentTitle(context.getString(R.string.out_of_stock_notification_title))
                .setContentText(context.getString(R.string.out_of_stock_notification, medicine.name, MedicineHelper.formatAmount(medicine.amount, medicine.unit)))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(contentIntent);
        if (medicine.iconId != 0) {
            MedicineIcons icons = new MedicineIcons(context);
            builder.setLargeIcon(icons.getIconBitmap(medicine.iconId));
        }
        if (color != null) {
            builder = builder.setColor(color.toArgb()).setColorized(true);
        }

        notificationManager.notify(notificationId, builder.build());
        Log.d(LogTags.REMINDER, String.format("Created notification %d", notificationId));
    }

}
