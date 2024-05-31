package com.futsch1.medtimer.overview;

import static android.text.format.DateUtils.isToday;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.MedicineViewModel;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.ScheduledReminder;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.helpers.ViewColorHelper;
import com.futsch1.medtimer.reminders.ReminderProcessor;
import com.futsch1.medtimer.reminders.ReminderWork;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collections;

public class NextRemindersViewHolder extends RecyclerView.ViewHolder {
    private final TextView nextReminderText;

    private NextRemindersViewHolder(View itemView) {
        super(itemView);
        nextReminderText = itemView.findViewById(R.id.nextReminderText);
    }

    static NextRemindersViewHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_next_reminder, parent, false);
        return new NextRemindersViewHolder(view);
    }

    public void bind(ScheduledReminder scheduledReminder, Looper looper, MedicineViewModel medicineViewModel) {
        Chip takenNow = itemView.findViewById(R.id.takenNow);
        Chip skippedNow = itemView.findViewById(R.id.skippedNow);
        takenNow.setOnClickListener(v -> processFutureReminder(scheduledReminder, true, looper, medicineViewModel));
        skippedNow.setOnClickListener(v -> processFutureReminder(scheduledReminder, false, looper, medicineViewModel));

        ZonedDateTime reminderTime = scheduledReminder.timestamp().atZone(ZoneId.systemDefault());
        String nextTime = reminderTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT));
        nextReminderText.setText(nextReminderText.getContext().getString(R.string.reminder_event, scheduledReminder.reminder().amount, scheduledReminder.medicine().name, nextTime));

        boolean isToday = isToday(scheduledReminder.timestamp().toEpochMilli());
        takenNow.setVisibility(isToday ? View.VISIBLE : View.GONE);
        skippedNow.setVisibility(isToday ? View.VISIBLE : View.GONE);

        if (scheduledReminder.medicine().useColor) {
            ViewColorHelper.setCardBackground((MaterialCardView) itemView, Collections.singletonList(nextReminderText), scheduledReminder.medicine().color);
        } else {
            ViewColorHelper.setDefaultColors((MaterialCardView) itemView, Collections.singletonList(nextReminderText));
        }
    }

    private void processFutureReminder(ScheduledReminder scheduledReminder, boolean taken, Looper looper, MedicineViewModel medicineViewModel) {
        Handler handler = new Handler(looper);
        handler.post(() -> {
            ReminderEvent reminderEvent = ReminderWork.buildReminderEvent(scheduledReminder.medicine(), scheduledReminder.reminder());
            if (reminderEvent != null) {
                reminderEvent.status = taken ? ReminderEvent.ReminderStatus.TAKEN : ReminderEvent.ReminderStatus.SKIPPED;
                reminderEvent.processedTimestamp = Instant.now().getEpochSecond();
                medicineViewModel.medicineRepository.insertReminderEvent(reminderEvent);
                ReminderProcessor.requestReschedule(nextReminderText.getContext());
            }
        });
    }
}
