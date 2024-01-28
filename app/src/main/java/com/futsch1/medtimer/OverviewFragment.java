package com.futsch1.medtimer;

import static com.futsch1.medtimer.ActivityCodes.NEXT_REMINDER_ACTION;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
    private NextReminderListener nextReminderListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragmentOverview = inflater.inflate(R.layout.fragment_overview, container, false);
        MedicineViewModel medicineViewModel = new ViewModelProvider(this).get(MedicineViewModel.class);

        nextReminderListener = new NextReminderListener(fragmentOverview.findViewById(R.id.nextReminderInfo), medicineViewModel);
        Intent nextReminder = requireContext().registerReceiver(nextReminderListener, new IntentFilter(NEXT_REMINDER_ACTION), Context.RECEIVER_EXPORTED);
        if (nextReminder != null) {
            nextReminderListener.onReceive(requireContext(), nextReminder);
        }

        RecyclerView latestReminders = fragmentOverview.findViewById(R.id.latestReminders);

        final LatestRemindersViewAdapter adapter = new LatestRemindersViewAdapter(new LatestRemindersViewAdapter.ReminderEventDiff(), medicineViewModel);
        latestReminders.setAdapter(adapter);
        latestReminders.setLayoutManager(new LinearLayoutManager(fragmentOverview.getContext()));

        medicineViewModel.getReminderEvents(5).observe(getViewLifecycleOwner(), adapter::submitList);

        return fragmentOverview;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        requireContext().unregisterReceiver(nextReminderListener);
        nextReminderListener.stop();
    }
}