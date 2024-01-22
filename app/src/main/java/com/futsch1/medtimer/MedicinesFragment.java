package com.futsch1.medtimer;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.adapters.MedicineViewAdapter;
import com.futsch1.medtimer.database.Medicine;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class MedicinesFragment extends Fragment {
    private MedicineViewModel medicineViewModel;

    ActivityResultLauncher<Intent> medicineActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK) {
                        if (result.getData() != null) {
                            String name = result.getData().getStringExtra(ActivityCodes.EXTRA_MEDICINE);
                            int id = result.getData().getIntExtra(ActivityCodes.EXTRA_ID, -1);
                            if (id > 0) {
                                medicineViewModel.updateMedicine(new Medicine(name, id));
                            } else {
                                medicineViewModel.insertMedicine(new Medicine(name));
                            }
                        }
                    }
                }
            });

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

        final MedicineViewAdapter adapter = new MedicineViewAdapter(new MedicineViewAdapter.MedicineDiff(), medicineViewModel, medicineActivityResultLauncher);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(fragmentView.getContext()));

        medicineViewModel.getMedicines().observe(getViewLifecycleOwner(), adapter::submitList);

        // FAB
        ExtendedFloatingActionButton fab = fragmentView.findViewById(R.id.addMedicine);
        fab.setOnClickListener(view -> {
            Intent intent = new Intent(getContext(), AddMedicine.class);
            medicineActivityResultLauncher.launch(intent);
        });

        return fragmentView;
    }
}