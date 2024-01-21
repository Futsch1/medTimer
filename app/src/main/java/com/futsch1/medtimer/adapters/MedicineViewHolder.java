package com.futsch1.medtimer.adapters;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.EditMedicine;
import com.futsch1.medtimer.MedicineViewModel;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.Medicine;

public class MedicineViewHolder extends RecyclerView.ViewHolder {
    private final TextView medicineNameView;
    private final ImageButton editButton;
    private final ImageButton deleteButton;

    private MedicineViewHolder(View itemView) {
        super(itemView);
        medicineNameView = itemView.findViewById(R.id.medicineName);
        editButton = itemView.findViewById(R.id.editMedicine);
        deleteButton = itemView.findViewById(R.id.deleteMedicine);
    }

    static MedicineViewHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_medicine, parent, false);
        return new MedicineViewHolder(view);
    }

    public void bind(Medicine medicine, MedicineViewModel viewModel) {
        medicineNameView.setText(medicine.name);

        deleteButton.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
            builder.setTitle(R.string.confirm);
            builder.setMessage(R.string.are_you_sure_delete_medicine);
            builder.setCancelable(false);
            builder.setPositiveButton(R.string.yes, (dialogInterface, i) -> viewModel.delete(medicine));
            builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
            });
            builder.show();
        });
        editButton.setOnClickListener(view -> {
            Intent intent = new Intent(view.getContext(), EditMedicine.class);
            intent.putExtra("medicineIndex", this.getAdapterPosition());
            view.getContext().startActivity(intent);
        });
    }
}
