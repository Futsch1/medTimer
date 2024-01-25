package com.futsch1.medtimer;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.adapters.MedicineViewAdapter;
import com.futsch1.medtimer.database.Medicine;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class MedicinesFragment extends Fragment {
    private MedicineViewModel medicineViewModel;

    public MedicinesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View fragmentView = inflater.inflate(R.layout.fragment_medicines, container, false);
        // Medicine recycler
        RecyclerView recyclerView = fragmentView.findViewById(R.id.medicineList);
        // Get a new or existing ViewModel from the ViewModelProvider.
        medicineViewModel = new ViewModelProvider(this).get(MedicineViewModel.class);

        final MedicineViewAdapter adapter = new MedicineViewAdapter(new MedicineViewAdapter.MedicineDiff(), medicineViewModel);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(fragmentView.getContext()));

        medicineViewModel.getMedicines().observe(getViewLifecycleOwner(), adapter::submitList);

        // FAB
        ExtendedFloatingActionButton fab = fragmentView.findViewById(R.id.addMedicine);
        fab.setOnClickListener(view -> {
            TextInputLayout textInputLayout = new TextInputLayout(requireContext());
            TextInputEditText editText = new TextInputEditText(requireContext());
            editText.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            editText.setHint(R.string.medicine_name);
            textInputLayout.addView(editText);

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setView(textInputLayout);
            builder.setTitle(R.string.add_medicine);
            builder.setPositiveButton(R.string.ok, (dialog, which) -> {
                Editable e = editText.getText();
                if (e != null) {
                    medicineViewModel.insertMedicine(new Medicine(e.toString()));
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            AlertDialog dialog = builder.create();
            dialog.show();
        });

        return fragmentView;
    }
}