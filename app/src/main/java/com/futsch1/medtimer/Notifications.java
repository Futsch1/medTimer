package com.futsch1.medtimer;

import static com.futsch1.medtimer.ActivityCodes.NOTIFICATION_CHANNEL_ID;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

public class Notifications {

    private static int notificationId = 1;

    public static void showNotification(@NonNull Context context, String remindTime, String medicineName, String amount, int reminderEventId) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

        Intent notifyTaken = ReminderProcessor.getTakenActionIntent(context, notificationId, reminderEventId);
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

        notificationManager.notify(notificationId++, builder.build());
    }
}
