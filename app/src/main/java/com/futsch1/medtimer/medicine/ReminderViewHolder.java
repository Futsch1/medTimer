package com.futsch1.medtimer.medicine;

import static com.futsch1.medtimer.helpers.TimeHelper.durationStringToMinutes;
import static com.futsch1.medtimer.helpers.TimeHelper.minutesToDurationString;
import static com.futsch1.medtimer.helpers.TimeHelper.minutesToTimeString;
import static com.futsch1.medtimer.helpers.TimeHelper.timeStringToMinutes;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.helpers.SummaryHelperKt;
import com.futsch1.medtimer.helpers.TimeHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.TimeFormat;

public class ReminderViewHolder extends RecyclerView.ViewHolder {
    private final EditText editTime;
    private final EditText editAmount;
    private final MaterialButton advancedSettings;
    private final FragmentActivity fragmentActivity;
    private final TextView advancedSettingsSummary;
    private final HandlerThread thread;
    private final TextInputLayout editTimeLayout;

    private Reminder reminder;

    private ReminderViewHolder(View itemView, FragmentActivity fragmentActivity, HandlerThread thread) {
        super(itemView);
        editTime = itemView.findViewById(R.id.editReminderTime);
        editTimeLayout = itemView.findViewById(R.id.editReminderTimeLayout);
        editAmount = itemView.findViewById(R.id.editAmount);
        advancedSettings = itemView.findViewById(R.id.open_advanced_settings);
        advancedSettingsSummary = itemView.findViewById(R.id.advancedSettingsSummary);
        this.thread = thread;

        this.fragmentActivity = fragmentActivity;
    }

    static ReminderViewHolder create(ViewGroup parent, FragmentActivity fragmentActivity, HandlerThread thread) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_reminder, parent, false);
        return new ReminderViewHolder(view, fragmentActivity, thread);
    }

    @SuppressLint("SetTextI18n")
    public void bind(Reminder reminder) {
        this.reminder = reminder;

        @StringRes int textId = reminder.linkedReminderId == 0 ? R.string.time : R.string.delay;
        editTimeLayout.setHint(textId);
        editTime.setText(reminder.linkedReminderId != 0 ? minutesToDurationString(reminder.timeInMinutes) : minutesToTimeString(editTime.getContext(), reminder.timeInMinutes));
        editTime.setOnFocusChangeListener((v, hasFocus) -> onFocusEditTime(reminder, hasFocus));

        advancedSettings.setOnClickListener(v -> onClickAdvancedSettings(reminder));

        editAmount.setText(reminder.amount);
        new Handler(thread.getLooper()).post(() ->
                advancedSettingsSummary.setText(SummaryHelperKt.reminderSummary(itemView.getContext(), reminder)));
    }

    private void onFocusEditTime(Reminder reminder, boolean hasFocus) {
        if (hasFocus) {
            if (reminder.linkedReminderId == 0) {
                editDuration(reminder);
            } else {
                editTime(reminder);
            }
        }
    }

    private void onClickAdvancedSettings(Reminder reminder) {
        NavController navController = Navigation.findNavController(itemView);
        EditMedicineFragmentDirections.ActionEditMedicineToAdvancedReminderSettings action =
                EditMedicineFragmentDirections.actionEditMedicineToAdvancedReminderSettings(
                        reminder.reminderId
                );
        navController.navigate(action);
    }

    private void editDuration(Reminder reminder) {
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

    private void editTime(Reminder reminder) {
        int startMinutes = durationStringToMinutes(editTime.getText().toString());
        if (startMinutes < 0) {
            startMinutes = Reminder.DEFAULT_TIME;
        }
        new TimeHelper.TimePickerWrapper(fragmentActivity, R.string.linked_reminder_delay, TimeFormat.CLOCK_24H)
                .show(startMinutes / 60, startMinutes % 60, minutes -> {
                    String selectedTime = minutesToDurationString(minutes);
                    editTime.setText(selectedTime);
                    reminder.timeInMinutes = minutes;
                });
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
