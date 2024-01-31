package com.futsch1.medtimer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class Autostart extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            Intent serviceIntent = new Intent(context, ReminderSchedulerService.class);
            context.startForegroundService(serviceIntent);
            Log.i("Service", "Started Reminder  SchedulerService");
        }
    }
}
