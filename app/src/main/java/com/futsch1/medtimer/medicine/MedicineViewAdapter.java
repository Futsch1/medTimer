package com.futsch1.medtimer.medicine;


import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.futsch1.medtimer.database.MedicineWithReminders;

public class MedicineViewAdapter extends ListAdapter<MedicineWithReminders, MedicineViewHolder> {

    public MedicineViewAdapter(@NonNull DiffUtil.ItemCallback<MedicineWithReminders> diffCallback) {
        super(diffCallback);
        setHasStableIds(true);
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
        holder.bind(current);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).medicine.medicineId;
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

