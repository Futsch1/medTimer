package com.futsch1.medtimer.adapters;


import android.content.Intent;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.futsch1.medtimer.MedicineViewModel;
import com.futsch1.medtimer.database.MedicineWithReminders;

public class MedicineViewAdapter extends ListAdapter<MedicineWithReminders, MedicineViewHolder> {

    private final MedicineViewModel viewModel;
    private final ActivityResultLauncher<Intent> activityResultLauncher;

    public MedicineViewAdapter(@NonNull DiffUtil.ItemCallback<MedicineWithReminders> diffCallback, MedicineViewModel medicineViewModel, ActivityResultLauncher<Intent> medicineActivityResultLauncher) {
        super(diffCallback);
        viewModel = medicineViewModel;
        activityResultLauncher = medicineActivityResultLauncher;
    }


    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public MedicineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return MedicineViewHolder.create(parent);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull MedicineViewHolder holder, final int position) {
        MedicineWithReminders current = getItem(position);
        holder.bind(current.medicine, viewModel, activityResultLauncher);
    }

    public static class MedicineDiff extends DiffUtil.ItemCallback<MedicineWithReminders> {

        @Override
        public boolean areItemsTheSame(@NonNull MedicineWithReminders oldItem, @NonNull MedicineWithReminders newItem) {
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull MedicineWithReminders oldItem, @NonNull MedicineWithReminders newItem) {
            return oldItem.medicine.medicineId == newItem.medicine.medicineId;
        }
    }
}

