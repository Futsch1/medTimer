package com.futsch1.medtimer;

import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleService;

import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.reminders.ReminderProcessor;

import java.util.List;

public class ReminderSchedulerService extends LifecycleService {
    public static boolean serviceRunning = false;

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

        medicineRepository.getLiveMedicines().observe(this, this::updateMedicine);

        Log.i("Scheduler", "Service created");
    }

    public void updateMedicine(List<MedicineWithReminders> ignoredMedicineWithReminders) {
        scheduleRequest();
    }

    private void scheduleRequest() {
        Log.i("Scheduler", "Requesting reschedule");
        ReminderProcessor.requestReschedule(this);
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
