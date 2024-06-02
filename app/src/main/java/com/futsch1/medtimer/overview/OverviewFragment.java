package com.futsch1.medtimer.overview;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.futsch1.medtimer.helpers.DeleteHelper;
import com.futsch1.medtimer.helpers.SwipeHelper;
import com.futsch1.medtimer.reminders.ReminderProcessor;
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class OverviewFragment extends Fragment {

    private View fragmentOverview;
    private MedicineViewModel medicineViewModel;
    private LatestRemindersViewAdapter latestRemindersViewAdapter;
    private NextRemindersViewAdapter nextRemindersViewAdapter;
    private LiveData<List<ReminderEvent>> liveData;
    private HandlerThread thread;
    private SwipeHelper swipeHelperEdit;
    private SwipeHelper swipeHelperDelete;
    private NextRemindersViewModel nextRemindersViewModel;
    private List<ReminderEvent> reminderEvents;
    private List<MedicineWithReminders> medicineWithReminders;
    private boolean nextRemindersExpanded = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fragmentOverview = inflater.inflate(R.layout.fragment_overview, container, false);
        medicineViewModel = new ViewModelProvider(this).get(MedicineViewModel.class);

        thread = new HandlerThread("LogManualDose");
        thread.start();

        RecyclerView latestReminders = setupLatestReminders();
        setupNextReminders();
        setupLogManualDose();
        setupSwipeEdit(latestReminders);
        setupSwipeDelete(latestReminders);
        setupExpandNextReminders();

        return fragmentOverview;
    }

    @NonNull
    private RecyclerView setupLatestReminders() {
        RecyclerView latestReminders = fragmentOverview.findViewById(R.id.latestReminders);
        latestRemindersViewAdapter = new LatestRemindersViewAdapter(new LatestRemindersViewAdapter.ReminderEventDiff());
        latestReminders.setAdapter(latestRemindersViewAdapter);
        latestReminders.setLayoutManager(new LinearLayoutManager(fragmentOverview.getContext()));
        // Scroll to top as soon as a new item was inserted
        latestRemindersViewAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                latestReminders.scrollToPosition(0);
            }
        });
        return latestReminders;
    }

    private void setupNextReminders() {
        RecyclerView nextReminders = fragmentOverview.findViewById(R.id.nextReminders);
        nextRemindersViewAdapter = new NextRemindersViewAdapter(new NextRemindersViewAdapter.ScheduledReminderDiff(), medicineViewModel);
        nextReminders.setAdapter(nextRemindersViewAdapter);
        nextReminders.setLayoutManager(new LinearLayoutManager(fragmentOverview.getContext()));
        nextRemindersViewAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                nextReminders.scrollToPosition(0);
            }
        });

        nextRemindersViewModel = new ViewModelProvider(this).get(NextRemindersViewModel.class);
        nextRemindersViewModel.getScheduledReminders().observe(getViewLifecycleOwner(), this::updatedNextReminders);

        setupScheduleObservers();
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

    private void setupSwipeEdit(RecyclerView latestReminders) {
        swipeHelperEdit = new SwipeHelper(requireContext(), ItemTouchHelper.RIGHT, 0xFF006400, android.R.drawable.ic_menu_edit, null) {
            @Override
            public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder, int direction) {
                if (direction == ItemTouchHelper.RIGHT) {
                    Handler handler = new Handler(thread.getLooper());
                    handler.post(() -> navigateToEditEvent(viewHolder.getItemId()));
                }
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeHelperEdit);
        itemTouchHelper.attachToRecyclerView(latestReminders);
    }

    private void setupSwipeDelete(RecyclerView latestReminders) {
        swipeHelperDelete = new SwipeHelper(requireContext(), ItemTouchHelper.LEFT, 0xFF8B0000, android.R.drawable.ic_menu_delete, null) {
            @Override
            public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder, int direction) {
                if (direction == ItemTouchHelper.LEFT) {
                    deleteItem(fragmentOverview.getContext(), viewHolder.getItemId(), viewHolder.getAdapterPosition());
                }
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeHelperDelete);
        itemTouchHelper.attachToRecyclerView(latestReminders);
    }

    private void setupExpandNextReminders() {
        MaterialButton expandNextReminders = fragmentOverview.findViewById(R.id.expandNextReminders);
        MaterialCardView nextRemindersCard = fragmentOverview.findViewById(R.id.nextRemindersCard);
        expandNextReminders.setOnClickListener(v -> {
            nextRemindersExpanded = !nextRemindersExpanded;
            adaptUIToNextRemindersExpandedState(expandNextReminders, nextRemindersCard);
            updatedNextReminders(nextRemindersViewModel.getScheduledReminders().getValue());
        });

        adaptUIToNextRemindersExpandedState(expandNextReminders, nextRemindersCard);
    }

    private void updatedNextReminders(@Nullable List<ScheduledReminder> scheduledReminders) {
        if (scheduledReminders == null || scheduledReminders.isEmpty()) {
            fragmentOverview.findViewById(R.id.expandNextReminders).setVisibility(View.GONE);

            nextRemindersViewAdapter.submitList(new ArrayList<>());
        } else {
            fragmentOverview.findViewById(R.id.expandNextReminders).setVisibility(View.VISIBLE);

            nextRemindersViewAdapter.submitList(nextRemindersExpanded ? scheduledReminders : scheduledReminders.subList(0, 1));
        }
    }

    private void setupScheduleObservers() {
        medicineViewModel.getReminderEvents(0, Instant.now().toEpochMilli() / 1000 - 48 * 60 * 60, true)
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

    private void deleteItem(Context context, long itemId, int adapterPosition) {
        DeleteHelper<LatestRemindersViewHolder> deleteHelper = new DeleteHelper<>(context, thread, latestRemindersViewAdapter);
        deleteHelper.deleteItem(adapterPosition, R.string.are_you_sure_delete_reminder_event, () -> {
            ReminderEvent reminderEvent = medicineViewModel.getReminderEvent((int) itemId);
            reminderEvent.status = ReminderEvent.ReminderStatus.DELETED;
            medicineViewModel.updateReminderEvent(reminderEvent);
            final Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> latestRemindersViewAdapter.notifyItemRangeChanged(adapterPosition, adapterPosition + 1));
        });
    }

    private void adaptUIToNextRemindersExpandedState(MaterialButton expandNextReminders, MaterialCardView nextRemindersCard) {
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) nextRemindersCard.getLayoutParams();
        if (nextRemindersExpanded) {
            expandNextReminders.setIconResource(R.drawable.chevron_up);
            layoutParams.height = 0;
            layoutParams.weight = 1;
            nextRemindersCard.setLayoutParams(layoutParams);
        } else {
            expandNextReminders.setIconResource(R.drawable.chevron_down);
            layoutParams.height = WRAP_CONTENT;
            layoutParams.weight = 0;
            nextRemindersCard.setLayoutParams(layoutParams);
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
        liveData = medicineViewModel.getReminderEvents(0, Instant.now().toEpochMilli() / 1000 - (eventAgeHours * 60 * 60), false);
        liveData.observe(getViewLifecycleOwner(), latestRemindersViewAdapter::submitList);

        ReminderProcessor.requestReschedule(requireContext());

        swipeHelperEdit.setup(requireContext());
        swipeHelperDelete.setup(requireContext());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (thread != null) {
            thread.quitSafely();
        }
    }
}