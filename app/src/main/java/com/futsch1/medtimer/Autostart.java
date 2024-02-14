package com.futsch1.medtimer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.futsch1.medtimer.reminders.ReminderProcessor;

public class Autostart extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && (intent.getAction().equals("android.intent.action.BOOT_COMPLETED") || intent.getAction().equals("android.intent.action.MY_PACKAGE_REPLACED"))) {
            Log.i("Autostart", "Requesting reschedule");
            ReminderProcessor.requestReschedule(context);
        }
    }
}
