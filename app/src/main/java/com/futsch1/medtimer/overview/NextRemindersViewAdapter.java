package com.futsch1.medtimer.overview;

import android.os.HandlerThread;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.futsch1.medtimer.MedicineViewModel;
import com.futsch1.medtimer.ScheduledReminder;

public class NextRemindersViewAdapter extends ListAdapter<ScheduledReminder, NextRemindersViewHolder> {

    private final HandlerThread thread;
    private final MedicineViewModel medicineViewModel;

    public NextRemindersViewAdapter(@NonNull DiffUtil.ItemCallback<ScheduledReminder> diffCallback, MedicineViewModel medicineViewModel) {
        super(diffCallback);
        setHasStableIds(true);
        this.medicineViewModel = medicineViewModel;
        this.thread = new HandlerThread("UpdateNextReminder");
        this.thread.start();
    }

    @NonNull
    @Override
    public NextRemindersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return NextRemindersViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull NextRemindersViewHolder holder, final int position) {
        ScheduledReminder current = getItem(position);
        holder.bind(current, thread.getLooper(), medicineViewModel);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).reminder().reminderId;
    }

    public static class ScheduledReminderDiff extends DiffUtil.ItemCallback<ScheduledReminder> {

        @Override
        public boolean areItemsTheSame(@NonNull ScheduledReminder oldItem, @NonNull ScheduledReminder newItem) {
            return oldItem.reminder().reminderId == newItem.reminder().reminderId && oldItem.timestamp().equals(newItem.timestamp());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ScheduledReminder oldItem, @NonNull ScheduledReminder newItem) {
            return oldItem.reminder().reminderId == newItem.reminder().reminderId && oldItem.timestamp().equals(newItem.timestamp()) &&
                    oldItem.reminder().amount.equals(newItem.reminder().amount) && oldItem.medicine().iconId == newItem.medicine().iconId;
        }
    }
}
