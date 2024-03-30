package com.futsch1.medtimer.overview;

import static com.futsch1.medtimer.ActivityCodes.NEXT_REMINDER_ACTION;

import android.app.AlertDialog;
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
import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.helpers.DialogHelper;

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

        // Adding a manual dose needs to be done in a background thread since the database is accessed
        thread = new HandlerThread("SetupLogManualDose");
        thread.start();
        Handler handler = new Handler(thread.getLooper());
        handler.post(this::setupLogManualDose);

        return fragmentOverview;
    }

    private void setupLogManualDose() {
        List<MedicineWithReminders> medicines = medicineViewModel.medicineRepository.getMedicines();
        CharSequence[] names = new CharSequence[medicines.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = medicines.get(i).medicine.name;
        }
        Button logManualDose = fragmentOverview.findViewById(R.id.logManualDose);
        logManualDose.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setItems(names, (dialog, which) -> logManualDose(medicines.get(which).medicine))
                .setTitle(R.string.tab_medicine)
                .show());
    }

    private void logManualDose(Medicine medicine) {
        ReminderEvent reminderEvent = new ReminderEvent();
        // Manual dose is not assigned to an existing reminder
        reminderEvent.reminderId = -1;
        reminderEvent.remindedTimestamp = Instant.now().toEpochMilli() / 1000;
        reminderEvent.processedTimestamp = reminderEvent.remindedTimestamp;
        reminderEvent.medicineName = medicine.name;
        reminderEvent.color = medicine.color;
        reminderEvent.useColor = medicine.useColor;
        reminderEvent.status = ReminderEvent.ReminderStatus.TAKEN;
        DialogHelper.showTextInputDialog(requireContext(), R.string.log_manual_dose, R.string.dosage, amount -> {
            reminderEvent.amount = amount;
            medicineViewModel.medicineRepository.insertReminderEvent(reminderEvent);
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        requireContext().unregisterReceiver(nextReminderListener);
        thread.quitSafely();
        nextReminderListener.stop();
    }
}