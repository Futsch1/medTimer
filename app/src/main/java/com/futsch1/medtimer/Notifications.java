package com.futsch1.medtimer;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_NOTIFICATION_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_EVENT_ID;
import static com.futsch1.medtimer.ActivityCodes.NOTIFICATION_CHANNEL_ID;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

public class Notifications {

    private final Context context;
    private int notificationId = 1;

    public Notifications(@NonNull Context context) {
        this.context = context;
    }

    public void showNotification(String medicineName, String amount, int reminderEventId) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

        Intent notifyTaken = new Intent(context, TakenService.class);
        notifyTaken.putExtra(EXTRA_NOTIFICATION_ID, notificationId++);
        notifyTaken.putExtra(EXTRA_REMINDER_EVENT_ID, reminderEventId);
        PendingIntent pendingTaken = PendingIntent.getService(context, 0, notifyTaken, PendingIntent.FLAG_IMMUTABLE);

        Intent notifyDismissed = new Intent(context, DismissService.class);
        notifyDismissed.putExtra(EXTRA_REMINDER_EVENT_ID, reminderEventId);
        PendingIntent pendingDismissed = PendingIntent.getService(context, 0, notifyDismissed, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.capsule)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(context.getString(R.string.notification_content, amount, medicineName))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingTaken)
                .setDeleteIntent(pendingDismissed)
                .setAutoCancel(true)
                .addAction(R.drawable.capsule, context.getString(R.string.notification_taken), pendingTaken);

        notificationManager.notify(notificationId, builder.build());
    }
}
