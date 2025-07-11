package com.futsch1.medtimer.overview;

import android.app.Application;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.helpers.DeleteHelper;
import com.futsch1.medtimer.helpers.ReminderHelperKt;
import com.futsch1.medtimer.helpers.ViewColorHelper;
import com.futsch1.medtimer.reminders.ReminderProcessor;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.Collections;

public class LatestRemindersViewHolder extends RecyclerView.ViewHolder {
    private final TextView reminderEventText;
    private final Chip chipTaken;
    private final Chip chipSkipped;
    private final ChipGroup chipGroup;
    private final SharedPreferences sharedPreferences;
    private boolean checkedChanged = false;

    private LatestRemindersViewHolder(View itemView) {
        super(itemView);
        reminderEventText = itemView.findViewById(R.id.reminderEventText);
        chipTaken = itemView.findViewById(R.id.chipTaken);
        chipSkipped = itemView.findViewById(R.id.chipSkipped);
        chipGroup = itemView.findViewById(R.id.takenOrSkipped);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(itemView.getContext());
    }

    static LatestRemindersViewHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_latest_reminder, parent, false);
        return new LatestRemindersViewHolder(view);
    }

    public void bind(ReminderEvent reminderEvent) {
        reminderEventText.setText(ReminderHelperKt.formatReminderString(reminderEventText.getContext(), reminderEvent, sharedPreferences));

        setupChips(reminderEvent);
        setupColorAndIcon(reminderEvent);
        setupEditEvent(reminderEvent);

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
            ViewColorHelper.setViewBackground(itemView, Collections.singletonList(reminderEventText), reminderEvent.color);
        } else {
            ViewColorHelper.setDefaultColors(itemView, Collections.singletonList(reminderEventText));
        }

        ViewColorHelper.setIconToImageView(itemView, itemView.findViewById(R.id.latestReminderIcon), reminderEvent.iconId);
    }

    private void setupEditEvent(ReminderEvent reminderEvent) {
        this.itemView.setOnClickListener((View v) -> {
            NavController navController = Navigation.findNavController(this.itemView);
            OverviewFragmentDirections.ActionOverviewFragmentToEditEventFragment action = OverviewFragmentDirections.actionOverviewFragmentToEditEventFragment(
                    reminderEvent.reminderEventId
            );
            try {
                navController.navigate(action);
            } catch (IllegalArgumentException | IllegalStateException e) {
                // Intentionally empty
            }
        });

    }

    private void processTakenOrSkipped(ReminderEvent reminderEvent, boolean taken) {
        ReminderProcessor.requestActionIntent(itemView.getContext(), reminderEvent.reminderEventId, taken);
    }

    private void processDeleteReRaiseReminderEvent(ReminderEvent reminderEvent, boolean checked) {
        if (checked && !checkedChanged) {
            new DeleteHelper(itemView.getContext()).deleteItem(R.string.delete_re_raise_event, () -> {
                MedicineRepository medicineRepository = new MedicineRepository((Application) itemView.getContext().getApplicationContext());
                medicineRepository.deleteReminderEvent(reminderEvent.reminderEventId);
                ReminderProcessor.requestReschedule(itemView.getContext());
            }, () -> {
                // Intentionally empty
            });
        }
        checkedChanged = false;
    }
}
