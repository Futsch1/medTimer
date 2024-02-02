package com.futsch1.medtimer.adapters;

import static com.futsch1.medtimer.TimeHelper.minutesToTime;

import android.app.TimePickerDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.MedicineViewModel;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.Reminder;

public class ReminderViewHolder extends RecyclerView.ViewHolder {
    private final EditText editTime;
    private final EditText editAmount;

    public Reminder reminder;


    private ReminderViewHolder(View itemView) {
        super(itemView);
        editTime = itemView.findViewById(R.id.editReminderTime);
        editAmount = itemView.findViewById(R.id.editAmount);
    }

    static ReminderViewHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_reminder, parent, false);
        return new ReminderViewHolder(view);
    }

    public void bind(Reminder reminder, MedicineViewModel viewModel) {
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
}
