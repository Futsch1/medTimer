package com.futsch1.medtimer.medicine;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.helpers.DatabaseEntityEditFragment;
import com.futsch1.medtimer.helpers.ReminderEntityInterface;
import com.futsch1.medtimer.helpers.TimeHelper;
import com.futsch1.medtimer.medicine.editReminder.RemindOnDays;
import com.futsch1.medtimer.medicine.editors.DateTimeEditor;
import com.futsch1.medtimer.medicine.editors.IntervalEditor;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.time.LocalDate;

import kotlin.Unit;

public class AdvancedReminderSettingsFragment extends DatabaseEntityEditFragment<Reminder> {

    private String[] daysArray;
    private EditText editConsecutiveDays;
    private EditText editPauseDays;
    private EditText editCycleStartDate;
    private TextInputEditText editInstructions;
    private TextInputLayout instructionSuggestions;
    private PeriodSettings periodSettings;
    private DateTimeEditor intervalStartDateTimeEditor;
    private IntervalEditor intervalEditor;
    private MaterialSwitch variableAmount;

    public AdvancedReminderSettingsFragment() {
        super(new ReminderEntityInterface(), R.layout.fragment_advanced_reminder_settings, AdvancedReminderSettingsFragment.class.getName());
    }

    @Override
    protected void setupMenu(@NonNull View fragmentView) {
        // Intentionally empty
    }

    @SuppressLint("SetTextI18n")
    @Override
    public boolean onEntityLoaded(Reminder entity, @NonNull View fragmentView) {
        daysArray = getResources().getStringArray(R.array.days);

        periodSettings = new PeriodSettings(fragmentView, getParentFragmentManager(), entity);
        editInstructions = fragmentView.findViewById(R.id.editInstructions);
        editPauseDays = fragmentView.findViewById(R.id.pauseDays);
        editConsecutiveDays = fragmentView.findViewById(R.id.consecutiveDays);
        editCycleStartDate = fragmentView.findViewById(R.id.cycleStartDate);
        instructionSuggestions = fragmentView.findViewById(R.id.editInstructionsLayout);
        variableAmount = fragmentView.findViewById(R.id.variableAmount);

        editConsecutiveDays.setText(Integer.toString(entity.consecutiveDays));
        editPauseDays.setText(Integer.toString(entity.pauseDays));
        editInstructions.setText(entity.instructions);
        variableAmount.setChecked(entity.variableAmount);

        setupInstructionSuggestions();

        setupRemindOnDays(entity, fragmentView);
        setupCycleStartDate(entity);

        setupAddLinkedReminder(entity, fragmentView);

        setupMenu(entity, fragmentView);

        if (entity.getReminderType() == Reminder.ReminderType.INTERVAL_BASED) {
            setupIntervalBasedReminderSettings(entity, fragmentView);
        }

        setupVisibilities(entity, fragmentView);

        return true;
    }

    @Override
    public void fillEntityData(Reminder entity, @NonNull View fragmentView) {
        entity.instructions = editInstructions.getText() != null ? editInstructions.getText().toString() : "";
        entity.variableAmount = variableAmount.isChecked();

        periodSettings.updateReminder();
        putConsecutiveDaysIntoReminder(entity);
        putPauseDaysIntoReminder(entity);
        putStartDateIntoReminder(entity);
        putIntervalIntoReminder(entity, fragmentView);
    }

    private void putConsecutiveDaysIntoReminder(Reminder entity) {
        try {
            if (entity.getReminderType() == Reminder.ReminderType.INTERVAL_BASED) {
                entity.consecutiveDays = Integer.parseInt(editConsecutiveDays.getText().toString());
                if (entity.consecutiveDays <= 0) {
                    entity.consecutiveDays = 1;
                }
            }
        } catch (NumberFormatException e) {
            entity.consecutiveDays = 1;
        }
    }

    private void putPauseDaysIntoReminder(Reminder entity) {
        try {
            entity.pauseDays = Integer.parseInt(editPauseDays.getText().toString());
        } catch (NumberFormatException e) {
            entity.pauseDays = 0;
        }
    }

    private void putStartDateIntoReminder(Reminder entity) {
        LocalDate startDate = getCycleStartDate();
        if (startDate != null) {
            entity.cycleStartDay = startDate.toEpochDay();
        }
    }

