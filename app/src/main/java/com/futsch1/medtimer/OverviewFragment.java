package com.futsch1.medtimer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.adapters.LatestRemindersViewAdapter;

public class OverviewFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragmentOverview = inflater.inflate(R.layout.fragment_overview, container, false);

        RecyclerView latestReminders = fragmentOverview.findViewById(R.id.latestReminders);

        MedicineViewModel medicineViewModel = new ViewModelProvider(this).get(MedicineViewModel.class);

        final LatestRemindersViewAdapter adapter = new LatestRemindersViewAdapter(new LatestRemindersViewAdapter.ReminderEventDiff(), medicineViewModel);
        latestReminders.setAdapter(adapter);
        latestReminders.setLayoutManager(new LinearLayoutManager(fragmentOverview.getContext()));

        medicineViewModel.getReminderEvents(5).observe(getViewLifecycleOwner(), adapter::submitList);

        return fragmentOverview;
    }
}