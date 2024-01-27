package com.futsch1.medtimer;

import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleService;

import com.futsch1.medtimer.database.MedicineRepository;

import java.time.Instant;

public class ReminderSchedulerService extends LifecycleService {
    public static boolean serviceRunning = false;
    private ReminderProcessor reminderProcessor;

    public ReminderSchedulerService() {
    }

    @Nullable
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        super.onBind(intent);
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        serviceRunning = true;

        MedicineRepository medicineRepository = new MedicineRepository(this.getApplication());
        reminderProcessor = new ReminderProcessor(getApplicationContext(), medicineRepository, new Notifications(getApplicationContext()));
        ReminderScheduler reminderScheduler = new ReminderScheduler((timestamp, medicine, reminder) -> {
            reminderProcessor.schedule(timestamp, medicine, reminder);
        }, Instant::now);

        medicineRepository.getMedicines().observe(this, reminderScheduler::updateMedicine);
        medicineRepository.getReminderEvents().observe(this, reminderScheduler::updateReminderEvents);

        Log.i("Scheduler", "Service created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("Scheduler", "Service destroyed");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }
}
