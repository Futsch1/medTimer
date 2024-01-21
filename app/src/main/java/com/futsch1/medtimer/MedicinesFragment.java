package com.futsch1.medtimer;

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

        // Medicine recycler
        RecyclerView recyclerView = null;
        if (container != null) {
            recyclerView = container.findViewById(R.id.medicineEntities);
            final MedicineViewAdapter adapter = new MedicineViewAdapter(new MedicineViewAdapter.MedicineDiff());
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(container.getContext()));

            // Get a new or existing ViewModel from the ViewModelProvider.
            medicineViewModel = new ViewModelProvider(this).get(MedicineViewModel.class);

            medicineViewModel.getMedicines().observe(getViewLifecycleOwner(), adapter::submitList);
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_medicines, container, false);
    }
}