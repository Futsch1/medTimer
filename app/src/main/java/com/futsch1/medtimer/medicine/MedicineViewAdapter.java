package com.futsch1.medtimer.medicine;


import android.os.HandlerThread;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.DiffUtil;

import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.helpers.IdlingListAdapter;

public class MedicineViewAdapter extends IdlingListAdapter<MedicineWithReminders, MedicineViewHolder> {

    private final HandlerThread thread;
    private final FragmentActivity activity;
    private final LifecycleOwner lifecycleOwner;

    public MedicineViewAdapter(HandlerThread thread, FragmentActivity activity, LifecycleOwner lifecycleOwner) {
        super(new MedicineDiff());
        setHasStableIds(true);
        this.thread = thread;
        this.activity = activity;
        this.lifecycleOwner = lifecycleOwner;
    }


    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public MedicineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return MedicineViewHolder.create(parent, activity, thread);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull MedicineViewHolder holder, final int position) {
        MedicineWithReminders current = getItem(position);
        holder.bind(current, lifecycleOwner);
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

