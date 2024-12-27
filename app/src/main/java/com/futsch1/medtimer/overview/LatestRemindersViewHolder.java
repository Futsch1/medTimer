package com.futsch1.medtimer.overview;

import android.app.Application;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.helpers.DeleteHelper;
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
    private final HandlerThread thread;
    private boolean checkedChanged = false;

    private LatestRemindersViewHolder(View itemView) {
        super(itemView);
        reminderEventText = itemView.findViewById(R.id.reminderEventText);
        chipTaken = itemView.findViewById(R.id.chipTaken);
        chipSkipped = itemView.findViewById(R.id.chipSkipped);
        chipGroup = itemView.findViewById(R.id.takenOrSkipped);
        thread = new HandlerThread("DeleteReminderEvent");
        thread.start();
    }

    static LatestRemindersViewHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_latest_reminder, parent, false);
        return new LatestRemindersViewHolder(view);
    }

    public void bind(ReminderEvent reminderEvent) {
        String takenDateTime = TimeHelper.toLocalizedDatetimeString(reminderEventText.getContext(),
                reminderEvent.remindedTimestamp);
        final int amountStringId = reminderEvent.amount.isBlank() ? R.string.reminder_event_blank : R.string.reminder_event;
        String reminderEventTextString = reminderEventText.getContext().getString(amountStringId, reminderEvent.amount, reminderEvent.medicineName, takenDateTime);
        reminderEventText.setText(reminderEventTextString);

        setupChips(reminderEvent);
        setupColorAndIcon(reminderEvent);

        checkedChanged = false;
    }

    private void setupChips(ReminderEvent reminderEvent) {
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
                checkedChanged = true;
            }
        });
        chipTaken.setOnClickListener(v -> processDeleteReRaiseReminderEvent(reminderEvent, chipTaken.isChecked()));
        chipSkipped.setOnClickListener(v -> processDeleteReRaiseReminderEvent(reminderEvent, chipSkipped.isChecked()));
    }

    private void setupColorAndIcon(ReminderEvent reminderEvent) {
        if (reminderEvent.useColor) {
            ViewColorHelper.setCardBackground((MaterialCardView) itemView, Collections.singletonList(reminderEventText), reminderEvent.color);
        } else {
            ViewColorHelper.setDefaultColors((MaterialCardView) itemView, Collections.singletonList(reminderEventText));
        }

        ViewColorHelper.setIconToImageView((MaterialCardView) itemView, itemView.findViewById(R.id.latestReminderIcon), reminderEvent.iconId);
    }

    private void processTakenOrSkipped(ReminderEvent reminderEvent, boolean taken) {
        ReminderProcessor.requestActionIntent(itemView.getContext(), reminderEvent.reminderEventId, taken);
    }

    private void processDeleteReRaiseReminderEvent(ReminderEvent reminderEvent, boolean checked) {
        if (checked && !checkedChanged) {
            new DeleteHelper(itemView.getContext()).deleteItem(R.string.delete_re_raise_event, () -> {
                Handler handler = new Handler(thread.getLooper());
                handler.post(() -> {
                    MedicineRepository medicineRepository = new MedicineRepository((Application) itemView.getContext().getApplicationContext());
                    medicineRepository.deleteReminderEvent(reminderEvent);
                    ReminderProcessor.requestReschedule(itemView.getContext());
                });
            }, () -> {
                // Intentionally empty
            });
        }
        checkedChanged = false;
    }
}
