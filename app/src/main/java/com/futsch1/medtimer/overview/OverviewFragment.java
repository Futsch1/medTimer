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
    private LatestRemindersViewAdapter latestRemindersViewAdapter;
    private LiveData<List<ReminderEvent>> liveData;
    private HandlerThread thread;
    private SwipeHelper swipeHelperEdit;
    private SwipeHelper swipeHelperDelete;
    private Chip showTaken;
    private Chip showSkipped;

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
        setupFilterButtons();

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
                    deleteItem(fragmentOverview.getContext(), viewHolder.getItemId(), viewHolder.getBindingAdapterPosition());
                }
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeHelperDelete);
        itemTouchHelper.attachToRecyclerView(latestReminders);
    }

    private void setupFilterButtons() {
        showTaken = fragmentOverview.findViewById(R.id.showTaken);
        showSkipped = fragmentOverview.findViewById(R.id.showSkipped);
        showTaken.setOnClickListener(v -> updateFilter());
        showSkipped.setOnClickListener(v -> updateFilter());
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

    private void updateFilter() {
        String filterString = "";
        if (showTaken.isChecked()) {
            filterString += "t";
        }
        if (showSkipped.isChecked()) {
            filterString += "s";
        }
        latestRemindersViewAdapter.getFilter().filter(filterString);
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
        liveData.observe(getViewLifecycleOwner(), reminders -> {
            latestRemindersViewAdapter.setData(reminders);
            updateFilter();
        });

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