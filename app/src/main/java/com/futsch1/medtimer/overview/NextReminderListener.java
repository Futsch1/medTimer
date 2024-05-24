package com.futsch1.medtimer.overview;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.widget.TextView;

import com.futsch1.medtimer.MedicineViewModel;
import com.futsch1.medtimer.NextRemindersViewModel;
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
import java.util.List;

public class NextReminderListener {
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

    public void processFutureReminder(boolean taken) {
        Handler handler = new Handler(thread.getLooper());
        handler.post(() -> {
            ReminderEvent reminderEvent = ReminderWork.buildReminderEvent(medicine, reminder);
            if (reminderEvent != null) {
                reminderEvent.status = taken ? ReminderEvent.ReminderStatus.TAKEN : ReminderEvent.ReminderStatus.SKIPPED;
                reminderEvent.processedTimestamp = Instant.now().getEpochSecond();
                medicineViewModel.medicineRepository.insertReminderEvent(reminderEvent);
                ReminderProcessor.requestReschedule(nextReminder.getContext());
            }
        });

    }
    
    public void setScheduledReminders(List<NextRemindersViewModel.ScheduledReminder> nextReminders) {
        if (!nextReminders.isEmpty()) {
            NextRemindersViewModel.ScheduledReminder scheduledReminder = nextReminders.get(0);
            reminder = scheduledReminder.reminder();
            medicine = scheduledReminder.medicine();
            Instant timestamp = scheduledReminder.timestamp();
            ZonedDateTime reminderTime = timestamp.atZone(ZoneId.systemDefault());
            reportIfNextReminderIsToday(reminderTime);
            String nextTime = reminderTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT));
            final Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> setNextReminderText(nextReminder.getContext(), reminder, medicine, nextTime));
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

    public interface NextReminderIsTodayCallback {
        void nextReminderIsToday(boolean isToday);
    }
}
