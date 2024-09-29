package com.futsch1.medtimer.medicine;

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

    private MedicineViewHolder(View holderItemView) {
        super(holderItemView);
        medicineNameView = holderItemView.findViewById(R.id.medicineName);
        remindersSummaryView = holderItemView.findViewById(R.id.remindersSummary);
    }

    static MedicineViewHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_medicine, parent, false);
        return new MedicineViewHolder(view);
    }

    public void bind(MedicineWithReminders medicineWithReminders) {
        medicineNameView.setText(medicineWithReminders.medicine.name);
        List<Reminder> activeReminders = medicineWithReminders.reminders.stream().filter(ReminderHelperKt::isReminderActive).collect(Collectors.toList());
        if (activeReminders.isEmpty()) {
            if (medicineWithReminders.reminders.isEmpty()) {
                remindersSummaryView.setText(R.string.no_reminders);
            } else {
                remindersSummaryView.setText(R.string.inactive);
            }
        } else {
            remindersSummaryView.setText(SummaryHelperKt.remindersSummary(itemView.getContext(), activeReminders));
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
                medicineWithReminders.medicine.medicineId,
                medicineWithReminders.medicine.name,
                medicineWithReminders.medicine.useColor,
                medicineWithReminders.medicine.color,
                medicineWithReminders.medicine.notificationImportance,
                medicineWithReminders.medicine.iconId
        );
        navController.navigate(action);
    }
}
