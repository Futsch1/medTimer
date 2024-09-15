package com.futsch1.medtimer.overview;

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
import com.futsch1.medtimer.OptionsMenu;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.helpers.DeleteHelper;
import com.futsch1.medtimer.helpers.SwipeHelper;
import com.futsch1.medtimer.reminders.ReminderProcessor;
import com.google.android.material.chip.Chip;

import java.time.Instant;
import java.util.List;

public class OverviewFragment extends Fragment {

    private View fragmentOverview;
    private MedicineViewModel medicineViewModel;
    private LatestRemindersViewAdapter adapter;
    private LiveData<List<ReminderEvent>> liveData;
    private HandlerThread thread;
    private Chip showOnlyOpen;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fragmentOverview = inflater.inflate(R.layout.fragment_overview, container, false);
        medicineViewModel = new ViewModelProvider(this).get(MedicineViewModel.class);

        thread = new HandlerThread("LogManualDose");
        thread.start();

        RecyclerView latestReminders = setupLatestReminders();
        new NextReminders(fragmentOverview, this, medicineViewModel);
        setupLogManualDose();
        setupSwipeEdit(latestReminders);
        setupSwipeDelete(latestReminders);
        setupFilterButton();

        OptionsMenu optionsMenu = new OptionsMenu(this.requireContext(),
                medicineViewModel,
                this,
                fragmentOverview);
        requireActivity().addMenuProvider(optionsMenu, getViewLifecycleOwner());

        return fragmentOverview;
    }

    @NonNull
    private RecyclerView setupLatestReminders() {
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
        return latestReminders;
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
        SwipeHelper swipeHelperEdit = new SwipeHelper(requireContext(), ItemTouchHelper.RIGHT, 0xFF006400, android.R.drawable.ic_menu_edit) {
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
        SwipeHelper.createLeftSwipeTouchHelper(requireContext(), viewHolder -> deleteItem(requireContext(), viewHolder.getItemId(), viewHolder.getBindingAdapterPosition()))
                .attachToRecyclerView(latestReminders);
    }

    private void setupFilterButton() {
        showOnlyOpen = fragmentOverview.findViewById(R.id.showOnlyOpen);
        showOnlyOpen.setOnClickListener(v -> updateFilter());
    }

    private void navigateToEditEvent(long eventId) {
        NavController navController = Navigation.findNavController(fragmentOverview);
        ReminderEvent reminderEvent = medicineViewModel.medicineRepository.getReminderEvent((int) eventId);
        if (reminderEvent != null) {
            OverviewFragmentDirections.ActionOverviewFragmentToEditEventFragment action = OverviewFragmentDirections.actionOverviewFragmentToEditEventFragment(
                    reminderEvent.reminderEventId,
                    reminderEvent.amount,
                    reminderEvent.medicineName,
                    reminderEvent.remindedTimestamp,
                    reminderEvent.reminderId <= 0
            );
            requireActivity().runOnUiThread(() ->
                    navController.navigate(action));
        }
    }

    private void deleteItem(Context context, long itemId, int adapterPosition) {
        DeleteHelper deleteHelper = new DeleteHelper(context);
        deleteHelper.deleteItem(R.string.are_you_sure_delete_reminder_event, () -> {
            final Handler threadHandler = new Handler(thread.getLooper());
            threadHandler.post(() -> {

                ReminderEvent reminderEvent = medicineViewModel.getReminderEvent((int) itemId);
                if (reminderEvent != null) {
                    reminderEvent.status = ReminderEvent.ReminderStatus.DELETED;
                    medicineViewModel.updateReminderEvent(reminderEvent);
                    final Handler mainHandler = new Handler(Looper.getMainLooper());
                    mainHandler.post(() -> adapter.notifyItemRangeChanged(adapterPosition, adapterPosition + 1));
                }
            });
        }, () -> adapter.notifyItemRangeChanged(adapterPosition, adapterPosition + 1));
    }

    private void updateFilter() {
        String filterString = showOnlyOpen.isChecked() ? "o" : "";
        adapter.getFilter().filter(filterString);
    }

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(fragmentOverview.getContext());
        long eventAgeHours = Long.parseLong(sharedPref.getString("overview_events", "24"));
        if (liveData != null) {
            liveData.removeObservers(getViewLifecycleOwner());
        }
        liveData = medicineViewModel.getLiveReminderEvents(0, Instant.now().toEpochMilli() / 1000 - (eventAgeHours * 60 * 60), false);
        liveData.observe(getViewLifecycleOwner(), reminders -> {
            adapter.setData(reminders);
            updateFilter();
        });

        ReminderProcessor.requestReschedule(requireContext());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (thread != null) {
            thread.quitSafely();
        }
    }
}