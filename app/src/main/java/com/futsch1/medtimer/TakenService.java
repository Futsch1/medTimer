package com.futsch1.medtimer;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_NOTIFICATION_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_EVENT_ID;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.ReminderEvent;

import java.time.Instant;

public class TakenService extends Service {
    private MedicineRepository medicineRepository;
    private HandlerThread backgroundThread;

    @Override
    public void onCreate() {
        medicineRepository = new MedicineRepository(this.getApplication());
        backgroundThread = new HandlerThread("BackgroundThread");
        backgroundThread.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Handler handler = new Handler(backgroundThread.getLooper());

        NotificationManager notificationManager = getApplicationContext().getSystemService(NotificationManager.class);
        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0);
        Log.d("Reminder", String.format("Dismissing notification %d", notificationId));
        notificationManager.cancel(notificationId);

        Runnable task = () -> {
            ReminderEvent reminderEvent = medicineRepository.getReminderEvent(intent.getIntExtra(EXTRA_REMINDER_EVENT_ID, 0));
            reminderEvent.status = ReminderEvent.ReminderStatus.TAKEN;
            reminderEvent.processedTimestamp = Instant.now().getEpochSecond();
            medicineRepository.updateReminderEvent(reminderEvent);
            Log.i("Reminder", String.format("Taken reminder for %s", reminderEvent.medicineName));
        };

        handler.post(task);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        backgroundThread.quitSafely();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}