package com.futsch1.medtimer.medicine;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.app.AlertDialog;
import android.content.Context;
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

import com.futsch1.medtimer.MedicineViewModel;
import com.futsch1.medtimer.OptionsMenu;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.helpers.DeleteHelper;
import com.futsch1.medtimer.helpers.SwipeHelper;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class MedicinesFragment extends Fragment {
    private HandlerThread thread;
    @SuppressWarnings("java:S1450")
    private MedicineViewModel medicineViewModel;
    @SuppressWarnings("java:S1450")
    private MedicineViewAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.thread = new HandlerThread("DeleteMedicine");
        this.thread.start();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View fragmentView = inflater.inflate(R.layout.fragment_medicines, container, false);
        // Medicine recycler
        RecyclerView recyclerView = fragmentView.findViewById(R.id.medicineList);

        // Get a new or existing ViewModel from the ViewModelProvider.
        medicineViewModel = new ViewModelProvider(this).get(MedicineViewModel.class);

        adapter = new MedicineViewAdapter(new MedicineViewAdapter.MedicineDiff());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(fragmentView.getContext()));

        // Swipe to delete
        ItemTouchHelper itemTouchHelper = SwipeHelper.createLeftSwipeTouchHelper(requireContext(), viewHolder -> deleteItem(requireContext(), viewHolder.getItemId(), viewHolder.getBindingAdapterPosition()));
        itemTouchHelper.attachToRecyclerView(recyclerView);

        // Connect view model to recycler view adapter
        medicineViewModel.getMedicines().observe(getViewLifecycleOwner(), adapter::submitList);

        setupAddMedicineButton(fragmentView);

        OptionsMenu optionsMenu = new OptionsMenu(this.requireContext(),
                new MedicineViewModel(requireActivity().getApplication()),
                this,
                fragmentView);
        requireActivity().addMenuProvider(optionsMenu, getViewLifecycleOwner());

        return fragmentView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (thread != null) {
            thread.quitSafely();
        }
    }

    private void deleteItem(Context context, long itemId, int adapterPosition) {
        DeleteHelper deleteHelper = new DeleteHelper(context);
        deleteHelper.deleteItem(R.string.are_you_sure_delete_medicine, () -> {
            final Handler threadHandler = new Handler(thread.getLooper());
            threadHandler.post(() -> {
                Medicine medicine = medicineViewModel.getMedicine((int) itemId);
                medicineViewModel.deleteMedicine(medicine);
                final Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> adapter.notifyItemRangeChanged(adapterPosition, adapterPosition + 1));
            });
        }, () -> adapter.notifyItemRangeChanged(adapterPosition, adapterPosition + 1));
    }

    private void setupAddMedicineButton(View fragmentView) {
        ExtendedFloatingActionButton fab = fragmentView.findViewById(R.id.addMedicine);
        fab.setOnClickListener(view -> {
            TextInputLayout textInputLayout = new TextInputLayout(requireContext());
            TextInputEditText editText = new TextInputEditText(requireContext());
            editText.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            editText.setHint(R.string.medicine_name);
            editText.setSingleLine();
            textInputLayout.addView(editText);

            AlertDialog.Builder builder = getAlertBuilder(textInputLayout, editText);
            AlertDialog dialog = builder.create();
            dialog.show();
        });
    }

    @NonNull
    private AlertDialog.Builder getAlertBuilder(TextInputLayout textInputLayout, TextInputEditText editText) {
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
        return builder;
    }
}