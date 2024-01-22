package com.futsch1.medtimer.adapters;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.MedicineViewModel;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.Reminder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReminderViewHolder extends RecyclerView.ViewHolder {
    private final EditText editTime;
    private final EditText editAmount;
    private final ImageButton deleteButton;


    private ReminderViewHolder(View itemView) {
        super(itemView);
        editTime = itemView.findViewById(R.id.editReminderTime);
        editAmount = itemView.findViewById(R.id.editAmount);
        deleteButton = itemView.findViewById(R.id.deleteReminder);
    }

    static ReminderViewHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_reminder, parent, false);
        return new ReminderViewHolder(view);
    }

    public void bind(Reminder reminder, MedicineViewModel viewModel, ActivityResultLauncher<Intent> activityResultLauncher) {
        Date dt = new Date(reminder.timeInMinutes * 1000 * 60);
        String timeOfDay = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(dt);
        editTime.setText(timeOfDay);
        editAmount.setText(reminder.amount);

        deleteButton.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
            builder.setTitle(R.string.confirm);
            builder.setMessage(R.string.are_you_sure_delete_medicine);
            builder.setCancelable(false);
            builder.setPositiveButton(R.string.yes, (dialogInterface, i) -> viewModel.deleteReminder(reminder));
            builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
            });
            builder.show();
        });
    }
}
