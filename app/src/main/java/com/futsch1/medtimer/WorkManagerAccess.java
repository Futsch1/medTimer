package com.futsch1.medtimer;

import android.content.Context;

import androidx.work.Configuration;
import androidx.work.WorkManager;

import java.util.concurrent.Executors;

public class WorkManagerAccess {
    private WorkManagerAccess() {
        // Intentionally empty
    }

    public static WorkManager getWorkManager(Context context) {
        if (!WorkManager.isInitialized()) {
            // Make sure work manager runs in a single thread to avoid race conditions
            WorkManager.initialize(
                    context,
                    new Configuration.Builder()
                            .setExecutor(Executors.newFixedThreadPool(1))
                            .build());
        }
        return WorkManager.getInstance(context);
    }
}
