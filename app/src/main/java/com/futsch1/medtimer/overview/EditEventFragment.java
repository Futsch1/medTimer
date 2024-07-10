package com.futsch1.medtimer.overview;

import static com.futsch1.medtimer.helpers.TimeHelper.changeTimeStampMinutes;
import static com.futsch1.medtimer.helpers.TimeHelper.minutesToTimeString;
import static com.futsch1.medtimer.helpers.TimeHelper.timeStringToMinutes;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
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

import java.time.ZoneId;

public class EditEventFragment extends Fragment {

    private final HandlerThread backgroundThread;
    private int eventId;
    private EditText editEventName;
    private EditText editEventAmount;
    private EditText editEventRemindedTimestamp;
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
        editEventRemindedTimestamp.setText(TimeHelper.toLocalizedTimeString(editEventArgs.getEventRemindedTimestamp(), ZoneId.systemDefault()));
        editEventRemindedTimestamp.setOnFocusChangeListener((v, hasFocus) -> onFocusEditTime(hasFocus));

        return editEventView;
    }

    private void onFocusEditTime(boolean hasFocus) {
        if (hasFocus) {
            int startMinutes = timeStringToMinutes(editEventRemindedTimestamp.getText().toString());
            if (startMinutes < 0) {
                startMinutes = Reminder.DEFAULT_TIME;
            }
            new TimeHelper.TimePickerWrapper(requireActivity()).show(startMinutes / 60, startMinutes % 60, minutes -> {
                String selectedTime = minutesToTimeString(requireContext(), minutes);
                editEventRemindedTimestamp.setText(selectedTime);
            });
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        if (medicineRepository != null) {
            Handler handler = new Handler(backgroundThread.getLooper());
            handler.post(() -> {
                ReminderEvent reminderEvent = medicineRepository.getReminderEvent(eventId);
                reminderEvent.medicineName = editEventName.getText().toString();
                reminderEvent.amount = editEventAmount.getText().toString();
                reminderEvent.remindedTimestamp = changeTimeStampMinutes(reminderEvent.remindedTimestamp, timeStringToMinutes(editEventRemindedTimestamp.getText().toString()));

                medicineRepository.updateReminderEvent(reminderEvent);
            });
        }

        backgroundThread.quitSafely();
    }
}