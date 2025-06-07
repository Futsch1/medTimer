package com.futsch1.medtimer.statistics;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.androidplot.pie.PieChart;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.MedicineRepository;

import java.util.List;

public class ChartsFragment extends Fragment {
    private HandlerThread backgroundThread;
    private MedicineRepository medicineRepository;

    private PieChart takenSkippedChartView;
    private PieChart takenSkippedTotalChartView;
    private XYPlot medicinesPerDayChartView;
    private TakenSkippedChart takenSkippedChart;
    private TakenSkippedChart takenSkippedTotalChart;
    private MedicinePerDayChart medicinesPerDayChart;

    private int days = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        backgroundThread = new HandlerThread("LoadStatistics");
        backgroundThread.start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View statisticsView = inflater.inflate(R.layout.fragment_charts, container, false);

        medicinesPerDayChartView = statisticsView.findViewById(R.id.medicinesPerDayChart);
        takenSkippedChartView = statisticsView.findViewById(R.id.takenSkippedChart);
        takenSkippedTotalChartView = statisticsView.findViewById(R.id.takenSkippedChartTotal);
        medicineRepository = new MedicineRepository(requireActivity().getApplication());

        setupTakenSkippedCharts();
        Handler handler = new Handler(backgroundThread.getLooper());
        handler.post(this::setupMedicinesPerDayChart);

        return statisticsView;
    }

    private void setupTakenSkippedCharts() {
        this.takenSkippedChart = new TakenSkippedChart(takenSkippedChartView, requireContext());
        this.takenSkippedTotalChart = new TakenSkippedChart(takenSkippedTotalChartView, requireContext());
    }

    private void setupMedicinesPerDayChart() {
        try {
            this.medicinesPerDayChart = new MedicinePerDayChart(medicinesPerDayChartView, requireContext(), medicineRepository.getMedicines());
        } catch (IllegalStateException e) {
            // Intentionally empty
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Handler handler = new Handler(backgroundThread.getLooper());
        handler.post(this::populateStatistics);
    }

    private void populateStatistics() {
        StatisticsProvider statisticsProvider;
        statisticsProvider = new StatisticsProvider(medicineRepository);

        try {
            List<SimpleXYSeries> series = statisticsProvider.getLastDaysReminders(days);
            requireActivity().runOnUiThread(() -> medicinesPerDayChart.updateData(series));

            StatisticsProvider.TakenSkipped data = statisticsProvider.getTakenSkippedData(days);
            requireActivity().runOnUiThread(() -> takenSkippedChart.updateData(data.taken(), data.skipped(), days));

            StatisticsProvider.TakenSkipped dataTotal = statisticsProvider.getTakenSkippedData(0);
            requireActivity().runOnUiThread(() -> takenSkippedTotalChart.updateData(dataTotal.taken(), dataTotal.skipped(), 0));
        } catch (IllegalStateException e) {
            // Intentionally empty - just don't do anything
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        backgroundThread.quit();
    }

    public void setDays(int days) {
        this.days = days;

        if (medicineRepository != null) {
            Handler handler = new Handler(backgroundThread.getLooper());
            handler.post(this::populateStatistics);
        }
    }

}