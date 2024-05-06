package com.futsch1.medtimer.overview;

import static com.futsch1.medtimer.ActivityCodes.NEXT_REMINDER_ACTION;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.MedicineViewModel;
import com.futsch1.medtimer.NextReminderListener;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.reminders.ReminderProcessor;

import java.time.Instant;
import java.util.List;

public class OverviewFragment extends Fragment {
    private NextReminderListener nextReminderListener;
    private View fragmentOverview;
    private MedicineViewModel medicineViewModel;
    private LatestRemindersViewAdapter adapter;
    private LiveData<List<ReminderEvent>> liveData;
    private HandlerThread thread;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fragmentOverview = inflater.inflate(R.layout.fragment_overview, container, false);
        medicineViewModel = new ViewModelProvider(this).get(MedicineViewModel.class);

        nextReminderListener = new NextReminderListener(fragmentOverview.findViewById(R.id.nextReminderInfo), medicineViewModel);
        Intent nextReminder = requireContext().registerReceiver(nextReminderListener, new IntentFilter(NEXT_REMINDER_ACTION), Context.RECEIVER_EXPORTED);
        if (nextReminder != null) {
            nextReminderListener.onReceive(requireContext(), nextReminder);
        }

        RecyclerView latestReminders = fragmentOverview.findViewById(R.id.latestReminders);

        adapter = new LatestRemindersViewAdapter(new LatestRemindersViewAdapter.ReminderEventDiff());
        latestReminders.setAdapter(adapter);
        latestReminders.setLayoutManager(new LinearLayoutManager(fragmentOverview.getContext()));
        // Scroll to top as soon as a new item was inserted
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                latestReminders.scrollToPosition(0);
            }
        });

        thread = new HandlerThread("LogManualDose");
        thread.start();
        setupLogManualDose();

        return fragmentOverview;
    }

    private void setupLogManualDose() {
        Button logManualDose = fragmentOverview.findViewById(R.id.logManualDose);
        logManualDose.setOnClickListener(v -> {
            Handler handler = new Handler(thread.getLooper());
            // Run the setup of the drop down in a separate thread to access the database
            handler.post(() -> new ManualDose(requireContext(), medicineViewModel.medicineRepository, this.requireActivity()).
                    logManualDose());
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(fragmentOverview.getContext());
        long eventAgeHours = Long.parseLong(sharedPref.getString("overview_events", "24"));
        if (liveData != null) {
            liveData.removeObservers(getViewLifecycleOwner());
        }
        liveData = medicineViewModel.getReminderEvents(0, Instant.now().toEpochMilli() / 1000 - (eventAgeHours * 60 * 60));
        liveData.observe(getViewLifecycleOwner(), adapter::submitList);

        ReminderProcessor.requestReschedule(requireContext());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (thread != null) {
            thread.quitSafely();
        }
        if (nextReminderListener != null) {
            requireContext().unregisterReceiver(nextReminderListener);
            nextReminderListener.stop();
        }
    }
}