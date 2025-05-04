package com.futsch1.medtimer.medicine;


import android.os.Handler;
import android.os.HandlerThread;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DiffUtil;

import com.futsch1.medtimer.database.FullMedicine;
import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.helpers.IdlingListAdapter;
import com.futsch1.medtimer.helpers.SwipeHelper;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MedicineViewAdapter extends IdlingListAdapter<FullMedicine, MedicineViewHolder> implements SwipeHelper.MovedCallback {

    private final HandlerThread thread;
    private final FragmentActivity activity;
    private final MedicineRepository medicineRepository;

    public MedicineViewAdapter(HandlerThread thread, FragmentActivity activity, @NotNull MedicineRepository medicineRepository) {
        super(new MedicineDiff());
        setHasStableIds(true);
        this.thread = thread;
        this.activity = activity;
        this.medicineRepository = medicineRepository;
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

    @Override
    public void onMoved(int fromPosition, int toPosition) {
        List<FullMedicine> list = new ArrayList<>(getCurrentList());
        if (toPosition != -1) {
            Collections.swap(list, fromPosition, toPosition);
            submitList(list);
        }
    }

    @Override
    public void onMoveCompleted(int fromPosition, int toPosition) {
        if (fromPosition != toPosition) {
            new Handler(thread.getLooper()).post(() -> {
                        activity.runOnUiThread(() -> notifyItemMoved(fromPosition, toPosition));
                        medicineRepository.moveMedicine(fromPosition, toPosition);
                    }
            );
        }
    }

    public static class MedicineDiff extends DiffUtil.ItemCallback<FullMedicine> {

        @Override
        public boolean areItemsTheSame(@NonNull FullMedicine oldItem, @NonNull FullMedicine newItem) {
            return oldItem.medicine.medicineId == newItem.medicine.medicineId;
        }

        @Override
        public boolean areContentsTheSame(@NonNull FullMedicine oldItem, @NonNull FullMedicine newItem) {
            return oldItem.medicine.medicineId == newItem.medicine.medicineId &&
                    oldItem.medicine.name.equals(newItem.medicine.name) &&
                    oldItem.medicine.color == newItem.medicine.color &&
                    oldItem.medicine.iconId == newItem.medicine.iconId;
        }
    }

}

