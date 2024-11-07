package com.futsch1.medtimer.reminders;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMAINING_REPEATS;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_EVENT_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REPEAT_TIME_SECONDS;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.ReminderEvent;

import java.time.Instant;

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

        int reminderId = inputData.getInt(EXTRA_REMINDER_ID, 0);
        int reminderEventId = inputData.getInt(EXTRA_REMINDER_EVENT_ID, 0);
        int repeatTimeSeconds = inputData.getInt(EXTRA_REPEAT_TIME_SECONDS, 0);
        int remainingRepeats = inputData.getInt(EXTRA_REMAINING_REPEATS, 0);
        Instant remindTime = Instant.now().plusSeconds(repeatTimeSeconds);

        ReminderNotificationData reminderNotificationData = new ReminderNotificationData(
                remindTime,
                reminderId,
                "Repeat",
                reminderEventId);
        enqueueNotification(reminderNotificationData);

        updateRemainingRepeats(reminderEventId, remainingRepeats - 1);

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
