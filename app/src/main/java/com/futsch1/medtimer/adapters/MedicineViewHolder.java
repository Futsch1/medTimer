package com.futsch1.medtimer.adapters;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_INDEX;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.EditMedicine;
import com.futsch1.medtimer.MedicineViewModel;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.MedicineWithReminders;

public class MedicineViewHolder extends RecyclerView.ViewHolder {
    private final TextView medicineNameView;
    private final TextView remindersSummaryView;
    private final ImageButton editButton;
    private final ImageButton deleteButton;

    private MedicineViewHolder(View itemView) {
        super(itemView);
        medicineNameView = itemView.findViewById(R.id.medicineName);
        remindersSummaryView = itemView.findViewById(R.id.remindersSummary);
        editButton = itemView.findViewById(R.id.editMedicine);
        deleteButton = itemView.findViewById(R.id.deleteMedicine);
    }

    static MedicineViewHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_medicine, parent, false);
        return new MedicineViewHolder(view);
    }

    public void bind(MedicineWithReminders medicineWithReminders, MedicineViewModel viewModel, ActivityResultLauncher<Intent> activityResultLauncher) {
        medicineNameView.setText(medicineWithReminders.medicine.name);
        int len = medicineWithReminders.reminders.size();
        if (len == 0) {
            remindersSummaryView.setText(R.string.no_reminders);
        } else {
            String reminders = remindersSummaryView.getResources().getQuantityString(R.plurals.reminders_per_day, len, len);
            remindersSummaryView.setText(reminders);
        }

        deleteButton.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
            builder.setTitle(R.string.confirm);
            builder.setMessage(R.string.are_you_sure_delete_medicine);
            builder.setCancelable(false);
            builder.setPositiveButton(R.string.yes, (dialogInterface, i) -> viewModel.deleteMedicine(medicineWithReminders.medicine));
            builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
            });
            builder.show();
        });
        editButton.setOnClickListener(view -> {
            Intent intent = new Intent(view.getContext(), EditMedicine.class);
            intent.putExtra(EXTRA_ID, medicineWithReminders.medicine.medicineId);
            intent.putExtra(EXTRA_INDEX, getAdapterPosition());
            activityResultLauncher.launch(intent);
        });
    }
}
