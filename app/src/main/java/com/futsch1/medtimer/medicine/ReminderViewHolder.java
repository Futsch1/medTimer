package com.futsch1.medtimer.medicine;

import static com.futsch1.medtimer.helpers.TimeHelper.minutesToTime;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
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
    private final EditText editDaysBetweenReminders;
    private final View holderItemView;

    private Reminder reminder;


    private ReminderViewHolder(View itemView) {
        super(itemView);
        editTime = itemView.findViewById(R.id.editReminderTime);
        editAmount = itemView.findViewById(R.id.editAmount);
        editDaysBetweenReminders = itemView.findViewById(R.id.daysBetweenReminders);
        this.holderItemView = itemView;
    }

    static ReminderViewHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_reminder, parent, false);
        return new ReminderViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    public void bind(Reminder reminder, DeleteCallback deleteCallback) {
        this.reminder = reminder;

        editTime.setText(minutesToTime(reminder.timeInMinutes));
        editDaysBetweenReminders.setText(Integer.toString(reminder.daysBetweenReminders));

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

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.holderItemView.getContext());

        holderItemView.setOnLongClickListener(v -> {
            if (sharedPref.getString("delete_items", "0").equals("0")) {
                return false;
            }

            PopupMenu popupMenu = new PopupMenu(editTime.getContext(), this.holderItemView);
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
    }

    public Reminder getReminder() {
        reminder.amount = editAmount.getText().toString();
        try {
            reminder.daysBetweenReminders = Integer.parseInt(editDaysBetweenReminders.getText().toString());
            if (reminder.daysBetweenReminders <= 0) {
                reminder.daysBetweenReminders = 1;
            }
        } catch (NumberFormatException e) {
            reminder.daysBetweenReminders = 1;
        }
        return reminder;
    }

    public interface DeleteCallback {
        void deleteItem(Context context, long itemId, int adapterPosition);
    }
}
