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
        Uri sound = getNotificationRingtone(context);
        int importance = getNotificationImportance(context);
        if (channel == null || !channel.getSound().equals(sound) || channel.getImportance() != importance) {
            createChannelInternal(context, sound, importance);
        }
    }

    private static NotificationChannel getNotificationChannel(Context context) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        return notificationManager.getNotificationChannel(getNotificationChannelId(context));
    }

    private static Uri getNotificationRingtone(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String ringtoneUri = sharedPref.getString("notification_ringtone", "content://settings/system/notification_sound");
        return Uri.parse(ringtoneUri);
    }

    private static int getNotificationImportance(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String notificationImportance = sharedPref.getString("notification_importance", "0");
        return getNotificationImportanceValue(notificationImportance);
    }

    private static void createChannelInternal(Context context, Uri sound, int importance) {
        NotificationChannel channel;
        CharSequence name = context.getString(R.string.channel_name);
        String description = context.getString(R.string.channel_description);

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

    private static int getNotificationImportanceValue(String notificationImportance) {
        return notificationImportance.equals("0") ? NotificationManager.IMPORTANCE_DEFAULT : NotificationManager.IMPORTANCE_HIGH;
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

    public static void updateNotificationChannelRingtone(Context context, Uri ringtone) {
        createChannelInternal(context, ringtone, getNotificationImportance(context));
    }

    public static void updateNotificationChannelImportance(Context context, String importance) {
        createChannelInternal(context, getNotificationRingtone(context), getNotificationImportanceValue(importance));
    }
}
