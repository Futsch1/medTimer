package com.futsch1.medtimer.adapters;

import static com.futsch1.medtimer.TimeHelper.minutesToTime;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupMenu;

import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.Reminder;

public class ReminderViewHolder extends RecyclerView.ViewHolder {
    private final EditText editTime;
    private final EditText editAmount;
    private final View itemView;

    public Reminder reminder;


    private ReminderViewHolder(View itemView) {
        super(itemView);
        editTime = itemView.findViewById(R.id.editReminderTime);
        editAmount = itemView.findViewById(R.id.editAmount);
        this.itemView = itemView;
    }

    static ReminderViewHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_reminder, parent, false);
        return new ReminderViewHolder(view);
    }

    public void bind(Reminder reminder, DeleteCallback deleteCallback) {
        this.reminder = reminder;

        editTime.setText(minutesToTime(reminder.timeInMinutes));

        TimePickerDialog timePickerDialog = new TimePickerDialog(editTime.getContext(), (view, hourOfDay, minute) -> {
            String selectedTime = minutesToTime(hourOfDay * 60L + minute);
            editTime.setText(selectedTime);
            reminder.timeInMinutes = hourOfDay * 60 + minute;
        }, 0, 0, DateFormat.is24HourFormat(editTime.getContext()));
        editTime.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                timePickerDialog.updateTime(reminder.timeInMinutes / 60, reminder.timeInMinutes % 60);
                timePickerDialog.show();
            }
        });

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.itemView.getContext());

        itemView.setOnLongClickListener(v -> {
            if (sharedPref.getString("delete_items", "0").equals("0")) {
                return false;
            }
            
            PopupMenu popupMenu = new PopupMenu(editTime.getContext(), this.itemView);
            popupMenu.getMenuInflater().inflate(R.menu.edit_delete_popup, popupMenu.getMenu());
            popupMenu.getMenu().findItem(R.id.edit).setVisible(false);
            popupMenu.getMenu().findItem(R.id.delete).setOnMenuItemClickListener(item -> {
                deleteCallback.deleteItem(editTime.getContext(), getItemId(), getAdapterPosition());
                return true;
            });
            popupMenu.show();
            return true;
        });

        editAmount.setText(reminder.amount);
        editAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                reminder.amount = s.toString();
            }
        });
    }

    public interface DeleteCallback {
        void deleteItem(Context context, long itemId, int adapterPosition);
    }
}
