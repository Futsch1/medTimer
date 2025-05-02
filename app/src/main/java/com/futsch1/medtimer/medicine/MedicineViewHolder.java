package com.futsch1.medtimer.medicine;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.FullMedicine;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.Tag;
import com.futsch1.medtimer.helpers.MedicineHelper;
import com.futsch1.medtimer.helpers.ReminderHelperKt;
import com.futsch1.medtimer.helpers.SummaryHelperKt;
import com.futsch1.medtimer.helpers.ViewColorHelper;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.chip.Chip;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MedicineViewHolder extends RecyclerView.ViewHolder {
    private final TextView medicineNameView;
    private final TextView remindersSummaryView;
    private final FlexboxLayout tags;
    private final HandlerThread thread;
    private final FragmentActivity activity;

    private MedicineViewHolder(View holderItemView, FragmentActivity activity, HandlerThread thread) {
        super(holderItemView);
        medicineNameView = holderItemView.findViewById(R.id.medicineName);
        remindersSummaryView = holderItemView.findViewById(R.id.remindersSummary);
        tags = holderItemView.findViewById(R.id.tags);
        this.thread = thread;
        this.activity = activity;
    }

    static MedicineViewHolder create(ViewGroup parent, FragmentActivity activity, HandlerThread thread) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_medicine, parent, false);
        return new MedicineViewHolder(view, activity, thread);
    }

    public void bind(FullMedicine medicine) {
        medicineNameView.setText(MedicineHelper.getMedicineNameWithStockText(itemView.getContext(), medicine.medicine));
        setupSummary(medicine);

        itemView.setOnClickListener(view -> navigateToEditFragment(medicine));

        if (medicine.medicine.useColor) {
            ViewColorHelper.setViewBackground(itemView, Arrays.asList(medicineNameView, remindersSummaryView), medicine.medicine.color);
        } else {
            ViewColorHelper.setDefaultColors(itemView, Arrays.asList(medicineNameView, remindersSummaryView));
        }

        ViewColorHelper.setIconToImageView(itemView, itemView.findViewById(R.id.medicineIcon), medicine.medicine.iconId);

        buildTags(medicine.tags);
    }

    private void setupSummary(FullMedicine medicine) {
        List<Reminder> activeReminders = medicine.reminders.stream().filter(ReminderHelperKt::isReminderActive).collect(Collectors.toList());
        if (activeReminders.isEmpty()) {
            if (medicine.reminders.isEmpty()) {
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

    private void navigateToEditFragment(FullMedicine medicine) {
        NavController navController = Navigation.findNavController(itemView);
        MedicinesFragmentDirections.ActionMedicinesFragmentToEditMedicineFragment action = MedicinesFragmentDirections.actionMedicinesFragmentToEditMedicineFragment(
                medicine.medicine.medicineId
        );
        try {
            navController.navigate(action);
        } catch (IllegalArgumentException e) {
            // Ignore
        }

    }

    void buildTags(List<Tag> tagsList) {
        tags.removeAllViews();
        for (Tag tag : tagsList) {
            @SuppressLint("InflateParams")
            Chip chip = (Chip) LayoutInflater.from(itemView.getContext()).inflate(R.layout.tag, null);
            chip.setText(tag.name);
            chip.setChecked(true);
            chip.setCheckable(false);
            chip.setCloseIconVisible(false);
            chip.setOnClickListener(v -> itemView.performClick());
            chip.setRippleColor(ColorStateList.valueOf(Color.TRANSPARENT));
            FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            int margin = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 4, this.itemView.getResources().getDisplayMetrics()
            );
            params.setMargins(margin, 0, margin, 0);
            chip.setLayoutParams(params);
            tags.addView(chip);
        }
    }
}
