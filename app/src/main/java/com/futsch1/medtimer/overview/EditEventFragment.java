package com.futsch1.medtimer.overview;

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
import com.futsch1.medtimer.database.ReminderEvent;

public class EditEventFragment extends Fragment {

    private final HandlerThread backgroundThread;
    private int eventId;
    private EditText editEventName;
    private EditText editEventAmount;
    private MedicineRepository medicineRepository;

    public EditEventFragment() {
        backgroundThread = new HandlerThread("EditEvent");
        backgroundThread.start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View editEventView = inflater.inflate(R.layout.fragment_edit_event, container, false);

        medicineRepository = new MedicineRepository(requireActivity().getApplication());

        assert getArguments() != null;
        EditEventFragmentArgs editEventArgs = EditEventFragmentArgs.fromBundle(getArguments());
        eventId = editEventArgs.getEventId();

        editEventName = editEventView.findViewById(R.id.editEventName);
        editEventName.setText(editEventArgs.getEventName());

        editEventAmount = editEventView.findViewById(R.id.editEventAmount);
        editEventAmount.setText(editEventArgs.getEventAmount());

        return editEventView;
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

                medicineRepository.updateReminderEvent(reminderEvent);
            });
        }

        backgroundThread.quitSafely();
    }
}