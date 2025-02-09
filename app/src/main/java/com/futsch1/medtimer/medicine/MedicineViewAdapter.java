package com.futsch1.medtimer.medicine;


import android.os.HandlerThread;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DiffUtil;

import com.futsch1.medtimer.database.FullMedicine;
import com.futsch1.medtimer.helpers.IdlingListAdapter;

public class MedicineViewAdapter extends IdlingListAdapter<FullMedicine, MedicineViewHolder> {

    private final HandlerThread thread;
    private final FragmentActivity activity;

    public MedicineViewAdapter(HandlerThread thread, FragmentActivity activity) {
        super(new MedicineDiff());
        setHasStableIds(true);
        this.thread = thread;
        this.activity = activity;
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
        FullMedicine current = getItem(position);
        holder.bind(current);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).medicine.medicineId;
    }

    public static class MedicineDiff extends DiffUtil.ItemCallback<FullMedicine> {

        @Override
        public boolean areItemsTheSame(@NonNull FullMedicine oldItem, @NonNull FullMedicine newItem) {
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull FullMedicine oldItem, @NonNull FullMedicine newItem) {
            return oldItem.medicine.medicineId == newItem.medicine.medicineId;
        }
    }
}

