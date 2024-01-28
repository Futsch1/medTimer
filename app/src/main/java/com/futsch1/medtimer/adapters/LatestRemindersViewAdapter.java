package com.futsch1.medtimer.adapters;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.futsch1.medtimer.MedicineViewModel;
import com.futsch1.medtimer.database.ReminderEvent;

public class LatestRemindersViewAdapter extends ListAdapter<ReminderEvent, LatestRemindersViewHolder> {

    private final MedicineViewModel viewModel;

    public LatestRemindersViewAdapter(@NonNull DiffUtil.ItemCallback<ReminderEvent> diffCallback, MedicineViewModel medicineViewModel) {
        super(diffCallback);
        viewModel = medicineViewModel;
    }


    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public LatestRemindersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return LatestRemindersViewHolder.create(parent);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull LatestRemindersViewHolder holder, final int position) {
        ReminderEvent current = getItem(position);
        holder.bind(current, viewModel);
    }

    public static class ReminderEventDiff extends DiffUtil.ItemCallback<ReminderEvent> {

        @Override
        public boolean areItemsTheSame(@NonNull ReminderEvent oldItem, @NonNull ReminderEvent newItem) {
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ReminderEvent oldItem, @NonNull ReminderEvent newItem) {
            return oldItem.reminderEventId == newItem.reminderEventId && oldItem.status.equals(newItem.status);
        }
    }
}
