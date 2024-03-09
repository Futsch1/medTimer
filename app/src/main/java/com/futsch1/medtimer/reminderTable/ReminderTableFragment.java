package com.futsch1.medtimer.reminderTable;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.evrencoskun.tableview.TableView;
import com.futsch1.medtimer.MedicineViewModel;
import com.futsch1.medtimer.R;

public class ReminderTableFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        MedicineViewModel medicineViewModel = new ViewModelProvider(this).get(MedicineViewModel.class);

        View fragmentView = inflater.inflate(R.layout.fragment_reminder_table, container, false);
        TableView tableView = fragmentView.findViewById(R.id.reminder_table);

        ReminderTableAdapter adapter = new ReminderTableAdapter();

        tableView.setAdapter(adapter);
        medicineViewModel.getReminderEvents(0, 0).observe(getViewLifecycleOwner(), adapter::submitList);

        return fragmentView;
    }

}