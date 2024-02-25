package com.futsch1.medtimer;

import static com.futsch1.medtimer.ActivityCodes.NOTIFICATION_CHANNEL_ID;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.net.Uri;

import androidx.preference.PreferenceManager;

public class NotificationChannelManager {

    private NotificationChannelManager() {
        // Intentionally empty
    }

    public static void createNotificationChannel(Context context) {
        NotificationChannel channel = getNotificationChannel(context);
        Uri sound = getNotificationSound(context);
        if (channel == null || !channel.getSound().equals(sound)) {
            createChannelInternal(context, sound);
        }
    }

    private static NotificationChannel getNotificationChannel(Context context) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        return notificationManager.getNotificationChannel(getNotificationChannelId(context));
    }

    private static Uri getNotificationSound(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String ringtoneUri = sharedPref.getString("notification_ringtone", "content://settings/system/notification_sound");
        return Uri.parse(ringtoneUri);
    }

    private static void createChannelInternal(Context context, Uri sound) {
        NotificationChannel channel;
        CharSequence name = context.getString(R.string.channel_name);
        String description = context.getString(R.string.channel_description);

        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        channel = new NotificationChannel(getNextNotificationChannelId(context), name, importance);
        channel.setDescription(description);
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build();
        channel.setSound(sound, audioAttributes);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this.
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    public static String getNotificationChannelId(Context context) {
        final int notificationChannelNumber = getNotificationChannelNumber(context);
        return NOTIFICATION_CHANNEL_ID + notificationChannelNumber;
    }

    private static String getNextNotificationChannelId(Context context) {
        int notificationId = getNotificationChannelNumber(context);
        SharedPreferences sharedPreferences = context.getSharedPreferences("medtimer.data", Context.MODE_PRIVATE);
        sharedPreferences.edit().putInt("notificationChannelId", notificationId + 1).apply();

        return NOTIFICATION_CHANNEL_ID + (notificationId + 1);
    }

    private static int getNotificationChannelNumber(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("medtimer.data", Context.MODE_PRIVATE);
        sharedPreferences.edit().apply();
        return sharedPreferences.getInt("notificationChannelId", 1);
    }

    public static void updateNotificationChannel(Context context, Uri newSound) {
        createChannelInternal(context, newSound);
    }
}
