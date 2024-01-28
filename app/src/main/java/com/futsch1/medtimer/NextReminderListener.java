package com.futsch1.medtimer;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_TIME;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.Reminder;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class NextReminderListener extends BroadcastReceiver {
    private final TextView nextReminder;
    private final MedicineViewModel medicineViewModel;
    private final HandlerThread thread;

    public NextReminderListener(TextView nextReminder, MedicineViewModel medicineViewModel) {
        this.nextReminder = nextReminder;
        this.medicineViewModel = medicineViewModel;
        this.thread = new HandlerThread("UpdateNextReminder");
        this.thread.start();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("ReminderListener", "Next reminder received");
        final Handler handler = new Handler(thread.getLooper());
        handler.post(() -> {
            int reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, 0);
            Instant timestamp = intent.getSerializableExtra(EXTRA_REMINDER_TIME, Instant.class);
            if (reminderId > 0 && timestamp != null) {
                Reminder reminder = medicineViewModel.getReminder(reminderId);
                Medicine medicine = medicineViewModel.getMedicine(reminder.medicineRelId);
                String nextTime = timestamp.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT));
                final Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() ->
                        nextReminder.setText(context.getString(R.string.reminder_event, reminder.amount, medicine.name, nextTime)));
            }
        });
    }

    public void stop() {
        thread.quitSafely();
    }
}
