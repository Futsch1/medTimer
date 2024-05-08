package com.futsch1.medtimer.overview;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_TIME;
import static com.futsch1.medtimer.ActivityCodes.NEXT_REMINDER_ACTION;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.futsch1.medtimer.MedicineViewModel;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.reminders.ReminderProcessor;
import com.futsch1.medtimer.reminders.ReminderWork;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class NextReminderListener extends BroadcastReceiver {
    private final TextView nextReminder;
    private final MedicineViewModel medicineViewModel;

    private final HandlerThread thread;
    private final NextReminderIsTodayCallback nextReminderIsTodayCallback;
    private Reminder reminder;
    private Medicine medicine;

    public NextReminderListener(TextView nextReminder, NextReminderIsTodayCallback nextReminderIsTodayCallback, MedicineViewModel medicineViewModel) {
        this.nextReminder = nextReminder;
        this.nextReminderIsTodayCallback = nextReminderIsTodayCallback;
        this.medicineViewModel = medicineViewModel;
        this.thread = new HandlerThread("UpdateNextReminder");
        this.thread.start();
    }

    public static void sendNextReminder(@NonNull Context context, int reminderId, Instant timestamp) {
        Intent nextReminderIntent = new Intent(NEXT_REMINDER_ACTION);
        nextReminderIntent.putExtra(EXTRA_REMINDER_ID, reminderId);
        nextReminderIntent.putExtra(EXTRA_REMINDER_TIME, timestamp);
        context.sendBroadcast(nextReminderIntent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("ReminderListener", "Next reminder received");
        final Handler handler = new Handler(thread.getLooper());
        handler.post(() -> receiveNextReminderIntent(context, intent));
    }

    private void receiveNextReminderIntent(Context context, Intent intent) {
        int reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, 0);
        Instant timestamp = intent.getSerializableExtra(EXTRA_REMINDER_TIME, Instant.class);
        if (reminderId > 0 && timestamp != null) {
            reminder = medicineViewModel.getReminder(reminderId);
            if (reminder != null) {
                medicine = medicineViewModel.getMedicine(reminder.medicineRelId);
                ZonedDateTime reminderTime = timestamp.atZone(ZoneId.systemDefault());
                reportIfNextReminderIsToday(reminderTime);
                String nextTime = reminderTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT));
                final Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> setNextReminderText(context, reminder, medicine, nextTime));
            }
        }
    }

    private void reportIfNextReminderIsToday(ZonedDateTime reminderTime) {
        nextReminderIsTodayCallback.nextReminderIsToday(reminderTime.getDayOfMonth() == ZonedDateTime.now().getDayOfMonth() &&
                reminderTime.getMonth() == ZonedDateTime.now().getMonth());
    }

    private void setNextReminderText(Context context, Reminder reminder, Medicine medicine, String nextTime) {
        if (reminder != null && medicine != null) {
            nextReminder.setText(context.getString(R.string.reminder_event, reminder.amount, medicine.name, nextTime));
            nextReminder.setCompoundDrawables(null, null, null, null);
        }
    }

    public void processFutureReminder(boolean taken) {
        Handler handler = new Handler(thread.getLooper());
        handler.post(() -> {
            ReminderEvent reminderEvent = ReminderWork.buildReminderEvent(medicine, reminder);
            if (reminderEvent != null) {
                reminderEvent.status = taken ? ReminderEvent.ReminderStatus.TAKEN : ReminderEvent.ReminderStatus.SKIPPED;
                medicineViewModel.medicineRepository.insertReminderEvent(reminderEvent);
                ReminderProcessor.requestReschedule(nextReminder.getContext());
            }
        });

    }

    public void stop() {
        thread.quitSafely();
    }

    public interface NextReminderIsTodayCallback {
        void nextReminderIsToday(boolean isToday);
    }
}
