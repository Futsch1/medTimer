package com.futsch1.medtimer;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.widget.Toast;

public class DismissService extends Service {
    public DismissService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        HandlerThread backgroundThread = new HandlerThread("BackgroundThread");
        backgroundThread.start();
        Handler handler = new Handler(backgroundThread.getLooper());

        NotificationManager notificationManager = getApplicationContext().getSystemService(NotificationManager.class);
        notificationManager.cancel(12);

        Runnable task = () -> {
            Toast.makeText(getApplicationContext(), "Start dismissed", Toast.LENGTH_LONG).show();
        };

        handler.post(task);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}