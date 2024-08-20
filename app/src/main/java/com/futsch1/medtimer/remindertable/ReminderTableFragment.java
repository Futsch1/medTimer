package com.futsch1.medtimer.remindertable;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.evrencoskun.tableview.TableView;
import com.evrencoskun.tableview.filter.Filter;
import com.futsch1.medtimer.MedicineViewModel;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.helpers.TableHelper;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ReminderTableFragment extends Fragment {

    private TextInputLayout filterLayout;
    private TextInputEditText filter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        MedicineViewModel medicineViewModel = new ViewModelProvider(this).get(MedicineViewModel.class);

        View fragmentView = inflater.inflate(R.layout.fragment_reminder_table, container, false);
        TableView tableView = fragmentView.findViewById(R.id.reminder_table);
        filter = fragmentView.findViewById(R.id.filter);
        filterLayout = fragmentView.findViewById(R.id.filterLayout);
        Filter tableFilter = new Filter(tableView);

        setupFilter(tableFilter);

        ReminderTableAdapter adapter = new ReminderTableAdapter(tableView);

        tableView.setAdapter(adapter);
        adapter.setColumnHeaderItems(TableHelper.getTableHeaders(getContext()));
        medicineViewModel.getLiveReminderEvents(0, 0, false).observe(getViewLifecycleOwner(), adapter::submitList);

        // This is a workaround for a recycler view bug that causes random crashes
        tableView.getCellRecyclerView().setItemAnimator(null);
        tableView.getColumnHeaderRecyclerView().setItemAnimator(null);
        tableView.getRowHeaderRecyclerView().setItemAnimator(null);

        tableView.setUnSelectedColor(MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSecondaryContainer, "TableView"));

        return fragmentView;
    }

    private void setupFilter(Filter tableFilter) {
        filterLayout.setEndIconOnClickListener(v -> {
            filter.setText("");
            tableFilter.set("");
        });
        filter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Intentionally empty
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tableFilter.set(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Intentionally empty
            }
        });
    }

}