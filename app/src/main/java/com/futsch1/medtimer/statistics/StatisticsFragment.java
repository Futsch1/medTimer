package com.futsch1.medtimer.statistics;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.androidplot.pie.PieChart;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.futsch1.medtimer.MedicineViewModel;
import com.futsch1.medtimer.OptionsMenu;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.MedicineRepository;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class StatisticsFragment extends Fragment {
    private final HandlerThread backgroundThread;
    private AnalysisDays analysisDays;
    private MedicineRepository medicineRepository;

    private PieChart takenSkippedChartView;
    private PieChart takenSkippedTotalChartView;
    private XYPlot medicinesPerDayChartView;
    private Spinner timeSpinner;
    private TakenSkippedChart takenSkippedChart;
    private TakenSkippedChart takenSkippedTotalChart;
    private MedicinePerDayChart medicinesPerDayChart;


    public StatisticsFragment() {
        backgroundThread = new HandlerThread("LoadStatistics");
        backgroundThread.start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View statisticsView = inflater.inflate(R.layout.fragment_statistics, container, false);

        analysisDays = new AnalysisDays(requireContext());
        medicinesPerDayChartView = statisticsView.findViewById(R.id.medicinesPerDayChart);
        takenSkippedChartView = statisticsView.findViewById(R.id.takenSkippedChart);
        takenSkippedTotalChartView = statisticsView.findViewById(R.id.takenSkippedChartTotal);
        timeSpinner = statisticsView.findViewById(R.id.timeSpinner);
        medicineRepository = new MedicineRepository(requireActivity().getApplication());

        setupReminderTableButton(statisticsView);
        setupReminderCalendarButton(statisticsView);
        setupTimeSpinner();
        setupTakenSkippedCharts();
        Handler handler = new Handler(backgroundThread.getLooper());
        handler.post(this::setupMedicinesPerDayChart);

        OptionsMenu optionsMenu = new OptionsMenu(this.requireContext(),
                new MedicineViewModel(requireActivity().getApplication()),
                this,
                statisticsView);
        requireActivity().addMenuProvider(optionsMenu, getViewLifecycleOwner());

        return statisticsView;
    }

    private static void setupReminderTableButton(View statisticsView) {
        MaterialButton reminderTableButton = statisticsView.findViewById(R.id.reminderTableButton);
        reminderTableButton.setOnClickListener(view -> {
            NavController navController = Navigation.findNavController(statisticsView);
            navController.navigate(com.futsch1.medtimer.statistics.StatisticsFragmentDirections.actionStatisticsFragmentToReminderTableFragment());
        });
    }

    private void setupReminderCalendarButton(View statisticsView) {
        MaterialButton reminderCalendarButton = statisticsView.findViewById(R.id.reminderCalendarButton);
        reminderCalendarButton.setOnClickListener(view -> {
            NavController navController = Navigation.findNavController(statisticsView);
            navController.navigate(com.futsch1.medtimer.statistics.StatisticsFragmentDirections.actionStatisticsFragmentToMedicineCalendarFragment(-1, 90, 0));
        });
    }

    private void setupTimeSpinner() {
        timeSpinner.setSelection(analysisDays.getPosition());
        timeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != analysisDays.getPosition()) {
                    analysisDays.setPosition(position);
                    Handler handler = new Handler(backgroundThread.getLooper());
                    handler.post(() -> populateStatistics());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Intentionally empty
            }
        });
    }

    private void setupTakenSkippedCharts() {
        this.takenSkippedChart = new TakenSkippedChart(takenSkippedChartView, requireContext());
        this.takenSkippedTotalChart = new TakenSkippedChart(takenSkippedTotalChartView, requireContext());
    }

    private void setupMedicinesPerDayChart() {
        this.medicinesPerDayChart = new MedicinePerDayChart(medicinesPerDayChartView, requireContext(), medicineRepository.getMedicines());
    }

    private void populateStatistics() {
        StatisticsProvider statisticsProvider;
        statisticsProvider = new StatisticsProvider(medicineRepository);
        int days = analysisDays.getDays();

        List<SimpleXYSeries> series = statisticsProvider.getLastDaysReminders(days);
        requireActivity().runOnUiThread(() -> medicinesPerDayChart.updateData(series));

        StatisticsProvider.TakenSkipped data = statisticsProvider.getTakenSkippedData(days);
        requireActivity().runOnUiThread(() -> takenSkippedChart.updateData(data.taken(), data.skipped(), days));

        StatisticsProvider.TakenSkipped dataTotal = statisticsProvider.getTakenSkippedData(0);
        requireActivity().runOnUiThread(() -> takenSkippedTotalChart.updateData(dataTotal.taken(), dataTotal.skipped(), 0));
    }

    @Override
    public void onResume() {
        super.onResume();

        Handler handler = new Handler(backgroundThread.getLooper());
        handler.post(this::populateStatistics);
    }
}