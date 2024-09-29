package com.futsch1.medtimer.medicine;

import static com.futsch1.medtimer.helpers.TimeHelper.minutesToTimeString;
import static com.futsch1.medtimer.helpers.TimeHelper.timeStringToMinutes;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.helpers.SummaryHelperKt;
import com.futsch1.medtimer.helpers.TimeHelper;
import com.google.android.material.button.MaterialButton;

public class ReminderViewHolder extends RecyclerView.ViewHolder {
    private final EditText editTime;
    private final EditText editAmount;
    private final MaterialButton advancedSettings;
    private final FragmentActivity fragmentActivity;
    private final TextView advancedSettingsSummary;

    private Reminder reminder;

    private ReminderViewHolder(View itemView, FragmentActivity fragmentActivity) {
        super(itemView);
        editTime = itemView.findViewById(R.id.editReminderTime);
        editAmount = itemView.findViewById(R.id.editAmount);
        advancedSettings = itemView.findViewById(R.id.open_advanced_settings);
        advancedSettingsSummary = itemView.findViewById(R.id.advancedSettingsSummary);

        this.fragmentActivity = fragmentActivity;
    }

    static ReminderViewHolder create(ViewGroup parent, FragmentActivity fragmentActivity) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_reminder, parent, false);
        return new ReminderViewHolder(view, fragmentActivity);
    }

    @SuppressLint("SetTextI18n")
    public void bind(Reminder reminder, String medicineName) {
        this.reminder = reminder;

        editTime.setText(minutesToTimeString(editTime.getContext(), reminder.timeInMinutes));
        editTime.setOnFocusChangeListener((v, hasFocus) -> onFocusEditTime(reminder, hasFocus));

        advancedSettings.setOnClickListener(v -> onClickAdvancedSettings(reminder, medicineName));

        editAmount.setText(reminder.amount);
        advancedSettingsSummary.setText(SummaryHelperKt.reminderSummary(itemView.getContext(), reminder));
    }

    private void onFocusEditTime(Reminder reminder, boolean hasFocus) {
        if (hasFocus) {
            int startMinutes = timeStringToMinutes(editTime.getContext(), editTime.getText().toString());
            if (startMinutes < 0) {
                startMinutes = Reminder.DEFAULT_TIME;
            }
            new TimeHelper.TimePickerWrapper(fragmentActivity).show(startMinutes / 60, startMinutes % 60, minutes -> {
                String selectedTime = minutesToTimeString(editTime.getContext(), minutes);
                editTime.setText(selectedTime);
                reminder.timeInMinutes = minutes;
            });
        }
    }

    private void onClickAdvancedSettings(Reminder reminder, String medicineName) {
        NavController navController = Navigation.findNavController(itemView);
        EditMedicineFragmentDirections.ActionEditMedicineToAdvancedReminderSettings action =
                EditMedicineFragmentDirections.actionEditMedicineToAdvancedReminderSettings(
                        reminder.reminderId,
                        medicineName
                );
        navController.navigate(action);
    }

    public Reminder getReminder() {
        reminder.amount = editAmount.getText().toString();
        int minutes = timeStringToMinutes(editTime.getContext(), editTime.getText().toString());
        if (minutes >= 0) {
            reminder.timeInMinutes = minutes;
        }
        return reminder;
    }
}
