package com.futsch1.medtimer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;

import com.futsch1.medtimer.database.FullMedicine;
import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.reminders.ReminderProcessor;

import java.util.List;

public class ReminderSchedulerService extends LifecycleService {

    private PhoneWearModule phoneWearModule;

    @Nullable
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        super.onBind(intent);
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notifyErrorPatch();

        MedicineRepository medicineRepository = new MedicineRepository(this.getApplication());

        medicineRepository.getLiveMedicines().observe(this, this::updateMedicine);

        phoneWearModule = new PhoneWearModule(this );
        phoneWearModule.checkNotifications(CoroutineCallback.Companion.call((fooBar, error) -> {
            //do something with result or error
            Log.i(LogTags.SCHEDULER, "sss"+ fooBar);
        }));

        Log.i(LogTags.SCHEDULER, "Service created");
    }

    private void notifyErrorPatch() {
        String CHANNEL_ID = "my_channel_01";
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT);

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("")
                .setContentText("").build();

        startForeground(1, notification);

    }

    public void updateMedicine(List<FullMedicine> ignoredFullMedicine) {
        Log.i(LogTags.SCHEDULER, "update");
        scheduleRequest();
    }

    private void loadWearIntegration() {
        phoneWearModule.connectedNodes(CoroutineCallback.Companion.call((fooBar, error) -> {
            //do something with result or error
            Log.i(LogTags.SCHEDULER, "connectedNodes "+ fooBar);
        }));
        phoneWearModule.isAvailable(CoroutineCallback.Companion.call((fooBar, error) -> {
            //do something with result or error
            Log.i(LogTags.SCHEDULER, "isAvailable "+ fooBar);
        }));
    }

    private void scheduleRequest() {
        ReminderProcessor.requestReschedule(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        phoneWearModule.close();

        Log.i(LogTags.SCHEDULER, "Service destroyed");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        scheduleRequest();
        return START_STICKY;
    }
}
