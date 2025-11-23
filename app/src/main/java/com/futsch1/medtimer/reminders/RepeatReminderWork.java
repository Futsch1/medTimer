package com.futsch1.medtimer.reminders;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMAINING_REPEATS;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REPEAT_TIME_SECONDS;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.ReminderEvent;

/**
 * Worker that schedules a repeat of the current reminder.
 */
public class RepeatReminderWork extends SnoozeWork {

    public RepeatReminderWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Data inputData = getInputData();

        ScheduledNotification scheduledNotification = ScheduledNotification.Companion.fromInputData(inputData);
        int repeatTimeSeconds = inputData.getInt(EXTRA_REPEAT_TIME_SECONDS, 0);
        int remainingRepeats = inputData.getInt(EXTRA_REMAINING_REPEATS, 0);
        scheduledNotification.delayBy(repeatTimeSeconds);

        enqueueNotification(scheduledNotification);

        for (int reminderEventId : scheduledNotification.getReminderEventIds()) {
            updateRemainingRepeats(reminderEventId, remainingRepeats - 1);
        }

        return Result.success();
    }

    private void updateRemainingRepeats(int reminderEventId, int remainingRepeats) {
        MedicineRepository medicineRepository = new MedicineRepository((Application) getApplicationContext());
        ReminderEvent reminderEvent = medicineRepository.getReminderEvent(reminderEventId);
        if (reminderEvent != null) {
            reminderEvent.remainingRepeats = remainingRepeats;
            medicineRepository.updateReminderEvent(reminderEvent);
        }
    }
}
