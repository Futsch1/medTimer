package com.futsch1.medtimer.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.Medicine;

public class MedicineViewHolder extends RecyclerView.ViewHolder {
    private final TextView medicineNameView;

    private MedicineViewHolder(View itemView) {
        super(itemView);
        medicineNameView = itemView.findViewById(R.id.medicineName);
    }

    static MedicineViewHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_medicine, parent, false);
        return new MedicineViewHolder(view);
    }

    public void bind(Medicine medicine) {
        medicineNameView.setText(medicine.name);
    }
}
