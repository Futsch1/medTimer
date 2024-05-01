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

import com.anychart.APIlib;
import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.charts.Cartesian;
import com.anychart.charts.Pie;
import com.anychart.data.Mapping;
import com.anychart.data.Set;
import com.anychart.enums.Align;
import com.anychart.enums.LegendLayout;
import com.anychart.enums.ScaleStackMode;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.MedicineRepository;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class StatisticsFragment extends Fragment {
    private final HandlerThread backgroundThread;
    private AnalysisDays analysisDays;

    private Pie takenSkippedChart;
    private Pie takenSkippedTotalChart;
    private Cartesian medicinesPerDayChart;
    private AnyChartView takenSkippedChartView;
    private AnyChartView takenSkippedTotalChartView;
    private AnyChartView medicinesPerDayChartView;
    private Spinner timeSpinner;
    private StatisticsProvider statisticsProvider;
    private int dayColumns = 0;
    private Set medicinesPerDayDataSet;

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
        medicinesPerDayChartView.setProgressBar(statisticsView.findViewById(R.id.progressBar));
        takenSkippedChartView = statisticsView.findViewById(R.id.takenSkippedChart);
        takenSkippedTotalChartView = statisticsView.findViewById(R.id.takenSkippedChartTotal);
        timeSpinner = statisticsView.findViewById(R.id.timeSpinner);

        setupReminderTableButton(statisticsView);
        setupTimeSpinner();
        setupMedicinesPerDayChart();
        setupTakenSkippedChart();
        setupTakenSkippedTotalChart();

        return statisticsView;
    }

    private static void setupReminderTableButton(View statisticsView) {
        MaterialButton reminderTableButton = statisticsView.findViewById(R.id.reminderTableButton);
        reminderTableButton.setOnClickListener(view -> {
            NavController navController = Navigation.findNavController(statisticsView);
            navController.navigate(com.futsch1.medtimer.MainFragmentDirections.actionMainFragmentToReminderTableFragment());
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

    private void setupMedicinesPerDayChart() {
        APIlib.getInstance().setActiveAnyChartView(medicinesPerDayChartView);
        medicinesPerDayChart = AnyChart.column();
        medicinesPerDayChart.yScale().stackMode(ScaleStackMode.VALUE);
        medicinesPerDayChart.legend().enabled(true);
        medicinesPerDayChartView.setChart(medicinesPerDayChart);
    }

    private void setupTakenSkippedChart() {
        APIlib.getInstance().setActiveAnyChartView(takenSkippedChartView);
        takenSkippedChart = AnyChart.pie();
        takenSkippedChart.legend()
                .position("center-bottom")
                .itemsLayout(LegendLayout.HORIZONTAL)
                .align(Align.CENTER);
        takenSkippedChartView.setChart(takenSkippedChart);
    }

    private void setupTakenSkippedTotalChart() {
        APIlib.getInstance().setActiveAnyChartView(takenSkippedTotalChartView);
        takenSkippedTotalChart = AnyChart.pie();
        takenSkippedTotalChart.legend()
                .position("center-bottom")
                .itemsLayout(LegendLayout.HORIZONTAL)
                .align(Align.CENTER);
        takenSkippedTotalChartView.setChart(takenSkippedTotalChart);
    }

    private void populateStatistics() {
        if (statisticsProvider == null) {
            statisticsProvider = new StatisticsProvider(new MedicineRepository(requireActivity().getApplication()), requireContext());
        }
        int days = analysisDays.getDays();

        StatisticsProvider.ColumnChartData columnChartData = statisticsProvider.getLastDaysReminders(days);
        requireActivity().runOnUiThread(() -> setMedicinesPerDayChartData(columnChartData));

        List<DataEntry> data = statisticsProvider.getTakenSkippedData(days);
        requireActivity().runOnUiThread(() -> {
            APIlib.getInstance().setActiveAnyChartView(takenSkippedChartView);
            takenSkippedChart.data(data);
        });

        List<DataEntry> dataTotal = statisticsProvider.getTakenSkippedData(0);
        requireActivity().runOnUiThread(() -> {
            APIlib.getInstance().setActiveAnyChartView(takenSkippedTotalChartView);
            takenSkippedTotalChart.data(dataTotal);
        });
    }

    private void setMedicinesPerDayChartData(StatisticsProvider.ColumnChartData columnChartData) {
        APIlib.getInstance().setActiveAnyChartView(medicinesPerDayChartView);
        if (dayColumns == 0 || dayColumns != columnChartData.seriesData().size()) {
            if (dayColumns != 0) {
                recreateMedicinesPerDayChartView();
            }
            medicinesPerDayDataSet = Set.instantiate();
            int i = 0;
            for (String series : columnChartData.series()) {
                String valueString = i > 0 ? "value" + i : "value";
                Mapping seriesMapping = medicinesPerDayDataSet.mapAs(" { x: 'x', value: '" + valueString + "' }");
                medicinesPerDayChart.column(seriesMapping).name(series);
                i++;
            }
            dayColumns = columnChartData.seriesData().size();
        }
        medicinesPerDayDataSet.data(columnChartData.seriesData());
    }

    private void recreateMedicinesPerDayChartView() {
        // It is not possible to change the chart in the chart view to change
        // a different number of columns and resetting the chart also does not work.
        // So the whole view is recreated.
        ViewGroup parent = (ViewGroup) medicinesPerDayChartView.getParent();
        int index = parent.indexOfChild(medicinesPerDayChartView);
        parent.removeView(medicinesPerDayChartView);
        ViewGroup.LayoutParams layout = medicinesPerDayChartView.getLayoutParams();
        medicinesPerDayChartView = new AnyChartView(requireContext());
        medicinesPerDayChartView.setLayoutParams(layout);
        parent.addView(medicinesPerDayChartView, index);
        setupMedicinesPerDayChart();
    }

    @Override
    public void onResume() {
        super.onResume();

        Handler handler = new Handler(backgroundThread.getLooper());
        handler.post(this::populateStatistics);
    }
}