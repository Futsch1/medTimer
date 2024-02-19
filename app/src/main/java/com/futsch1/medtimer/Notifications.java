package com.futsch1.medtimer;

import static com.futsch1.medtimer.ActivityCodes.NOTIFICATION_CHANNEL_ID;

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

    public static int showNotification(@NonNull Context context, String remindTime, String medicineName, String amount, int reminderEventId, Color color) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("medtimer.data", Context.MODE_PRIVATE);
        sharedPreferences.edit().apply();

        int notificationId = sharedPreferences.getInt("notificationId", 1);
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

        Intent notifyTaken = ReminderProcessor.getTakenActionIntent(context, reminderEventId);
        PendingIntent pendingTaken = PendingIntent.getBroadcast(context, notificationId, notifyTaken, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent notifyDismissed = ReminderProcessor.getDismissedActionIntent(context, reminderEventId);
        PendingIntent pendingDismissed = PendingIntent.getBroadcast(context, notificationId, notifyDismissed, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent startApp = new Intent(context, MainActivity.class);
        startApp.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, notificationId, startApp, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.capsule)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(context.getString(R.string.notification_content, remindTime, amount, medicineName))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(contentIntent)
                .setDeleteIntent(pendingDismissed)
                .addAction(R.drawable.capsule, context.getString(R.string.notification_taken), pendingTaken);
        if (color != null) {
            builder = builder.setColor(color.toArgb()).setColorized(true);
        }

        notificationManager.notify(notificationId, builder.build());
        Log.d(LogTags.REMINDER, String.format("Created notification %d", notificationId));

        context.getSharedPreferences("medtimer.data", Context.MODE_PRIVATE).edit().putInt("notificationId", notificationId + 1).apply();

        return notificationId;
    }
}
