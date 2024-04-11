package com.futsch1.medtimer.medicine;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_MEDICINE_NAME;
import static com.futsch1.medtimer.helpers.TimeHelper.minutesToTime;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupMenu;

import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.helpers.TimeHelper;
import com.google.android.material.button.MaterialButton;

public class ReminderViewHolder extends RecyclerView.ViewHolder {
    private final EditText editTime;
    private final EditText editAmount;
    private final View holderItemView;
    private final MaterialButton advancedSettings;

    private Reminder reminder;


    private ReminderViewHolder(View itemView) {
        super(itemView);
        editTime = itemView.findViewById(R.id.editReminderTime);
        editAmount = itemView.findViewById(R.id.editAmount);
        advancedSettings = itemView.findViewById(R.id.open_advanced_settings);
        this.holderItemView = itemView;
    }

    static ReminderViewHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_reminder, parent, false);
        return new ReminderViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    public void bind(Reminder reminder, String medicineName, DeleteCallback deleteCallback) {
        this.reminder = reminder;

        editTime.setText(minutesToTime(reminder.timeInMinutes));

        editTime.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                new TimeHelper.TimePickerWrapper(editTime.getContext()).show(reminder.timeInMinutes / 60, reminder.timeInMinutes % 60, minutes -> {
                    String selectedTime = minutesToTime(minutes);
                    editTime.setText(selectedTime);
                    reminder.timeInMinutes = minutes;
                });
            }
        });

        advancedSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this.holderItemView.getContext(), AdvancedReminderSettings.class);
            intent.putExtra(EXTRA_ID, reminder.reminderId);
            intent.putExtra(EXTRA_MEDICINE_NAME, medicineName);
            this.holderItemView.getContext().startActivity(intent);
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
        return reminder;
    }

    public interface DeleteCallback {
        void deleteItem(Context context, long itemId, int adapterPosition);
    }
}
