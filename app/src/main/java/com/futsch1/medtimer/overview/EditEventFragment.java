package com.futsch1.medtimer.overview;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.fragment.app.Fragment;

import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.helpers.TimeHelper;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.time.LocalDate;

public class EditEventFragment extends Fragment {

    private final HandlerThread backgroundThread;
    private int eventId;
    private EditText editEventName;
    private EditText editEventAmount;
    private EditText editEventRemindedTimestamp;
    private EditText editEventRemindedDate;
    private MedicineRepository medicineRepository;

    public EditEventFragment() {
        backgroundThread = new HandlerThread("EditEvent");
        backgroundThread.start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View editEventView = inflater.inflate(R.layout.fragment_edit_event, container, false);

        medicineRepository = new MedicineRepository(requireActivity().getApplication());

        assert getArguments() != null;
        EditEventFragmentArgs editEventArgs = EditEventFragmentArgs.fromBundle(getArguments());
        eventId = editEventArgs.getEventId();

        editEventName = editEventView.findViewById(R.id.editEventName);
        editEventName.setText(editEventArgs.getEventName());

        editEventAmount = editEventView.findViewById(R.id.editEventAmount);
        editEventAmount.setText(editEventArgs.getEventAmount());

        editEventRemindedTimestamp = editEventView.findViewById(R.id.editEventRemindedTimestamp);
        editEventRemindedTimestamp.setText(TimeHelper.toLocalizedTimeString(editEventRemindedTimestamp.getContext(),
                editEventArgs.getEventRemindedTimestamp()));
        editEventRemindedTimestamp.setOnFocusChangeListener((v, hasFocus) -> onFocusEditTime(hasFocus));

        editEventRemindedDate = editEventView.findViewById(R.id.editEventRemindedDate);
        editEventRemindedDate.setText(TimeHelper.toLocalizedDateString(editEventRemindedTimestamp.getContext(),
                editEventArgs.getEventRemindedTimestamp()));
        editEventRemindedDate.setOnFocusChangeListener((v, hasFocus) -> onFocusEditDate(hasFocus));
        editEventRemindedDate.setVisibility(editEventArgs.getEventCanEditDate() ? View.VISIBLE : View.GONE);

        return editEventView;
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
    public void onDestroy() {
        super.onDestroy();

        if (medicineRepository != null) {
            Handler handler = new Handler(backgroundThread.getLooper());
            handler.post(() -> {
                ReminderEvent reminderEvent = medicineRepository.getReminderEvent(eventId);
                if (reminderEvent != null) {
                    reminderEvent.medicineName = editEventName.getText().toString();
                    reminderEvent.amount = editEventAmount.getText().toString();
                    reminderEvent.remindedTimestamp = TimeHelper.changeTimeStampMinutes(reminderEvent.remindedTimestamp,
                            TimeHelper.timeStringToMinutes(editEventRemindedTimestamp.getContext(), editEventRemindedTimestamp.getText().toString()));
                    reminderEvent.remindedTimestamp = TimeHelper.changeTimeStampDate(reminderEvent.remindedTimestamp,
                            TimeHelper.dateStringToDate(editEventRemindedDate.getText().toString()));

                    medicineRepository.updateReminderEvent(reminderEvent);
                }
            });
        }

        backgroundThread.quitSafely();
    }
}