package com.futsch1.medtimer.medicine;


import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.futsch1.medtimer.database.Reminder;

public class ReminderViewAdapter extends ListAdapter<Reminder, ReminderViewHolder> {

    private final ReminderViewHolder.DeleteCallback deleteCallback;
    private final String medicineName;

    public ReminderViewAdapter(@NonNull DiffUtil.ItemCallback<Reminder> diffCallback, ReminderViewHolder.DeleteCallback deleteCallback, String medicineName) {
        super(diffCallback);
        this.deleteCallback = deleteCallback;
        this.medicineName = medicineName;
        setHasStableIds(true);
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
        holder.bind(current, medicineName, deleteCallback);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).reminderId;
    }

    public static class ReminderDiff extends DiffUtil.ItemCallback<Reminder> {

        @Override
        public boolean areItemsTheSame(@NonNull Reminder oldItem, @NonNull Reminder newItem) {
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Reminder oldItem, @NonNull Reminder newItem) {
            return oldItem.reminderId == newItem.reminderId && oldItem.amount.equals(newItem.amount)
                    && oldItem.timeInMinutes == newItem.timeInMinutes && oldItem.daysBetweenReminders == newItem.daysBetweenReminders;
        }
    }
}

