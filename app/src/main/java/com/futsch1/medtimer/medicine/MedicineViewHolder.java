package com.futsch1.medtimer.medicine;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.MainFragmentDirections;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.helpers.TimeHelper;
import com.futsch1.medtimer.helpers.ViewColorHelper;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Arrays;

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

    public void bind(MedicineWithReminders medicineWithReminders, DeleteCallback deleteCallback) {
        medicineNameView.setText(medicineWithReminders.medicine.name);
        if (medicineWithReminders.reminders.isEmpty()) {
            remindersSummaryView.setText(R.string.no_reminders);
        } else {
            remindersSummaryView.setText(getRemindersSummary(medicineWithReminders));
        }

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.holderItemView.getContext());

        holderItemView.setOnLongClickListener(v -> {
            if (sharedPref.getString("delete_items", "0").equals("0")) {
                return false;
            }
            PopupMenu popupMenu = new PopupMenu(holderItemView.getContext(), this.holderItemView);
            popupMenu.getMenuInflater().inflate(R.menu.edit_delete_popup, popupMenu.getMenu());
            popupMenu.getMenu().findItem(R.id.edit).setOnMenuItemClickListener(item -> {
                navigateToEditFragment(medicineWithReminders);
                return true;
            });
            popupMenu.getMenu().findItem(R.id.delete).setOnMenuItemClickListener(item -> {
                deleteCallback.deleteItem(holderItemView.getContext(), getItemId(), getAdapterPosition());
                return true;
            });
            popupMenu.show();
            return true;
        });

        holderItemView.setOnClickListener(view -> navigateToEditFragment(medicineWithReminders));

        if (medicineWithReminders.medicine.useColor) {
            ViewColorHelper.setCardBackground((MaterialCardView) holderItemView, Arrays.asList(medicineNameView, remindersSummaryView), medicineWithReminders.medicine.color);
        } else {
            ViewColorHelper.setDefaultColors((MaterialCardView) holderItemView, Arrays.asList(medicineNameView, remindersSummaryView));
        }
    }

    private String getRemindersSummary(MedicineWithReminders medicineWithReminders) {
        ArrayList<String> reminderTimes = new ArrayList<>();
        int[] timesInMinutes = medicineWithReminders.reminders.stream().mapToInt(r -> r.timeInMinutes).sorted().toArray();
        for (int minute : timesInMinutes) {
            reminderTimes.add(TimeHelper.minutesToTime(minute));
        }
        int len = medicineWithReminders.reminders.size();
        return remindersSummaryView.getResources().getQuantityString(R.plurals.sum_reminders, len, len, String.join(", ", reminderTimes));

    }

    private void navigateToEditFragment(MedicineWithReminders medicineWithReminders) {
        NavController navController = Navigation.findNavController(holderItemView);
        MainFragmentDirections.ActionFragmentMainToEditMedicine action = MainFragmentDirections.actionFragmentMainToEditMedicine(
                medicineWithReminders.medicine.medicineId,
                medicineWithReminders.medicine.name,
                medicineWithReminders.medicine.useColor,
                medicineWithReminders.medicine.color
        );
        navController.navigate(action);
    }

    public interface DeleteCallback {
        void deleteItem(Context context, long itemId, int adapterPosition);
    }
}
