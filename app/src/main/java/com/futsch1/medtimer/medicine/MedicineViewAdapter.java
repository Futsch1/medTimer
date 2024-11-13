package com.futsch1.medtimer.medicine;


import android.app.Activity;
import android.os.HandlerThread;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.futsch1.medtimer.database.MedicineWithReminders;

public class MedicineViewAdapter extends ListAdapter<MedicineWithReminders, MedicineViewHolder> {

    private final HandlerThread thread;
    private final Activity activity;

    public MedicineViewAdapter(@NonNull DiffUtil.ItemCallback<MedicineWithReminders> diffCallback, HandlerThread thread, Activity activity) {
        super(diffCallback);
        setHasStableIds(true);
        this.thread = thread;
        this.activity = activity;
    }


    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public MedicineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return MedicineViewHolder.create(parent, thread, activity);
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

