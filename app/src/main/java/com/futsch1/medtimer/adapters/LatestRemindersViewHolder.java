package com.futsch1.medtimer.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.MedicineViewModel;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.ReminderEvent;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

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

    public void bind(ReminderEvent reminderEvent, MedicineViewModel viewModel) {
        String takenDateTime = Instant.ofEpochSecond(reminderEvent.raisedTimestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT));
        reminderEventText.setText(reminderEventText.getContext().getString(R.string.reminder_event, reminderEvent.amount, reminderEvent.medicineName, takenDateTime));

        Log.d("ReminderEvent", "Binding reminder " + reminderEvent.reminderEventId);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
        });
        chipTaken.setChecked(reminderEvent.status == ReminderEvent.ReminderStatus.TAKEN);
        chipSkipped.setChecked(reminderEvent.status == ReminderEvent.ReminderStatus.SKIPPED);
        chipGroup.setSelectionRequired(true);

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chipTaken) {
                    reminderEvent.status = ReminderEvent.ReminderStatus.TAKEN;
                }
                if (checkedId == R.id.chipSkipped) {
                    reminderEvent.status = ReminderEvent.ReminderStatus.SKIPPED;
                }
                Log.d("ReminderEvent", "Updating reminder " + reminderEvent.reminderEventId);
                viewModel.updateReminderEvent(reminderEvent);
            }
        });
    }
}
