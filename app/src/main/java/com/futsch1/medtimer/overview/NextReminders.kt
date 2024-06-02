package com.futsch1.medtimer.overview;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.MedicineViewModel;
import com.futsch1.medtimer.NextRemindersViewModel;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.ScheduledReminder;
import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class NextReminders {
    private final NextRemindersViewModel nextRemindersViewModel;
    private final NextRemindersViewAdapter nextRemindersViewAdapter;
    private final MedicineViewModel medicineViewModel;
    private final MaterialButton expandNextReminders;
    private List<ReminderEvent> reminderEvents;
    private boolean nextRemindersExpanded = false;
    private List<MedicineWithReminders> medicineWithReminders;

    @SuppressLint("WrongViewCast")
    public NextReminders(View fragmentView, Fragment parentFragment, MedicineViewModel medicineViewModel) {
        this.medicineViewModel = medicineViewModel;
        expandNextReminders = fragmentView.findViewById(R.id.expandNextReminders);

        nextRemindersViewAdapter = new NextRemindersViewAdapter(new NextRemindersViewAdapter.ScheduledReminderDiff(), medicineViewModel);

        RecyclerView recyclerView = fragmentView.findViewById(R.id.nextReminders);
        recyclerView.setAdapter(nextRemindersViewAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        nextRemindersViewAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                recyclerView.scrollToPosition(0);
            }
        });

        nextRemindersViewModel = new ViewModelProvider(parentFragment).get(NextRemindersViewModel.class);
        nextRemindersViewModel.getScheduledReminders().observe(parentFragment.getViewLifecycleOwner(), this::updatedNextReminders);

        setupScheduleObservers(parentFragment);
        setupExpandNextReminders(fragmentView);
    }

    private void updatedNextReminders(@Nullable List<ScheduledReminder> scheduledReminders) {
        if (scheduledReminders == null || scheduledReminders.isEmpty()) {
            expandNextReminders.setVisibility(View.GONE);

            nextRemindersViewAdapter.submitList(new ArrayList<>());
        } else {
            expandNextReminders.setVisibility(View.VISIBLE);

            nextRemindersViewAdapter.submitList(nextRemindersExpanded ? scheduledReminders : scheduledReminders.subList(0, 1));
        }
    }

    private void setupScheduleObservers(Fragment parentFragment) {
        medicineViewModel.getReminderEvents(0, Instant.now().toEpochMilli() / 1000 - 48 * 60 * 60, true)
                .observe(parentFragment.getViewLifecycleOwner(), this::changedReminderEvents);
        medicineViewModel.getMedicines().observe(parentFragment.getViewLifecycleOwner(), this::changedMedicines);
    }

    private void setupExpandNextReminders(View fragmentView) {
        MaterialCardView nextRemindersCard = fragmentView.findViewById(R.id.nextRemindersCard);
        expandNextReminders.setOnClickListener(v -> {
            nextRemindersExpanded = !nextRemindersExpanded;
            adaptUIToNextRemindersExpandedState(expandNextReminders, nextRemindersCard);
            updatedNextReminders(nextRemindersViewModel.getScheduledReminders().getValue());
        });

        adaptUIToNextRemindersExpandedState(expandNextReminders, nextRemindersCard);
    }

    private void changedReminderEvents(List<ReminderEvent> reminderEvents) {
        this.reminderEvents = reminderEvents;
        calculateSchedule();
    }

    private void changedMedicines(List<MedicineWithReminders> medicineWithReminders) {
        this.medicineWithReminders = medicineWithReminders;
        calculateSchedule();
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
}
