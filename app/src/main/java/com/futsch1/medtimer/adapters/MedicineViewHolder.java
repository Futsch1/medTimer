package com.futsch1.medtimer.adapters;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_INDEX;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.EditMedicine;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.MedicineWithReminders;

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

    public void bind(MedicineWithReminders medicineWithReminders) {
        medicineNameView.setText(medicineWithReminders.medicine.name);
        int len = medicineWithReminders.reminders.size();
        if (len == 0) {
            remindersSummaryView.setText(R.string.no_reminders);
        } else {
            String reminders = remindersSummaryView.getResources().getQuantityString(R.plurals.reminders_per_day, len, len);
            remindersSummaryView.setText(reminders);
        }

        itemView.setOnClickListener(view -> {
            Intent intent = new Intent(view.getContext(), EditMedicine.class);
            intent.putExtra(EXTRA_ID, medicineWithReminders.medicine.medicineId);
            intent.putExtra(EXTRA_INDEX, getAdapterPosition());
            view.getContext().startActivity(intent);
        });
    }
}
