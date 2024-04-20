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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.futsch1.medtimer.MedicineViewModel;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.helpers.TimeHelper;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.time.LocalDate;
import java.util.ArrayList;

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
    private TextView remindOnDays;
    private View advancedReminderView;
    private AdvancedReminderSettingsFragmentArgs args;

    public AdvancedReminderSettingsFragment() {
        backgroundThread = new HandlerThread("AdvancedReminderSettings");
        backgroundThread.start();
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Handler handler = new Handler(backgroundThread.getLooper());
        handler.post(this::loadReminder);

        assert getArguments() != null;
        args = AdvancedReminderSettingsFragmentArgs.fromBundle(getArguments());

        daysArray = getResources().getStringArray(R.array.days);

        advancedReminderView = inflater.inflate(R.layout.fragment_advanced_reminder_settings, container, false);

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

        editPauseDays = advancedReminderView.findViewById(R.id.pauseDays);
        editConsecutiveDays = advancedReminderView.findViewById(R.id.consecutiveDays);
        editCycleStartDate = advancedReminderView.findViewById(R.id.cycleStartDate);
        editInstructions = advancedReminderView.findViewById(R.id.editInstructions);
        instructionSuggestions = advancedReminderView.findViewById(R.id.editInstructionsLayout);
        remindOnDays = advancedReminderView.findViewById(R.id.remindOnDays);

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
        setDaysText();

        remindOnDays.setOnClickListener(v -> {

            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.remind_on)
                    .setCancelable(false);

            boolean[] checkedItems = new boolean[daysArray.length];
            for (int i = 0; i < daysArray.length; i++) {
                checkedItems[i] = reminder.days.get(i);
            }
            builder.setMultiChoiceItems(daysArray, checkedItems, (dialogInterface, i, b) -> checkedItems[i] = b);

            builder.setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                for (int j = 0; j < daysArray.length; j++) {
                    reminder.days.set(j, checkedItems[j]);
                }

                setDaysText();
            });

            builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());
            // show dialog
            builder.show();
        });
    }

    private void setupCycleStartDate() {
        setCycleStartDate();
        editCycleStartDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                LocalDate startDate = LocalDate.ofEpochDay(reminder.cycleStartDay);
                MaterialDatePicker<Long> datePickerDialog = MaterialDatePicker.Builder.datePicker()
                        .setSelection(startDate.toEpochDay() * DateUtils.DAY_IN_MILLIS)
                        .build();
                datePickerDialog.addOnPositiveButtonClickListener(selectedDate -> {
                    reminder.cycleStartDay = selectedDate / DateUtils.DAY_IN_MILLIS;
                    setCycleStartDate();
                });
                datePickerDialog.show(getParentFragmentManager(), "date_picker");
            }
        });
    }

    private void setDaysText() {
        ArrayList<String> checkedDays = new ArrayList<>();
        for (int j = 0; j < daysArray.length; j++) {
            if (Boolean.TRUE.equals(reminder.days.get(j))) {
                checkedDays.add(daysArray[j]);
            }
        }

        if (checkedDays.size() == daysArray.length) {
            remindOnDays.setText(R.string.every_day);
        } else {
            remindOnDays.setText(String.join(", ", checkedDays));
        }

    }

    private void setCycleStartDate() {
        editCycleStartDate.setText(TimeHelper.daysSinceEpochToDate(reminder.cycleStartDay));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        reminder.instructions = editInstructions.getText() != null ? editInstructions.getText().toString() : "";
        try {
            reminder.consecutiveDays = Integer.parseInt(editConsecutiveDays.getText().toString());
            if (reminder.consecutiveDays <= 0) {
                reminder.consecutiveDays = 1;
            }
        } catch (NumberFormatException e) {
            reminder.consecutiveDays = 1;
        }
        try {
            reminder.pauseDays = Integer.parseInt(editPauseDays.getText().toString());
        } catch (NumberFormatException e) {
            reminder.pauseDays = 0;
        }

        medicineViewModel.updateReminder(reminder);
    }

}