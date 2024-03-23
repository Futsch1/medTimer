package com.futsch1.medtimer;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.futsch1.medtimer.reminders.ReminderProcessor;

public class Notifications {

    private Notifications() {
        // Intentionally empty
    }

    public static int showNotification(@NonNull Context context, String remindTime, String medicineName, String amount, String instructions, int reminderEventId, Color color) {
        int notificationId = getNextNotificationId(context);
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

        Intent notifyTaken = ReminderProcessor.getTakenActionIntent(context, reminderEventId);
        PendingIntent pendingTaken = PendingIntent.getBroadcast(context, notificationId, notifyTaken, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent notifyDismissed = ReminderProcessor.getDismissedActionIntent(context, reminderEventId);
        PendingIntent pendingDismissed = PendingIntent.getBroadcast(context, notificationId, notifyDismissed, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent contentIntent = getStartAppIntent(context, notificationId);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationChannelManager.getNotificationChannelId(context))
                .setSmallIcon(R.drawable.capsule)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(getNotificationString(context, remindTime, amount, medicineName, instructions))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(contentIntent)
                .setDeleteIntent(pendingDismissed)
                .addAction(R.drawable.capsule, context.getString(R.string.notification_taken), pendingTaken);
        if (color != null) {
            builder = builder.setColor(color.toArgb()).setColorized(true);
        }

        notificationManager.notify(notificationId, builder.build());
        Log.d(LogTags.REMINDER, String.format("Created notification %d", notificationId));

        return notificationId;
    }

    private static int getNextNotificationId(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("medtimer.data", Context.MODE_PRIVATE);
        sharedPreferences.edit().apply();
        int notificationId = sharedPreferences.getInt("notificationId", 1);
        sharedPreferences.edit().putInt("notificationId", notificationId + 1).apply();

        return notificationId;
    }

    private static PendingIntent getStartAppIntent(@NonNull Context context, int notificationId) {
        Intent startApp = new Intent(context, MainActivity.class);
        startApp.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, notificationId, startApp, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static String getNotificationString(@NonNull Context context, String remindTime, String amount, String medicineName, String instructions) {
        if (!instructions.isEmpty()) {
            instructions = " " + instructions;
        }

        return context.getString(R.string.notification_content, remindTime, amount, medicineName, instructions);
    }

}
