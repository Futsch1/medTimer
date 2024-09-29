package com.futsch1.medtimer.medicine;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.futsch1.medtimer.MedicineViewModel;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.helpers.TimeHelper;
import com.futsch1.medtimer.medicine.editReminder.RemindOnDays;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.time.LocalDate;

import kotlin.Unit;

public class AdvancedReminderSettingsFragment extends Fragment {

    private final HandlerThread backgroundThread;
    private String[] daysArray;
    private EditText editConsecutiveDays;
    private EditText editPauseDays;
    private EditText editCycleStartDate;
    private TextInputEditText editInstructions;
    private TextInputLayout instructionSuggestions;
    private MedicineViewModel medicineViewModel;
    private Reminder reminder;
    private View advancedReminderView;
    private PeriodSettings periodSettings;
    private AdvancedReminderSettingsFragmentArgs args;

    public AdvancedReminderSettingsFragment() {
        backgroundThread = new HandlerThread("AdvancedReminderSettings");
        backgroundThread.start();
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        assert getArguments() != null;
        args = AdvancedReminderSettingsFragmentArgs.fromBundle(getArguments());

        daysArray = getResources().getStringArray(R.array.days);

        advancedReminderView = inflater.inflate(R.layout.fragment_advanced_reminder_settings, container, false);

        Handler handler = new Handler(backgroundThread.getLooper());
        handler.post(this::loadReminder);

        return advancedReminderView;
    }

    private void loadReminder() {
        int reminderId = args.getReminderId();
        medicineViewModel = new ViewModelProvider(this).get(MedicineViewModel.class);

        reminder = medicineViewModel.getReminder(reminderId);

        requireActivity().runOnUiThread(this::setupView);
    }

    @SuppressLint("SetTextI18n")
    private void setupView() {
        requireActivity().addMenuProvider(new AdvancedReminderSettingsMenuProvider(reminder, backgroundThread, medicineViewModel, advancedReminderView),
                getViewLifecycleOwner());

        periodSettings = new PeriodSettings(advancedReminderView, getParentFragmentManager(), reminder);
        editInstructions = advancedReminderView.findViewById(R.id.editInstructions);
        editPauseDays = advancedReminderView.findViewById(R.id.pauseDays);
        editConsecutiveDays = advancedReminderView.findViewById(R.id.consecutiveDays);
        editCycleStartDate = advancedReminderView.findViewById(R.id.cycleStartDate);
        instructionSuggestions = advancedReminderView.findViewById(R.id.editInstructionsLayout);

        editConsecutiveDays.setText(Integer.toString(reminder.consecutiveDays));
        editPauseDays.setText(Integer.toString(reminder.pauseDays));
        editInstructions.setText(reminder.instructions);

        setupInstructionSuggestions();

        setupRemindOnDays();
        setupCycleStartDate();
    }

    private void setupInstructionSuggestions() {
        instructionSuggestions.setEndIconOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle(R.string.instruction_templates);
            builder.setItems(R.array.instructions_suggestions, (dialog, which) -> {
                if (which > 0) {
                    String[] instructionSuggestionsArray = getResources().getStringArray(R.array.instructions_suggestions);
                    editInstructions.setText(instructionSuggestionsArray[which]);
                } else {
                    editInstructions.setText("");
                }
            });
            builder.show();

        });
    }

    private void setupRemindOnDays() {
        String[] daysOfMonth = new String[31];
        for (int i = 0; i < 31; i++) {
            daysOfMonth[i] = Integer.toString(i + 1);
        }
        new RemindOnDays(requireContext(), advancedReminderView.findViewById(R.id.remindOnWeekdays), new RemindOnDays.Strings(R.string.every_day, null, R.string.never), daysArray,
                i -> reminder.days.get(i),
                (i, b) -> {
                    reminder.days.set(i, b);
                    return Unit.INSTANCE;
                });
        new RemindOnDays(requireContext(), advancedReminderView.findViewById(R.id.remindOnDaysOfMonth), new RemindOnDays.Strings(R.string.every_day_of_month, R.string.on_day_of_month, R.string.never), daysOfMonth,
                i -> (reminder.activeDaysOfMonth & (1 << i)) != 0,
                (i, b) -> {
                    if (Boolean.TRUE.equals(b)) {
                        reminder.activeDaysOfMonth |= (1 << i);
                    } else {
                        reminder.activeDaysOfMonth &= ~(1 << i);
                    }

                    return Unit.INSTANCE;
                });
    }

    private void setupCycleStartDate() {
        setCycleStartDate(reminder.cycleStartDay);
        editCycleStartDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                LocalDate startDate = getCycleStartDate();
                if (startDate == null) {
                    startDate = LocalDate.now();
                }
                MaterialDatePicker<Long> datePickerDialog = MaterialDatePicker.Builder.datePicker()
                        .setSelection(startDate.toEpochDay() * DateUtils.DAY_IN_MILLIS)
                        .build();
                datePickerDialog.addOnPositiveButtonClickListener(selectedDate -> setCycleStartDate(selectedDate / DateUtils.DAY_IN_MILLIS));
                datePickerDialog.show(getParentFragmentManager(), "date_picker");
            }
        });
    }

    private @Nullable LocalDate getCycleStartDate() {
        return TimeHelper.dateStringToDate(editCycleStartDate.getText().toString());
    }

    private void setCycleStartDate(long daysSinceEpoch) {
        editCycleStartDate.setText(TimeHelper.daysSinceEpochToDateString(editCycleStartDate.getContext(),
                daysSinceEpoch));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (editInstructions != null) {
            reminder.instructions = editInstructions.getText() != null ? editInstructions.getText().toString() : "";

            periodSettings.updateReminder();
            putConsecutiveDaysIntoReminder();
            putPauseDaysIntoReminder();
            putStartDateIntoReminder();

            medicineViewModel.updateReminder(reminder);
        }
    }

    private void putConsecutiveDaysIntoReminder() {
        try {
            reminder.consecutiveDays = Integer.parseInt(editConsecutiveDays.getText().toString());
            if (reminder.consecutiveDays <= 0) {
                reminder.consecutiveDays = 1;
            }
        } catch (NumberFormatException e) {
            reminder.consecutiveDays = 1;
        }
    }

    private void putPauseDaysIntoReminder() {
        try {
            reminder.pauseDays = Integer.parseInt(editPauseDays.getText().toString());
        } catch (NumberFormatException e) {
            reminder.pauseDays = 0;
        }
    }

    private void putStartDateIntoReminder() {
        LocalDate startDate = getCycleStartDate();
        if (startDate != null) {
            reminder.cycleStartDay = startDate.toEpochDay();
        }
    }
}