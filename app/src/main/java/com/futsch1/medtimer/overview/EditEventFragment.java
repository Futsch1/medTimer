package com.futsch1.medtimer.overview;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.futsch1.medtimer.R;
import com.futsch1.medtimer.helpers.TimeHelper;
import com.google.android.material.textfield.TextInputEditText;

import java.time.ZoneId;

public class EditEventFragment extends Fragment {

    private int eventId;
    private TextInputEditText editEventName;
    private TextInputEditText editEventDate;
    private TextInputEditText editEventTime;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View editEventView = inflater.inflate(R.layout.fragment_edit_event, container, false);

        assert getArguments() != null;
        EditEventFragmentArgs editEventArgs = EditEventFragmentArgs.fromBundle(getArguments());
        eventId = editEventArgs.getEventId();
        String eventName = editEventArgs.getEventName();
        long eventTimestamp = editEventArgs.getEventTime();

        editEventName = editEventView.findViewById(R.id.editEventName);
        editEventName.setText(eventName);

        editEventDate = editEventView.findViewById(R.id.editEventDate);
        editEventDate.setText(TimeHelper.toLocalizedDateString(eventTimestamp, ZoneId.systemDefault()));

        editEventTime = editEventView.findViewById(R.id.editEventTime);
        editEventTime.setText(TimeHelper.toLocalizedTimeString(eventTimestamp, ZoneId.systemDefault()));

        return editEventView;
    }
}