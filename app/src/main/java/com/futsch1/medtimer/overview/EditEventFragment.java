package com.futsch1.medtimer.overview;

import android.text.format.DateUtils;
import android.view.View;
import android.widget.EditText;

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

    public EditEventFragment() {
        super(new ReminderEventEntityInterface(), R.layout.fragment_edit_event, EditEventFragment.class.getName());
    }

    @Override
    public boolean onEntityLoaded(ReminderEvent entity, @NonNull View fragmentView) {
        editEventName = fragmentView.findViewById(R.id.editEventName);
        editEventName.setText(entity.medicineName);

        editEventAmount = fragmentView.findViewById(R.id.editEventAmount);
        editEventAmount.setText(entity.amount);

        editEventRemindedTimestamp = fragmentView.findViewById(R.id.editEventRemindedTimestamp);
        editEventRemindedTimestamp.setText(TimeHelper.toLocalizedTimeString(editEventRemindedTimestamp.getContext(),
                entity.remindedTimestamp));
        editEventRemindedTimestamp.setOnFocusChangeListener((v, hasFocus) -> onFocusEditTime(hasFocus));

        editEventRemindedDate = fragmentView.findViewById(R.id.editEventRemindedDate);
        editEventRemindedDate.setText(TimeHelper.toLocalizedDateString(editEventRemindedTimestamp.getContext(),
                entity.remindedTimestamp));
        editEventRemindedDate.setOnFocusChangeListener((v, hasFocus) -> onFocusEditDate(hasFocus));
        editEventRemindedDate.setVisibility(EditEventFragmentArgs.fromBundle(requireArguments()).getEventCanEditDate() ? View.VISIBLE : View.GONE);

        return true;
    }

    private void onFocusEditTime(boolean hasFocus) {
        if (hasFocus) {
            int startMinutes = TimeHelper.timeStringToMinutes(editEventRemindedTimestamp.getContext(),
                    editEventRemindedTimestamp.getText().toString());
            if (startMinutes < 0) {
                startMinutes = Reminder.DEFAULT_TIME;
            }
            new TimeHelper.TimePickerWrapper(requireActivity()).show(startMinutes / 60, startMinutes % 60, minutes -> {
                String selectedTime = TimeHelper.minutesToTimeString(requireContext(), minutes);
                editEventRemindedTimestamp.setText(selectedTime);
            });
        }
    }

    private void onFocusEditDate(boolean hasFocus) {
        if (hasFocus) {
            LocalDate startDate = TimeHelper.dateStringToDate(editEventRemindedDate.getText().toString());
            if (startDate == null) {
                startDate = LocalDate.now();
            }

            MaterialDatePicker<Long> datePickerDialog = MaterialDatePicker.Builder.datePicker()
                    .setSelection(startDate.toEpochDay() * DateUtils.DAY_IN_MILLIS)
                    .build();
            datePickerDialog.addOnPositiveButtonClickListener(selectedDate -> {
                String selectedDateString = TimeHelper.toLocalizedDateString(editEventRemindedDate.getContext(),
                        selectedDate / 1000);
                editEventRemindedDate.setText(selectedDateString);
            });
            datePickerDialog.show(getParentFragmentManager(), "date_picker");
        }
    }

    @Override
    public void fillEntityData(ReminderEvent entity, @NonNull View fragmentView) {
        entity.medicineName = editEventName.getText().toString();
        entity.amount = editEventAmount.getText().toString();
        int minutes = TimeHelper.timeStringToMinutes(editEventRemindedTimestamp.getContext(), editEventRemindedTimestamp.getText().toString());
        if (minutes >= 0) {
            entity.remindedTimestamp = TimeHelper.changeTimeStampMinutes(entity.remindedTimestamp, minutes);
        }
        entity.remindedTimestamp = TimeHelper.changeTimeStampDate(entity.remindedTimestamp,
                TimeHelper.dateStringToDate(editEventRemindedDate.getText().toString()));
    }

    @Override
    public int getEntityId() {
        return EditEventFragmentArgs.fromBundle(requireArguments()).getEventId();
    }
}