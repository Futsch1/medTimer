package com.futsch1.medtimer.reminders;

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
import com.futsch1.medtimer.NotificationChannelManager;
import com.futsch1.medtimer.R;

public class Notifications {
    private final Context context;
    private final SharedPreferences sharedPreferences;

    public Notifications(@NonNull Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences("medtimer.data", Context.MODE_PRIVATE);
    }

    public int showNotification(String remindTime, String medicineName, String amount, String instructions, int reminderId, int reminderEventId, Color color) {
        int notificationId = getNextNotificationId();
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

        Intent notifyTaken = ReminderProcessor.getTakenActionIntent(context, reminderEventId);
        PendingIntent pendingTaken = PendingIntent.getBroadcast(context, notificationId, notifyTaken, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent contentIntent = getStartAppIntent(notificationId);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationChannelManager.getNotificationChannelId(context))
                .setSmallIcon(R.drawable.capsule)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(getNotificationString(remindTime, amount, medicineName, instructions))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(contentIntent)
                .addAction(R.drawable.check2_circle, context.getString(R.string.notification_taken), pendingTaken);
        if (color != null) {
            builder = builder.setColor(color.toArgb()).setColorized(true);
        }

        buildActions(builder, notificationId, reminderEventId, reminderId);

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

    private String getNotificationString(String remindTime, String amount, String medicineName, String instructions) {
        if (!instructions.isEmpty()) {
            instructions = " " + instructions;
        }

        return context.getString(R.string.notification_content, remindTime, amount, medicineName, instructions);
    }

    private void buildActions(NotificationCompat.Builder builder, int notificationId, int reminderEventId, int reminderId) {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String dismissNotificationAction = defaultSharedPreferences.getString("dismiss_notification_action", "0");
        int snoozeTime = Integer.parseInt(defaultSharedPreferences.getString("snooze_duration", "15"));

        Intent snooze = ReminderProcessor.getSnoozeIntent(context, reminderId, reminderEventId, notificationId, snoozeTime);
        PendingIntent pendingSnooze = PendingIntent.getBroadcast(context, notificationId, snooze, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent notifyDismissed = ReminderProcessor.getDismissedActionIntent(context, reminderEventId);
        PendingIntent pendingDismissed = PendingIntent.getBroadcast(context, notificationId, notifyDismissed, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        if (dismissNotificationAction.equals("0")) {
            builder.addAction(R.drawable.hourglass_split, context.getString(R.string.notification_snooze), pendingSnooze);
            builder.setDeleteIntent(pendingDismissed);
        } else {
            builder.addAction(R.drawable.x_circle, context.getString(R.string.notification_skip), pendingSnooze);
            builder.setDeleteIntent(pendingSnooze);
        }
    }

}
