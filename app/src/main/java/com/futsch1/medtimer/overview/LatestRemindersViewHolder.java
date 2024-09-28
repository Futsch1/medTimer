package com.futsch1.medtimer.overview;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.helpers.TimeHelper;
import com.futsch1.medtimer.helpers.ViewColorHelper;
import com.futsch1.medtimer.reminders.ReminderProcessor;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.Collections;

public class LatestRemindersViewHolder extends RecyclerView.ViewHolder {
    private final TextView reminderEventText;
    private final Chip chipTaken;
    private final Chip chipSkipped;
    private final ChipGroup chipGroup;

    private LatestRemindersViewHolder(View itemView) {
        super(itemView);
        reminderEventText = itemView.findViewById(R.id.reminderEventText);
        chipTaken = itemView.findViewById(R.id.chipTaken);
        chipSkipped = itemView.findViewById(R.id.chipSkipped);
        chipGroup = itemView.findViewById(R.id.takenOrSkipped);
    }

    static LatestRemindersViewHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_latest_reminder, parent, false);
        return new LatestRemindersViewHolder(view);
    }

    public void bind(ReminderEvent reminderEvent) {
        String takenDateTime = TimeHelper.toLocalizedDatetimeString(reminderEventText.getContext(),
                reminderEvent.remindedTimestamp);
        final int amountStringId = reminderEvent.amount.isBlank() ? R.string.reminder_event_blank : R.string.reminder_event;
        String reminderEventTextString = reminderEventText.getContext().getString(amountStringId, reminderEvent.amount, reminderEvent.medicineName, takenDateTime);
        reminderEventText.setText(reminderEventTextString);

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            // Intentionally empty
        });
        chipGroup.setSelectionRequired(false);
        chipTaken.setChecked(reminderEvent.status == ReminderEvent.ReminderStatus.TAKEN);
        chipSkipped.setChecked(reminderEvent.status == ReminderEvent.ReminderStatus.SKIPPED);
        chipGroup.setSelectionRequired(true);

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                processTakenOrSkipped(reminderEvent, R.id.chipTaken == checkedId);
            }
        });

        if (reminderEvent.useColor) {
            ViewColorHelper.setCardBackground((MaterialCardView) itemView, Collections.singletonList(reminderEventText), reminderEvent.color);
        } else {
            ViewColorHelper.setDefaultColors((MaterialCardView) itemView, Collections.singletonList(reminderEventText));
        }

        ViewColorHelper.setIconToImageView((MaterialCardView) itemView, itemView.findViewById(R.id.latestReminderIcon), reminderEvent.iconId);
    }

    private void processTakenOrSkipped(ReminderEvent reminderEvent, boolean taken) {
        Intent i =
                taken ?
                        ReminderProcessor.getTakenActionIntent(itemView.getContext(), reminderEvent.reminderEventId) :
                        ReminderProcessor.getDismissedActionIntent(itemView.getContext(), reminderEvent.reminderEventId);
        itemView.getContext().sendBroadcast(i, "com.futsch1.medtimer.NOTIFICATION_PROCESSED");
    }
}
