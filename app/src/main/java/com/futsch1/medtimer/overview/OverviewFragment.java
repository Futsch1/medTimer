package com.futsch1.medtimer.overview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
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
import com.google.android.material.color.MaterialColors;
import com.google.android.material.divider.MaterialDivider;

import java.time.Instant;
import java.util.List;

public class OverviewFragment extends Fragment {

    private View fragmentOverview;
    private MedicineViewModel medicineViewModel;
    private LatestRemindersViewAdapter adapter;
    private LiveData<List<ReminderEvent>> liveData;
    private HandlerThread thread;
    private Chip showOnlyOpen;
    private OptionsMenu optionsMenu = null;

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
        setupSwipeDelete(latestReminders);
        setupFilterButton();
        setupResizeHandle();

        setDividerPercentage(PreferenceManager.getDefaultSharedPreferences(fragmentOverview.getContext()).getFloat("overview_divider_percentage", 0.3f));

        optionsMenu = new OptionsMenu(this,
                medicineViewModel,
                fragmentOverview, false);
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

    private void setupSwipeDelete(RecyclerView latestReminders) {
        SwipeHelper.createLeftSwipeTouchHelper(requireContext(), viewHolder -> deleteReminderEvent(requireContext(), viewHolder.getItemId(), viewHolder.getBindingAdapterPosition()))
                .attachToRecyclerView(latestReminders);
    }

    private void setupFilterButton() {
        showOnlyOpen = fragmentOverview.findViewById(R.id.showOnlyOpen);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(fragmentOverview.getContext());
        showOnlyOpen.setChecked(sharedPref.getBoolean("show_only_open", false));
        showOnlyOpen.setOnClickListener(v -> updateFilter());
        updateFilter();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupResizeHandle() {
        MaterialDivider resizeHandle = fragmentOverview.findViewById(R.id.overviewDivider);
        if (resizeHandle != null) {
            resizeHandle.setOnTouchListener(touchDivider(resizeHandle));
            fragmentOverview.findViewById(R.id.dividerImageDown).setOnTouchListener(touchDivider(resizeHandle));
            fragmentOverview.findViewById(R.id.dividerImageUp).setOnTouchListener(touchDivider(resizeHandle));
        }
    }

    private void setDividerPercentage(float percentage) {
        Guideline guideline = fragmentOverview.findViewById(R.id.guideline);

        if (guideline != null) {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) guideline.getLayoutParams();
            params.guidePercent = percentage;
            guideline.setLayoutParams(params);
        }
    }

    private void deleteReminderEvent(Context context, long itemId, int adapterPosition) {
        DeleteHelper deleteHelper = new DeleteHelper(context);
        deleteHelper.deleteItem(R.string.are_you_sure_delete_reminder_event, () -> {
            final Handler threadHandler = new Handler(thread.getLooper());
            threadHandler.post(() -> {

                ReminderEvent reminderEvent = medicineViewModel.medicineRepository.getReminderEvent((int) itemId);
                if (reminderEvent != null) {
                    reminderEvent.status = ReminderEvent.ReminderStatus.DELETED;
                    medicineViewModel.medicineRepository.updateReminderEvent(reminderEvent);
                    final Handler mainHandler = new Handler(Looper.getMainLooper());
                    mainHandler.post(() -> adapter.notifyItemRangeChanged(adapterPosition, adapterPosition + 1));
                }
            });
        }, () -> adapter.notifyItemRangeChanged(adapterPosition, adapterPosition + 1));
    }

    private void updateFilter() {
        String filterString = showOnlyOpen.isChecked() ? "o" : "";
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(fragmentOverview.getContext());
        sharedPref.edit().putBoolean("show_only_open", showOnlyOpen.isChecked()).apply();
        adapter.getFilter().filter(filterString);
    }

    @SuppressLint("ClickableViewAccessibility")
    private View.OnTouchListener touchDivider(MaterialDivider resizeHandle) {
        ImageView dividerImageUp = fragmentOverview.findViewById(R.id.dividerImageUp);
        ImageView dividerImageDown = fragmentOverview.findViewById(R.id.dividerImageDown);
        int colorDown = MaterialColors.getColor(resizeHandle, com.google.android.material.R.attr.colorPrimary);
        ColorStateList colorStateListDown = ColorStateList.valueOf(colorDown);
        int colorUp = MaterialColors.getColor(resizeHandle, com.google.android.material.R.attr.colorOutlineVariant);
        ColorStateList colorStateListUp = ColorStateList.valueOf(colorUp);
        return (v, e) -> {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    resizeHandle.setDividerColor(colorDown);
                    dividerImageDown.setImageTintList(colorStateListDown);
                    dividerImageUp.setImageTintList(colorStateListDown);
                    break;
                case MotionEvent.ACTION_MOVE:
                    float percentage = getPercentage(e);
                    setDividerPercentage(percentage);
                    break;
                case MotionEvent.ACTION_UP:
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(fragmentOverview.getContext());
                    sharedPref.edit().putFloat("overview_divider_percentage", getPercentage(e)).apply();
                    resizeHandle.setDividerColor(colorUp);
                    dividerImageDown.setImageTintList(colorStateListUp);
                    dividerImageUp.setImageTintList(colorStateListUp);
                    break;
                default:
                    break;

            }
            return true;
        };
    }

    private float getPercentage(MotionEvent e) {
        float newY = e.getRawY();
        return Float.max(0.2f, Float.min(newY / getResources().getDisplayMetrics().heightPixels, 0.8f));
    }

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(fragmentOverview.getContext());
        long eventAgeHours = Long.parseLong(sharedPref.getString("overview_events", "24"));
        if (liveData != null) {
            liveData.removeObservers(getViewLifecycleOwner());
        }
        adapter.resetInitialized();
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
            thread.quit();
        }
        if (optionsMenu != null) {
            optionsMenu.onDestroy();
        }
    }
}