package com.futsch1.medtimer.medicine;


import android.os.HandlerThread;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DiffUtil;

import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.helpers.IdlingListAdapter;

public class ReminderViewAdapter extends IdlingListAdapter<Reminder, ReminderViewHolder> {

    private final FragmentActivity fragmentActivity;
    private final HandlerThread thread;
    private Medicine medicine;

    public ReminderViewAdapter(FragmentActivity fragmentActivity, HandlerThread thread) {
        super(new ReminderDiff());
        this.fragmentActivity = fragmentActivity;
        this.thread = thread;
        setHasStableIds(true);
    }

    public void setMedicine(Medicine medicine) {
        this.medicine = medicine;
    }


    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return ReminderViewHolder.create(parent, fragmentActivity, thread);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, final int position) {
        Reminder current = getItem(position);
        holder.bind(current, medicine);
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
                    && oldItem.timeInMinutes == newItem.timeInMinutes;
        }
    }
}

