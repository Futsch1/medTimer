package com.futsch1.medtimer.medicine;

import static android.view.MotionEvent.ACTION_UP;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.helpers.MedicineHelper;
import com.futsch1.medtimer.helpers.ReminderHelperKt;
import com.futsch1.medtimer.helpers.SummaryHelperKt;
import com.futsch1.medtimer.helpers.ViewColorHelper;
import com.futsch1.medtimer.medicine.tags.MedicineWithTagsViewModel;
import com.futsch1.medtimer.medicine.tags.TagWithStateCollector;
import com.futsch1.medtimer.medicine.tags.TagsAdapter;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.material.card.MaterialCardView;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import kotlin.Unit;

public class MedicineViewHolder extends RecyclerView.ViewHolder {
    private final TextView medicineNameView;
    private final TextView remindersSummaryView;
    private final RecyclerView tags;
    private final HandlerThread thread;
    private final Activity activity;

    private MedicineViewHolder(View holderItemView, Activity activity, HandlerThread thread) {
        super(holderItemView);
        medicineNameView = holderItemView.findViewById(R.id.medicineName);
        remindersSummaryView = holderItemView.findViewById(R.id.remindersSummary);
        tags = holderItemView.findViewById(R.id.tags);
        this.thread = thread;
        this.activity = activity;
    }

    static MedicineViewHolder create(ViewGroup parent, Activity activity, HandlerThread thread) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_medicine, parent, false);
        return new MedicineViewHolder(view, activity, thread);
    }

    public void bind(MedicineWithReminders medicineWithReminders, LifecycleOwner lifecycleOwner) {
        medicineNameView.setText(MedicineHelper.getMedicineNameWithStockText(itemView.getContext(), medicineWithReminders.medicine));
        setupSummary(medicineWithReminders);

        itemView.setOnClickListener(view -> navigateToEditFragment(medicineWithReminders));

        if (medicineWithReminders.medicine.useColor) {
            ViewColorHelper.setCardBackground((MaterialCardView) itemView, Arrays.asList(medicineNameView, remindersSummaryView), medicineWithReminders.medicine.color);
        } else {
            ViewColorHelper.setDefaultColors((MaterialCardView) itemView, Arrays.asList(medicineNameView, remindersSummaryView));
        }

        ViewColorHelper.setIconToImageView((MaterialCardView) itemView, itemView.findViewById(R.id.medicineIcon), medicineWithReminders.medicine.iconId);

        setupTags(medicineWithReminders.medicine.medicineId, lifecycleOwner);
    }

    private void setupSummary(MedicineWithReminders medicineWithReminders) {
        List<Reminder> activeReminders = medicineWithReminders.reminders.stream().filter(ReminderHelperKt::isReminderActive).collect(Collectors.toList());
        if (activeReminders.isEmpty()) {
            if (medicineWithReminders.reminders.isEmpty()) {
                remindersSummaryView.setText(R.string.no_reminders);
            } else {
                remindersSummaryView.setText(R.string.inactive);
            }
        } else {
            new Handler(thread.getLooper()).post(() -> {
                String summary = SummaryHelperKt.remindersSummary(itemView.getContext(), activeReminders);
                this.activity.runOnUiThread(() ->
                        remindersSummaryView.setText(summary));
            });
        }
    }

    private void navigateToEditFragment(MedicineWithReminders medicineWithReminders) {
        NavController navController = Navigation.findNavController(itemView);
        MedicinesFragmentDirections.ActionMedicinesFragmentToEditMedicineFragment action = MedicinesFragmentDirections.actionMedicinesFragmentToEditMedicineFragment(
                medicineWithReminders.medicine.medicineId
        );
        try {
            navController.navigate(action);
        } catch (IllegalArgumentException e) {
            // Ignore
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupTags(int medicineId, LifecycleOwner lifecycleOwner) {
        MedicineWithTagsViewModel medicineWithTagsViewModel = new ViewModelProvider((ViewModelStoreOwner) activity,
                new MedicineWithTagsViewModel.Factory(activity.getApplication(), medicineId)).get(MedicineWithTagsViewModel.class);
        TagsAdapter tagAdapter = new TagsAdapter(medicineWithTagsViewModel);
        TagWithStateCollector collector = new TagWithStateCollector(tagAdapter, () -> Unit.INSTANCE).allTags(false);
        tags.setAdapter(tagAdapter);
        tags.setLayoutManager(new FlexboxLayoutManager(itemView.getContext()));
        // Pass click events to the itemView
        tags.setOnTouchListener((v, e) -> {
                    if (e.getAction() == ACTION_UP) {
                        itemView.performClick();
                    }
                    return false;
                }
        );
        tags.setOnClickListener(v -> itemView.performClick());

        medicineWithTagsViewModel.getTags().observe(lifecycleOwner, collector::setTags);
        medicineWithTagsViewModel.getMedicineWithTags().observe(lifecycleOwner, collector::setMedicineWithTags);
    }
}
