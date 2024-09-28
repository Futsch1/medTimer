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
import com.futsch1.medtimer.helpers.MedicineIcons;
import com.futsch1.medtimer.helpers.ReminderHelperKt;
import com.futsch1.medtimer.helpers.TimeHelper;
import com.futsch1.medtimer.helpers.ViewColorHelper;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MedicineViewHolder extends RecyclerView.ViewHolder {
    private final TextView medicineNameView;
    private final TextView remindersSummaryView;
    private final View holderItemView;

    private MedicineViewHolder(View holderItemView) {
        super(holderItemView);
        this.holderItemView = holderItemView;
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
            remindersSummaryView.setText(getRemindersSummary(activeReminders));
        }

        holderItemView.setOnClickListener(view -> navigateToEditFragment(medicineWithReminders));

        if (medicineWithReminders.medicine.useColor) {
            ViewColorHelper.setCardBackground((MaterialCardView) holderItemView, Arrays.asList(medicineNameView, remindersSummaryView), medicineWithReminders.medicine.color);
        } else {
            ViewColorHelper.setDefaultColors((MaterialCardView) holderItemView, Arrays.asList(medicineNameView, remindersSummaryView));
        }

        MedicineIcons.toImageView(holderItemView.findViewById(R.id.medicineIcon), medicineWithReminders.medicine.iconId);
    }

    private String getRemindersSummary(List<Reminder> reminders) {
        ArrayList<String> reminderTimes = new ArrayList<>();
        int[] timesInMinutes = reminders.stream().mapToInt(r -> r.timeInMinutes).sorted().toArray();
        for (int minute : timesInMinutes) {
            reminderTimes.add(TimeHelper.minutesToTimeString(holderItemView.getContext(), minute));
        }
        int len = reminders.size();
        return remindersSummaryView.getResources().getQuantityString(R.plurals.sum_reminders, len, len, String.join(", ", reminderTimes));

    }

    private void navigateToEditFragment(MedicineWithReminders medicineWithReminders) {
        NavController navController = Navigation.findNavController(holderItemView);
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
