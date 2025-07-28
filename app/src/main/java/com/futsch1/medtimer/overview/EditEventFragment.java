package com.futsch1.medtimer.overview;

import android.text.format.DateUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.helpers.DatabaseEntityEditFragment;
import com.futsch1.medtimer.helpers.ReminderEventEntityInterface;
import com.futsch1.medtimer.helpers.TimeHelper;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.time.LocalDate;

public class EditEventFragment extends DatabaseEntityEditFragment<ReminderEvent> {

    private EditText editEventName;
    private EditText editEventAmount;
    private EditText editEventRemindedTimestamp;
    private EditText editEventRemindedDate;
    private EditText editEventTakenTimestamp;
    private EditText editEventTakenDate;

    public EditEventFragment() {
        super(new ReminderEventEntityInterface(), R.layout.sidesheet_edit_event, EditEventFragment.class.getName());
    }


    @Override
    protected void setupMenu(@NonNull View fragmentView) {
        requireActivity().addMenuProvider(new EditEventMenuProvider(getEntityId(), this.getThread(), this.getMedicineViewModel(), fragmentView), getViewLifecycleOwner());
    }

    @Override
    public boolean onEntityLoaded(ReminderEvent entity, @NonNull View fragmentView) {
        editEventName = fragmentView.findViewById(R.id.editEventName);
        editEventName.setText(entity.medicineName);

        editEventAmount = fragmentView.findViewById(R.id.editEventAmount);
        editEventAmount.setText(entity.amount);

        editEventRemindedTimestamp = fragmentView.findViewById(R.id.editEventRemindedTimestamp);
        setupEditTime(entity.remindedTimestamp, editEventRemindedTimestamp);
        editEventRemindedDate = fragmentView.findViewById(R.id.editEventRemindedDate);
        setupEditDate(entity.remindedTimestamp, editEventRemindedDate);

        editEventTakenTimestamp = fragmentView.findViewById(R.id.editEventTakenTimestamp);
        editEventTakenDate = fragmentView.findViewById(R.id.editEventTakenDate);
        configureTakenText(fragmentView, entity);
        if (entity.status != ReminderEvent.ReminderStatus.RAISED) {
            setupEditTime(entity.processedTimestamp, editEventTakenTimestamp);
            setupEditDate(entity.processedTimestamp, editEventTakenDate);
        } else {
            editEventTakenTimestamp.setVisibility(View.GONE);
            editEventTakenDate.setVisibility(View.GONE);
        }

        return true;
    }

    private void setupEditTime(long timestamp, EditText editText) {
        editText.setText(TimeHelper.toLocalizedTimeString(editText.getContext(),
                timestamp));
        editText.setOnFocusChangeListener((v, hasFocus) -> onFocusEditTime(hasFocus, editText));
    }

    private void setupEditDate(long timestamp, EditText editText) {
        editText.setText(TimeHelper.toLocalizedDateString(editEventRemindedTimestamp.getContext(),
                timestamp));
        editText.setOnFocusChangeListener((v, hasFocus) -> onFocusEditDate(hasFocus, editText));
        editText.setVisibility(View.VISIBLE);
    }

    private void configureTakenText(View fragmentView, ReminderEvent entity) {
        TextView takenText = fragmentView.findViewById(R.id.takenText);
        if (entity.status == ReminderEvent.ReminderStatus.TAKEN) {
            takenText.setText(R.string.taken);
        } else if (entity.status == ReminderEvent.ReminderStatus.SKIPPED) {
            takenText.setText(R.string.skipped);
        } else {
            takenText.setVisibility(View.GONE);
        }
    }

    private void onFocusEditTime(boolean hasFocus, EditText editText) {
        if (hasFocus) {
            int startMinutes = TimeHelper.timeStringToMinutes(editText.getContext(),
                    editText.getText().toString());
            if (startMinutes < 0) {
                startMinutes = Reminder.DEFAULT_TIME;
            }
            new TimeHelper.TimePickerWrapper(requireActivity()).show(startMinutes / 60, startMinutes % 60, minutes -> {
                try {
                    String selectedTime = TimeHelper.minutesToTimeString(requireContext(), minutes);
                    editText.setText(selectedTime);
                } catch (IllegalStateException e) {
                    // Intentionally empty
                }
            });
        }
    }

    private void onFocusEditDate(boolean hasFocus, EditText editText) {
        if (hasFocus) {
            LocalDate startDate = TimeHelper.dateStringToDate(requireContext(), editText.getText().toString());
            if (startDate == null) {
                startDate = LocalDate.now();
            }

            MaterialDatePicker<Long> datePickerDialog = MaterialDatePicker.Builder.datePicker()
                    .setSelection(startDate.toEpochDay() * DateUtils.DAY_IN_MILLIS)
                    .build();
            datePickerDialog.addOnPositiveButtonClickListener(selectedDate -> {
                String selectedDateString = TimeHelper.toLocalizedDateString(editText.getContext(),
                        selectedDate / 1000);
                editText.setText(selectedDateString);
            });
            datePickerDialog.show(getParentFragmentManager(), "date_picker");
        }
    }

    @Override
    public void fillEntityData(ReminderEvent entity, @NonNull View fragmentView) {
        entity.medicineName = editEventName.getText().toString();
        entity.amount = editEventAmount.getText().toString();

        entity.remindedTimestamp = processDateTimeEdits(entity.remindedTimestamp, editEventRemindedTimestamp, editEventRemindedDate);
        entity.processedTimestamp = processDateTimeEdits(entity.processedTimestamp, editEventTakenTimestamp, editEventTakenDate);
    }

    private long processDateTimeEdits(long timestamp, EditText editTimestamp, EditText editDate) {
        int minutes = TimeHelper.timeStringToMinutes(editTimestamp.getContext(), editTimestamp.getText().toString());
        if (minutes >= 0) {
            timestamp = TimeHelper.changeTimeStampMinutes(timestamp, minutes);
        }
        timestamp = TimeHelper.changeTimeStampDate(timestamp,
                TimeHelper.dateStringToDate(requireContext(), editDate.getText().toString()));
        return timestamp;
    }

    @Override
    public int getEntityId() {
        return EditEventFragmentArgs.fromBundle(requireArguments()).getEventId();
    }
}