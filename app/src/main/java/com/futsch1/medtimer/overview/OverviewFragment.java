package com.futsch1.medtimer.overview;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.MedicineViewModel;
import com.futsch1.medtimer.NextRemindersViewModel;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.ScheduledReminder;
import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.helpers.SwipeHelper;
import com.futsch1.medtimer.reminders.ReminderProcessor;
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class OverviewFragment extends Fragment {

    private View fragmentOverview;
    private MedicineViewModel medicineViewModel;
    private LatestRemindersViewAdapter adapter;
    private LiveData<List<ReminderEvent>> liveData;
    private HandlerThread thread;
    private SwipeHelper swipeHelper;
    private NextRemindersViewModel nextRemindersViewModel;
    private List<ReminderEvent> reminderEvents;
    private List<MedicineWithReminders> medicineWithReminders;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fragmentOverview = inflater.inflate(R.layout.fragment_overview, container, false);
        medicineViewModel = new ViewModelProvider(this).get(MedicineViewModel.class);

        setupTakenSkippedButtonsAndNextReminderListener();

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

        setupSwiping(latestReminders);

        return fragmentOverview;
    }

    private void setupTakenSkippedButtonsAndNextReminderListener() {
        NextReminderListener nextReminderListener;
        Button takenNow = fragmentOverview.findViewById(R.id.takenNow);
        Button skippedNow = fragmentOverview.findViewById(R.id.skippedNow);
        NextReminderListener.NextReminderIsTodayCallback callback = isToday -> {
            final Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> {
                takenNow.setVisibility(isToday ? View.VISIBLE : View.GONE);
                skippedNow.setVisibility(isToday ? View.VISIBLE : View.GONE);
            });
        };

        nextRemindersViewModel = new ViewModelProvider(this).get(NextRemindersViewModel.class);
        nextReminderListener = new NextReminderListener(fragmentOverview.findViewById(R.id.nextReminderInfo), callback, medicineViewModel);
        nextRemindersViewModel.getScheduledReminders().observe(getViewLifecycleOwner(), nextReminderListener::setScheduledReminders);

        setupScheduleObservers();

        takenNow.setOnClickListener(buttonView -> nextReminderListener.processFutureReminder(true));
        skippedNow.setOnClickListener(buttonView -> nextReminderListener.processFutureReminder(false));
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

    private void setupSwiping(RecyclerView latestReminders) {
        swipeHelper = new SwipeHelper(requireContext(), ItemTouchHelper.RIGHT, 0xFF006400, android.R.drawable.ic_menu_edit, null) {
            @Override
            public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder, int direction) {
                if (direction == ItemTouchHelper.RIGHT) {
                    Handler handler = new Handler(thread.getLooper());
                    handler.post(() -> navigateToEditEvent(viewHolder.getItemId()));
                }
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeHelper);
        itemTouchHelper.attachToRecyclerView(latestReminders);

    }

    private void setupScheduleObservers() {
        medicineViewModel.getReminderEvents(0, Instant.now().toEpochMilli() / 1000 - 48 * 60 * 60)
                .observe(getViewLifecycleOwner(), this::changedReminderEvents);
        medicineViewModel.getMedicines().observe(getViewLifecycleOwner(), this::changedMedicines);
    }

    private void navigateToEditEvent(long eventId) {
        NavController navController = Navigation.findNavController(fragmentOverview);
        ReminderEvent reminderEvent = medicineViewModel.medicineRepository.getReminderEvent((int) eventId);
        if (reminderEvent != null) {
            OverviewFragmentDirections.ActionOverviewFragmentToEditEventFragment action = OverviewFragmentDirections.actionOverviewFragmentToEditEventFragment(
                    reminderEvent.reminderEventId,
                    reminderEvent.amount,
                    reminderEvent.medicineName,
                    reminderEvent.remindedTimestamp
            );
            requireActivity().runOnUiThread(() ->
                    navController.navigate(action));
        }
    }

    private void changedReminderEvents(List<ReminderEvent> reminderEvents) {
        this.reminderEvents = reminderEvents;
        calculateSchedule();
    }

    private void changedMedicines(List<MedicineWithReminders> medicineWithReminders) {
        this.medicineWithReminders = medicineWithReminders;
        calculateSchedule();
    }

    private void calculateSchedule() {
        ReminderScheduler scheduler = new ReminderScheduler(new ReminderScheduler.TimeAccess() {
            @Override
            public ZoneId systemZone() {
                return ZoneId.systemDefault();
            }

            @Override
            public LocalDate localDate() {
                return LocalDate.now();
            }
        });

        if (medicineWithReminders != null && reminderEvents != null) {
            List<ScheduledReminder> reminders = scheduler.schedule(medicineWithReminders, reminderEvents);
            nextRemindersViewModel.setScheduledReminders(reminders);
        }
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

        swipeHelper.setup(requireContext());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (thread != null) {
            thread.quitSafely();
        }
    }
}