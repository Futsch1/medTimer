package com.futsch1.medtimer;

import static com.futsch1.medtimer.ActivityCodes.REMINDER_ACTION;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
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

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onCreate() {
        super.onCreate();

        serviceRunning = true;

        MedicineRepository medicineRepository = new MedicineRepository(this.getApplication());
        reminderProcessor = new ReminderProcessor(getApplicationContext(), medicineRepository, new Notifications(getApplicationContext()));
        ReminderScheduler reminderScheduler = new ReminderScheduler((timestamp, medicine, reminder) -> reminderProcessor.schedule(timestamp, medicine, reminder), Instant::now);

        medicineRepository.getMedicines().observe(this, reminderScheduler::updateMedicine);
        medicineRepository.getReminderEvents().observe(this, reminderScheduler::updateReminderEvents);

        registerReceiver(reminderProcessor, new IntentFilter(REMINDER_ACTION), Context.RECEIVER_EXPORTED);

        Log.i("Scheduler", "Service created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(reminderProcessor);

        Log.i("Scheduler", "Service destroyed");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }
}
