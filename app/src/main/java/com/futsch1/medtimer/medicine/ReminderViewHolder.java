package com.futsch1.medtimer.medicine;

import static com.futsch1.medtimer.helpers.TimeHelper.minutesToTimeString;
import static com.futsch1.medtimer.helpers.TimeHelper.timeStringToMinutes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.helpers.ReminderHelperKt;
import com.futsch1.medtimer.helpers.TimeHelper;
import com.google.android.material.button.MaterialButton;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class ReminderViewHolder extends RecyclerView.ViewHolder {
    private final EditText editTime;
    private final EditText editAmount;
    private final View holderItemView;
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

        this.holderItemView = itemView;
        this.fragmentActivity = fragmentActivity;
    }

    static ReminderViewHolder create(ViewGroup parent, FragmentActivity fragmentActivity) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_reminder, parent, false);
        return new ReminderViewHolder(view, fragmentActivity);
    }

    @SuppressLint("SetTextI18n")
    public void bind(Reminder reminder, String medicineName, DeleteCallback deleteCallback) {
        this.reminder = reminder;

        editTime.setText(minutesToTimeString(editTime.getContext(), reminder.timeInMinutes));
        editTime.setOnFocusChangeListener((v, hasFocus) -> onFocusEditTime(reminder, hasFocus));

        advancedSettings.setOnClickListener(v -> onClickAdvancedSettings(reminder, medicineName));

        holderItemView.setOnLongClickListener(v -> onLongClick(deleteCallback));

        editAmount.setText(reminder.amount);
        advancedSettingsSummary.setText(getAdvancedSettingsSummary(reminder));
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
        NavController navController = Navigation.findNavController(this.holderItemView);
        EditMedicineFragmentDirections.ActionEditMedicineToAdvancedReminderSettings action =
                EditMedicineFragmentDirections.actionEditMedicineToAdvancedReminderSettings(
                        reminder.reminderId,
                        medicineName
                );
        navController.navigate(action);
    }

    private boolean onLongClick(DeleteCallback deleteCallback) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.holderItemView.getContext());

        if (sharedPref.getString("delete_items", "0").equals("0")) {
            return false;
        }

        PopupMenu popupMenu = new PopupMenu(editTime.getContext(), this.holderItemView);
        popupMenu.getMenuInflater().inflate(R.menu.edit_delete_popup, popupMenu.getMenu());
        popupMenu.getMenu().findItem(R.id.edit).setVisible(false);
        popupMenu.getMenu().findItem(R.id.delete).setOnMenuItemClickListener(item -> {
            deleteCallback.deleteItem(editTime.getContext(), getItemId(), getBindingAdapterPosition());
            return true;
        });
        popupMenu.show();
        return true;
    }

    private String getAdvancedSettingsSummary(Reminder reminder) {
        List<String> strings = new LinkedList<>();

        boolean weekdayLimited = !reminder.days.stream().allMatch(day -> day == Boolean.TRUE);
        boolean cyclic = reminder.pauseDays > 0;
        if (!ReminderHelperKt.isReminderActive(reminder)) {
            strings.add(holderItemView.getContext().getString(R.string.inactive));
        }
        if (weekdayLimited) {
            strings.add(holderItemView.getContext().getString(R.string.weekday_limited));
        }
        if (cyclic) {
            strings.add(getCyclicReminderString(reminder));
        }
        if (!weekdayLimited && !cyclic) {
            strings.add(holderItemView.getContext().getString(R.string.every_day));
        }
        if (reminder.instructions != null && !reminder.instructions.isEmpty()) {
            strings.add(reminder.instructions);
        }

        return String.join(", ", strings);
    }

    private @NonNull String getCyclicReminderString(Reminder reminder) {
        return holderItemView.getContext().getString(R.string.cycle_reminders) +
                " " +
                reminder.consecutiveDays +
                "/" +
                reminder.pauseDays +
                ", " +
                firstToLower(holderItemView.getContext().getString(R.string.cycle_start_date)) +
                " " +
                TimeHelper.daysSinceEpochToDateString(editTime.getContext(), reminder.cycleStartDay);
    }

    private String firstToLower(String string) {
        return string.substring(0, 1).toLowerCase(Locale.getDefault()) + string.substring(1);
    }

    public Reminder getReminder() {
        reminder.amount = editAmount.getText().toString();
        int minutes = timeStringToMinutes(editTime.getContext(), editTime.getText().toString());
        if (minutes >= 0) {
            reminder.timeInMinutes = minutes;
        }
        return reminder;
    }

    public interface DeleteCallback {
        void deleteItem(Context context, long itemId, int adapterPosition);
    }
}
