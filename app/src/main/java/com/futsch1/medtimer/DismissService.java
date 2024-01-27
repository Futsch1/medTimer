package com.futsch1.medtimer;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_EVENT_ID;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.ReminderEvent;

import java.time.Instant;

public class DismissService extends Service {
    MedicineRepository medicineRepository;

    @Override
    public void onCreate() {
        medicineRepository = new MedicineRepository(this.getApplication());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        HandlerThread backgroundThread = new HandlerThread("BackgroundThread");
        backgroundThread.start();
        Handler handler = new Handler(backgroundThread.getLooper());

        Runnable task = () -> {
            ReminderEvent reminderEvent = medicineRepository.getReminderEvent(intent.getIntExtra(EXTRA_REMINDER_EVENT_ID, 0));
            reminderEvent.status = ReminderEvent.ReminderStatus.SKIPPED;
            reminderEvent.processedTimestamp = Instant.now().getEpochSecond();
            medicineRepository.updateReminderEvent(reminderEvent);
            Log.i("Reminder", String.format("Dismissed reminder for %s", reminderEvent.medicineName));
        };

        handler.post(task);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}