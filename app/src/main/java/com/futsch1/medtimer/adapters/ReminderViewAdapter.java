package com.futsch1.medtimer.adapters;


import android.content.Intent;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.futsch1.medtimer.MedicineViewModel;
import com.futsch1.medtimer.database.Reminder;

public class ReminderViewAdapter extends ListAdapter<Reminder, ReminderViewHolder> {

    private final MedicineViewModel viewModel;
    private final ActivityResultLauncher<Intent> activityResultLauncher;

    public ReminderViewAdapter(@NonNull DiffUtil.ItemCallback<Reminder> diffCallback, MedicineViewModel medicineViewModel, ActivityResultLauncher<Intent> medicineActivityResultLauncher) {
        super(diffCallback);
        viewModel = medicineViewModel;
        activityResultLauncher = medicineActivityResultLauncher;
    }


    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return ReminderViewHolder.create(parent);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, final int position) {
        Reminder current = getItem(position);
        holder.bind(current, viewModel, activityResultLauncher);
    }

    public static class ReminderDiff extends DiffUtil.ItemCallback<Reminder> {

        @Override
        public boolean areItemsTheSame(@NonNull Reminder oldItem, @NonNull Reminder newItem) {
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Reminder oldItem, @NonNull Reminder newItem) {
            return oldItem.reminderId == newItem.reminderId;
        }
    }
}

