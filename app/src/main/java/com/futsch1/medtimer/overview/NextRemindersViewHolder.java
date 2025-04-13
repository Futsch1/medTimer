package com.futsch1.medtimer.overview;

import static android.text.format.DateUtils.isToday;
import static com.futsch1.medtimer.helpers.ReminderHelperKt.formatScheduledReminderString;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.preference.PreferenceManager;
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

import java.time.ZoneId;
import java.util.Collections;

public class NextRemindersViewHolder extends RecyclerView.ViewHolder {
    private final TextView nextReminderText;
    private final SharedPreferences sharedPreferences;

    private NextRemindersViewHolder(View itemView) {
        super(itemView);
        nextReminderText = itemView.findViewById(R.id.nextReminderText);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(itemView.getContext());
    }

    static NextRemindersViewHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_next_reminder, parent, false);
        return new NextRemindersViewHolder(view);
    }

    public void bind(ScheduledReminder scheduledReminder, Looper looper, MedicineViewModel medicineViewModel) {
        Chip takenNow = itemView.findViewById(R.id.takenNow);
        Chip skippedNow = itemView.findViewById(R.id.skippedNow);
        takenNow.setOnClickListener(v -> processFutureReminder(scheduledReminder, true, looper, medicineViewModel));
        skippedNow.setOnClickListener(v -> processFutureReminder(scheduledReminder, false, looper, medicineViewModel));

        nextReminderText.setText(formatScheduledReminderString(nextReminderText.getContext(), scheduledReminder, sharedPreferences));

        boolean isToday = isToday(scheduledReminder.timestamp().toEpochMilli());
        takenNow.setVisibility(isToday ? View.VISIBLE : View.GONE);
        skippedNow.setVisibility(isToday ? View.VISIBLE : View.GONE);

        if (scheduledReminder.medicine().medicine.useColor) {
            ViewColorHelper.setCardBackground((MaterialCardView) itemView, Collections.singletonList(nextReminderText), scheduledReminder.medicine().medicine.color);
        } else {
            ViewColorHelper.setDefaultColors((MaterialCardView) itemView, Collections.singletonList(nextReminderText));
        }

        ViewColorHelper.setIconToImageView((MaterialCardView) itemView, itemView.findViewById(R.id.nextReminderIcon), scheduledReminder.medicine().medicine.iconId);
    }

    private void processFutureReminder(ScheduledReminder scheduledReminder, boolean taken, Looper looper, MedicineViewModel medicineViewModel) {
        Handler handler = new Handler(looper);
        handler.post(() -> {
            ReminderEvent reminderEvent = ReminderWork.buildReminderEvent(scheduledReminder.timestamp().atZone(ZoneId.systemDefault()).toLocalDateTime(),
                    scheduledReminder.medicine(), scheduledReminder.reminder());
            if (reminderEvent != null) {
                long reminderEventId = medicineViewModel.medicineRepository.insertReminderEvent(reminderEvent);
                itemView.getContext().sendBroadcast(taken ?
                                ReminderProcessor.getTakenActionIntent(itemView.getContext(), (int) reminderEventId)
                                : ReminderProcessor.getSkippedActionIntent(itemView.getContext(), (int) reminderEventId),
                        "com.futsch1.medtimer.NOTIFICATION_PROCESSED");
            }
        });
    }
}
