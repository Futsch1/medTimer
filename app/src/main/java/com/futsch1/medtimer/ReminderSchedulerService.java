package com.futsch1.medtimer;

import static com.futsch1.medtimer.ActivityCodes.RESCHEDULE_ACTION;

import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleService;

import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.database.ReminderEvent;

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
        medicineRepository.getLiveReminderEvents(0).observe(this, this::updateReminderEvents);

        Log.i("Scheduler", "Service created");
    }

    public void updateMedicine(List<MedicineWithReminders> medicineWithReminders) {
        scheduleRequest();
    }

    public void updateReminderEvents(List<ReminderEvent> reminderEvents) {
        scheduleRequest();
    }

    private void scheduleRequest() {
        Log.i("Scheduler", "Requesting reschedule");
        Intent i = new Intent(RESCHEDULE_ACTION);
        i.setClass(getApplicationContext(), ReminderProcessor.class);
        sendBroadcast(i);
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
