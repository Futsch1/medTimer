package com.futsch1.medtimer;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.adapters.MedicineViewAdapter;
import com.futsch1.medtimer.database.Medicine;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class MedicinesFragment extends Fragment {
    private final HandlerThread thread;
    private MedicineViewModel medicineViewModel;

    public MedicinesFragment() {
        // Required empty public constructor
        this.thread = new HandlerThread("UpdateNextReminder");
        this.thread.start();
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

        // Swipe to delete
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.LEFT, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder, int direction) {
                if (direction == ItemTouchHelper.LEFT) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(fragmentView.getContext());
                    builder.setTitle(R.string.confirm);
                    builder.setMessage(R.string.are_you_sure_delete_medicine);
                    builder.setCancelable(false);
                    builder.setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                        final Handler handler = new Handler(thread.getLooper());
                        handler.post(() -> {
                            Medicine medicine = medicineViewModel.getMedicine((int) viewHolder.getItemId());
                            medicineViewModel.deleteMedicine(medicine);
                            final Handler mainHandler = new Handler(Looper.getMainLooper());
                            mainHandler.post(() -> {
                                adapter.notifyItemRangeChanged(viewHolder.getAdapterPosition(), viewHolder.getAdapterPosition() + 1);
                            });
                        });
                    });
                    builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                        adapter.notifyItemRangeChanged(viewHolder.getAdapterPosition(), viewHolder.getAdapterPosition() + 1);
                    });
                    builder.show();
                }
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        // Connect view model to recycler view adapter
        medicineViewModel.getMedicines().observe(getViewLifecycleOwner(), adapter::submitList);

        // Floating add medicine button
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