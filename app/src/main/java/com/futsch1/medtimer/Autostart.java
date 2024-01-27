package com.futsch1.medtimer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class Autostart extends BroadcastReceiver {
    public void onReceive(Context context, Intent arg1) {
        Intent intent = new Intent(context, ReminderSchedulerService.class);
        context.startForegroundService(intent);
        Log.i("Service", "Started ReminderSchedulerService");
    }
}
