package com.futsch1.medtimer.medicine;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_COLOR;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_MEDICINE_NAME;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_USE_COLOR;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.helpers.TimeHelper;
import com.futsch1.medtimer.helpers.ViewColorHelper;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;

public class MedicineViewHolder extends RecyclerView.ViewHolder {
    private final TextView medicineNameView;
    private final TextView remindersSummaryView;
    private final View itemView;

    private MedicineViewHolder(View itemView) {
        super(itemView);
        this.itemView = itemView;
        medicineNameView = itemView.findViewById(R.id.medicineName);
        remindersSummaryView = itemView.findViewById(R.id.remindersSummary);
    }

    static MedicineViewHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_medicine, parent, false);
        return new MedicineViewHolder(view);
    }

    public void bind(MedicineWithReminders medicineWithReminders, DeleteCallback deleteCallback) {
        medicineNameView.setText(medicineWithReminders.medicine.name);
        int len = medicineWithReminders.reminders.size();
        if (len == 0) {
            remindersSummaryView.setText(R.string.no_reminders);
        } else {
            ArrayList<String> reminderTimes = new ArrayList<>();
            int[] timesInMinutes = medicineWithReminders.reminders.stream().mapToInt((r) -> r.timeInMinutes).sorted().toArray();
            for (int minute : timesInMinutes) {
                reminderTimes.add(TimeHelper.minutesToTime(minute));
            }
            String reminders = remindersSummaryView.getResources().getQuantityString(R.plurals.reminders_per_day, len, len, String.join(", ", reminderTimes));
            remindersSummaryView.setText(reminders);
        }

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.itemView.getContext());

        itemView.setOnLongClickListener(v -> {
            if (sharedPref.getString("delete_items", "0").equals("0")) {
                return false;
            }
            PopupMenu popupMenu = new PopupMenu(itemView.getContext(), this.itemView);
            popupMenu.getMenuInflater().inflate(R.menu.edit_delete_popup, popupMenu.getMenu());
            popupMenu.getMenu().findItem(R.id.edit).setOnMenuItemClickListener(item -> {
                startEditActivity(medicineWithReminders);
                return true;
            });
            popupMenu.getMenu().findItem(R.id.delete).setOnMenuItemClickListener(item -> {
                deleteCallback.deleteItem(itemView.getContext(), getItemId(), getAdapterPosition());
                return true;
            });
            popupMenu.show();
            return true;
        });


        itemView.setOnClickListener(view -> startEditActivity(medicineWithReminders));

        if (medicineWithReminders.medicine.useColor) {
            ViewColorHelper.setCardBackground((MaterialCardView) itemView, new TextView[]{medicineNameView, remindersSummaryView}, medicineWithReminders.medicine.color);
        } else {
            ViewColorHelper.setDefaultColors((MaterialCardView) itemView, new TextView[]{medicineNameView, remindersSummaryView});
        }
    }

    private void startEditActivity(MedicineWithReminders medicineWithReminders) {
        Intent intent = new Intent(itemView.getContext(), EditMedicine.class);
        intent.putExtra(EXTRA_ID, medicineWithReminders.medicine.medicineId);
        intent.putExtra(EXTRA_USE_COLOR, medicineWithReminders.medicine.useColor);
        intent.putExtra(EXTRA_COLOR, medicineWithReminders.medicine.color);
        intent.putExtra(EXTRA_MEDICINE_NAME, medicineWithReminders.medicine.name);
        itemView.getContext().startActivity(intent);
    }

    public interface DeleteCallback {
        void deleteItem(Context context, long itemId, int adapterPosition);
    }
}
