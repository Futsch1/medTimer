package com.futsch1.medtimer;

import static android.app.Activity.RESULT_OK;
import static com.futsch1.medtimer.ActivityCodes.MEDICINE_ACTIVITY_REQUEST_CODE;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.adapters.MedicineViewAdapter;
import com.futsch1.medtimer.database.Medicine;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MedicinesFragment extends Fragment {
    private MedicineViewModel medicineViewModel;

    public MedicinesFragment() {
        // Required empty public constructor
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MEDICINE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            Medicine medicine = new Medicine(data.getStringExtra(ActivityCodes.EXTRA_REPLY));
            medicineViewModel.insert(medicine);
        }
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
        RecyclerView recyclerView = fragmentView.findViewById(R.id.medicineEntities);
        // Get a new or existing ViewModel from the ViewModelProvider.
        medicineViewModel = new ViewModelProvider(this).get(MedicineViewModel.class);

        final MedicineViewAdapter adapter = new MedicineViewAdapter(new MedicineViewAdapter.MedicineDiff(), medicineViewModel);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(fragmentView.getContext()));

        medicineViewModel.getMedicines().observe(getViewLifecycleOwner(), adapter::submitList);

        // FAB
        FloatingActionButton fab = fragmentView.findViewById(R.id.addMedicine);
        fab.setOnClickListener(view -> {
            Intent intent = new Intent(getContext(), MedicineActivity.class);
            startActivityForResult(intent, MEDICINE_ACTIVITY_REQUEST_CODE);
        });

        return fragmentView;
    }
}