    private void putIntervalIntoReminder(Reminder entity, View fragmentView) {
        if (entity.getReminderType() == Reminder.ReminderType.INTERVAL_BASED) {
            int minutes = intervalEditor.getMinutes();
            if (minutes > 0) {
                entity.timeInMinutes = minutes;
            }
            long intervalStartDateTime = intervalStartDateTimeEditor.getDateTimeSecondsSinceEpoch();
            if (intervalStartDateTime >= 0) {
                entity.intervalStart = intervalStartDateTime;
            }
            entity.intervalStartsFromProcessed = ((RadioButton) fragmentView.findViewById(R.id.intervalStarsFromProcessed)).isChecked();
        }
    }

    @Override
    public int getEntityId() {
        return AdvancedReminderSettingsFragmentArgs.fromBundle(requireArguments()).getReminderId();
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

    private void setupRemindOnDays(Reminder entity, View fragmentView) {
        String[] daysOfMonth = new String[31];
        for (int i = 0; i < 31; i++) {
            daysOfMonth[i] = Integer.toString(i + 1);
        }
        new RemindOnDays(requireContext(), fragmentView.findViewById(R.id.remindOnWeekdays), new RemindOnDays.Strings(R.string.every_day, null, R.string.never), daysArray,
                i -> entity.days.get(i),
                (i, b) -> {
                    entity.days.set(i, b);
                    return Unit.INSTANCE;
                });
        new RemindOnDays(requireContext(), fragmentView.findViewById(R.id.remindOnDaysOfMonth), new RemindOnDays.Strings(R.string.every_day_of_month, R.string.on_day_of_month, R.string.never), daysOfMonth,
                i -> (entity.activeDaysOfMonth & (1 << i)) != 0,
                (i, b) -> {
                    if (Boolean.TRUE.equals(b)) {
                        entity.activeDaysOfMonth |= (1 << i);
                    } else {
                        entity.activeDaysOfMonth &= ~(1 << i);
                    }

                    return Unit.INSTANCE;
                });
    }

    private void setupCycleStartDate(Reminder entity) {
        setCycleStartDate(entity.cycleStartDay);
        editCycleStartDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                TimeHelper.DatePickerWrapper datePickerWrapper = new TimeHelper.DatePickerWrapper(requireActivity().getSupportFragmentManager(), R.string.cycle_start_date);
                datePickerWrapper.show(getCycleStartDate(), this::setCycleStartDate);
            }
        });
    }

    private void setupAddLinkedReminder(Reminder entity, View fragmentView) {
        ExtendedFloatingActionButton addLinkedReminder = fragmentView.findViewById(R.id.addLinkedReminder);
        addLinkedReminder.setOnClickListener(v -> new LinkedReminderHandling(entity, medicineViewModel).addLinkedReminder(requireActivity()));
    }

    protected void setupMenu(Reminder entity, @NonNull View fragmentView) {
        requireActivity().addMenuProvider(new AdvancedReminderSettingsMenuProvider(entity, getThread(), medicineViewModel, fragmentView),
                getViewLifecycleOwner());
    }

    private void setupIntervalBasedReminderSettings(Reminder entity, View fragmentView) {
        intervalEditor = new IntervalEditor(
                fragmentView.findViewById(R.id.editIntervalTime),
                fragmentView.findViewById(R.id.intervalUnit), entity.timeInMinutes
        );

        intervalStartDateTimeEditor = new DateTimeEditor(
                requireActivity(),
                fragmentView.findViewById(R.id.editIntervalStartDateTime),
                entity.intervalStart
        );

        RadioGroup intervalBasedGroup = fragmentView.findViewById(R.id.intervalStartType);
        intervalBasedGroup.check(entity.intervalStartsFromProcessed ? R.id.intervalStarsFromProcessed : R.id.intervalStartsFromReminded);
    }

    private void setupVisibilities(Reminder entity, View fragmentView) {
        Reminder.ReminderType reminderType = entity.getReminderType();
        fragmentView.findViewById(R.id.cyclicRemindersGroup).setVisibility(reminderType == Reminder.ReminderType.TIME_BASED ? View.VISIBLE : View.GONE);
        fragmentView.findViewById(R.id.timeBasedGroup).setVisibility(reminderType == Reminder.ReminderType.TIME_BASED ? View.VISIBLE : View.GONE);
        fragmentView.findViewById(R.id.intervalBasedGroup).setVisibility(reminderType == Reminder.ReminderType.INTERVAL_BASED ? View.VISIBLE : View.GONE);
    }

    private @Nullable LocalDate getCycleStartDate() {
        return TimeHelper.dateStringToDate(editCycleStartDate.getText().toString());
    }

    private void setCycleStartDate(long daysSinceEpoch) {
        editCycleStartDate.setText(TimeHelper.daysSinceEpochToDateString(editCycleStartDate.getContext(),
                daysSinceEpoch));
    }
}