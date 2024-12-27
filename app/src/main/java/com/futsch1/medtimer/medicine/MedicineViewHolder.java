package com.futsch1.medtimer.medicine;

import android.app.Activity;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
import com.google.android.material.card.MaterialCardView;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MedicineViewHolder extends RecyclerView.ViewHolder {
    private final TextView medicineNameView;
    private final TextView remindersSummaryView;
    private final HandlerThread thread;
    private final Activity activity;

    private MedicineViewHolder(View holderItemView, Activity activity, HandlerThread thread) {
        super(holderItemView);
        medicineNameView = holderItemView.findViewById(R.id.medicineName);
        remindersSummaryView = holderItemView.findViewById(R.id.remindersSummary);
        this.thread = thread;
        this.activity = activity;
    }

    static MedicineViewHolder create(ViewGroup parent, Activity activity, HandlerThread thread) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_medicine, parent, false);
        return new MedicineViewHolder(view, activity, thread);
    }

    public void bind(MedicineWithReminders medicineWithReminders) {
        medicineNameView.setText(MedicineHelper.getMedicineNameWithStockText(itemView.getContext(), medicineWithReminders.medicine));
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

        itemView.setOnClickListener(view -> navigateToEditFragment(medicineWithReminders));

        if (medicineWithReminders.medicine.useColor) {
            ViewColorHelper.setCardBackground((MaterialCardView) itemView, Arrays.asList(medicineNameView, remindersSummaryView), medicineWithReminders.medicine.color);
        } else {
            ViewColorHelper.setDefaultColors((MaterialCardView) itemView, Arrays.asList(medicineNameView, remindersSummaryView));
        }

        ViewColorHelper.setIconToImageView((MaterialCardView) itemView, itemView.findViewById(R.id.medicineIcon), medicineWithReminders.medicine.iconId);
    }

    private void navigateToEditFragment(MedicineWithReminders medicineWithReminders) {
        NavController navController = Navigation.findNavController(itemView);
        MedicinesFragmentDirections.ActionMedicinesFragmentToEditMedicineFragment action = MedicinesFragmentDirections.actionMedicinesFragmentToEditMedicineFragment(
                medicineWithReminders.medicine.medicineId
        );
        navController.navigate(action);
    }
}
