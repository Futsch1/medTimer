package com.futsch1.medtimer.medicine;

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
import com.futsch1.medtimer.medicine.editors.TimeEditor;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import kotlin.Unit;

public class ReminderViewHolder extends RecyclerView.ViewHolder {
    private final TextInputEditText editTime;
    private final EditText editAmount;
    private final MaterialButton advancedSettings;
    private final FragmentActivity fragmentActivity;
    private final TextView advancedSettingsSummary;
    private final HandlerThread thread;
    private final TextInputLayout editTimeLayout;

    private Reminder reminder;
    private TimeEditor timeEditor;

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

        setupTimeEditor();

        advancedSettings.setOnClickListener(v -> onClickAdvancedSettings(reminder));

        editAmount.setText(reminder.amount);

        new Handler(thread.getLooper()).post(() -> {
            String summary = SummaryHelperKt.reminderSummary(itemView.getContext(), reminder);
            this.fragmentActivity.runOnUiThread(() ->
                    advancedSettingsSummary.setText(summary));

        });
    }

    private void setupTimeEditor() {
        if (reminder.getReminderType() != Reminder.ReminderType.INTERVAL_BASED) {
            @StringRes int textId = reminder.getReminderType() == Reminder.ReminderType.TIME_BASED ? R.string.time : R.string.delay;
            editTimeLayout.setHint(textId);
            timeEditor = new TimeEditor(fragmentActivity, editTime, reminder.timeInMinutes, minutes -> {
                reminder.timeInMinutes = minutes;
                return Unit.INSTANCE;
            }, reminder.getReminderType() == Reminder.ReminderType.LINKED ? R.string.linked_reminder_delay : null);
        } else {
            editTimeLayout.setVisibility(View.GONE);
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

    public Reminder getReminder() {
        reminder.amount = editAmount.getText().toString();
        if (timeEditor != null) {
            int minutes = timeEditor.getMinutes();
            if (minutes >= 0) {
                reminder.timeInMinutes = minutes;
            }
        }
        return reminder;
    }
}
