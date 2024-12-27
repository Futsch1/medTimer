package com.futsch1.medtimer;

import android.content.Context;

import androidx.test.espresso.idling.concurrent.IdlingThreadPoolExecutor;
import androidx.work.Configuration;
import androidx.work.WorkManager;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class WorkManagerAccess {
    private WorkManagerAccess() {
        // Intentionally empty
    }

    public static WorkManager getWorkManager(Context context) {
        if (!WorkManager.isInitialized()) {
            IdlingThreadPoolExecutor executor = new IdlingThreadPoolExecutor(
                    "MedTimerWorkManagerExecutor", 1, 1, 100, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                    new ThreadFactory() {
                        private int count = 1;

                        @Override
                        public Thread newThread(Runnable r) {
                            return new Thread(r, "MedTimerWorkManager-" + count++);
                        }
                    });
            // Make sure work manager runs in a single thread to avoid race conditions
            WorkManager.initialize(
                    context,
                    new Configuration.Builder()
                            .setExecutor(executor)
                            .setTaskExecutor(executor)
                            .build());
        }
        return WorkManager.getInstance(context);
    }
}
