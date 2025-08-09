package com.futsch1.medtimer.medicine;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.MedicineViewModel;
import com.futsch1.medtimer.OptionsMenu;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.helpers.DeleteHelper;
import com.futsch1.medtimer.helpers.SimpleIdlingResource;
import com.futsch1.medtimer.helpers.SwipeHelper;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class MedicinesFragment extends Fragment {
    private SimpleIdlingResource idlingResource;
    @SuppressWarnings("java:S1450")
    private MedicineViewModel medicineViewModel;
    @SuppressWarnings("java:S1450")
    private MedicineViewAdapter adapter;
    private HandlerThread thread;
    private OptionsMenu optionsMenu = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.thread = new HandlerThread("GetSummary");
        this.thread.start();
        idlingResource = new SimpleIdlingResource(MedicinesFragment.class.getName());
        idlingResource.setBusy();

        // Get a new or existing ViewModel from the ViewModelProvider.
        medicineViewModel = new ViewModelProvider(this).get(MedicineViewModel.class);

        optionsMenu = new OptionsMenu(this,
                medicineViewModel,
                NavHostFragment.findNavController(this), false);

        adapter = new MedicineViewAdapter(thread, requireActivity(), medicineViewModel.medicineRepository);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_medicines, container, false);
        // Medicine recycler
        RecyclerView recyclerView = fragmentView.findViewById(R.id.medicineList);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(fragmentView.getContext()));

        // Swipe to delete
        ItemTouchHelper itemTouchHelper = SwipeHelper.createSwipeHelper(requireContext(), viewHolder -> deleteItem(requireContext(), viewHolder.getItemId(), viewHolder.getBindingAdapterPosition()), adapter);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        postponeEnterTransition();

        setupAddMedicineButton(fragmentView);

        MedicinesMenu medicinesMenu = new MedicinesMenu(medicineViewModel, thread);
        requireActivity().addMenuProvider(medicinesMenu, getViewLifecycleOwner());

        // Connect view model to recycler view adapter
        medicineViewModel.getMedicines().observe(getViewLifecycleOwner(), l -> {
            adapter.submitList(l);
            medicinesMenu.medicinesList = l;
            startPostponedEnterTransition();
            idlingResource.setIdle();
        });

        requireActivity().addMenuProvider(optionsMenu, getViewLifecycleOwner());

        return fragmentView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (optionsMenu != null) {
            optionsMenu.onDestroy();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (thread != null) {
            thread.quit();
        }
        if (idlingResource != null) {
            idlingResource.destroy();
        }
    }

    private void deleteItem(Context context, long itemId, int adapterPosition) {
        DeleteHelper deleteHelper = new DeleteHelper(context);
        deleteHelper.deleteItem(R.string.are_you_sure_delete_medicine, () -> medicineViewModel.medicineRepository.deleteMedicine((int) itemId), () -> adapter.notifyItemChanged(adapterPosition));
    }

    private void setupAddMedicineButton(View fragmentView) {
        ExtendedFloatingActionButton fab = fragmentView.findViewById(R.id.addMedicine);
        fab.setOnClickListener(view -> {
            TextInputLayout textInputLayout = new TextInputLayout(requireContext());
            TextInputEditText editText = new TextInputEditText(requireContext());
            editText.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            editText.setHint(R.string.medicine_name);
            editText.setSingleLine();
            editText.setId(R.id.medicineName);
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
                new Handler(thread.getLooper()).post(() -> {
                    double highestSortOrder = medicineViewModel.medicineRepository.getHighestMedicineSortOrder();
                    Medicine medicine = new Medicine(e.toString().trim());
                    medicine.sortOrder = highestSortOrder + 1;
                    int medicineId = (int) medicineViewModel.medicineRepository.insertMedicine(medicine);
                    requireActivity().runOnUiThread(() -> navigateToMedicineId(medicineId));
                });
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        return builder;
    }

    private void navigateToMedicineId(int medicineId) {
        NavController navController = Navigation.findNavController(this.requireView());
        MedicinesFragmentDirections.ActionMedicinesFragmentToEditMedicineFragment action = MedicinesFragmentDirections.actionMedicinesFragmentToEditMedicineFragment(
                medicineId
        );
        navController.navigate(action);
    }
}