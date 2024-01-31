package com.futsch1.medtimer;

import static com.futsch1.medtimer.ActivityCodes.DISMISSED_ACTION;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_NOTIFICATION_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_EVENT_ID;
import static com.futsch1.medtimer.ActivityCodes.NOTIFICATION_CHANNEL_ID;
import static com.futsch1.medtimer.ActivityCodes.TAKEN_ACTION;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

public class Notifications {

    private static int notificationId = 1;

    public static void showNotification(@NonNull Context context, String medicineName, String amount, int reminderEventId) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

        Intent notifyTaken = new Intent(context, ReminderProcessor.class);
        notifyTaken.setAction(TAKEN_ACTION);
        notifyTaken.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
        notifyTaken.putExtra(EXTRA_REMINDER_EVENT_ID, reminderEventId);
        PendingIntent pendingTaken = PendingIntent.getBroadcast(context, notificationId, notifyTaken, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent notifyDismissed = new Intent(context, ReminderProcessor.class);
        notifyDismissed.setAction(DISMISSED_ACTION);
        notifyDismissed.putExtra(EXTRA_REMINDER_EVENT_ID, reminderEventId);
        PendingIntent pendingDismissed = PendingIntent.getBroadcast(context, 11, notifyDismissed, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.capsule)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(context.getString(R.string.notification_content, amount, medicineName))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingTaken)
                .setDeleteIntent(pendingDismissed)
                .addAction(R.drawable.capsule, context.getString(R.string.notification_taken), pendingTaken);

        notificationManager.notify(notificationId++, builder.build());
    }
}
