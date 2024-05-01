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

    private void setupTakenSkippedChart() {
        APIlib.getInstance().setActiveAnyChartView(takenSkippedChartView);
        setupLicense(takenSkippedChartView);
        takenSkippedChart = AnyChart.pie();
        takenSkippedChart.legend().enabled(false);
        takenSkippedChart.credits("");
        takenSkippedChartView.setChart(takenSkippedChart);
    }

    private void setupTakenSkippedTotalChart() {
        APIlib.getInstance().setActiveAnyChartView(takenSkippedTotalChartView);
        setupLicense(takenSkippedTotalChartView);
        takenSkippedTotalChart = AnyChart.pie();
        takenSkippedTotalChart.title(requireContext().getString(R.string.total));
        takenSkippedTotalChart.credits("");
        takenSkippedTotalChartView.setChart(takenSkippedTotalChart);
    }

    private void populateStatistics() {
        StatisticsProvider statisticsProvider;
        statisticsProvider = new StatisticsProvider(new MedicineRepository(requireActivity().getApplication()), requireContext());
        int days = analysisDays.getDays();

        StatisticsProvider.ColumnChartData columnChartData = statisticsProvider.getLastDaysReminders(days);
        requireActivity().runOnUiThread(() -> setMedicinesPerDayChartData(columnChartData));

        List<DataEntry> data = statisticsProvider.getTakenSkippedData(days);
        requireActivity().runOnUiThread(() -> {
            APIlib.getInstance().setActiveAnyChartView(takenSkippedChartView);
            takenSkippedChart.title(requireContext().getString(R.string.last_n_days, days));
            takenSkippedChart.data(data);
        });

        List<DataEntry> dataTotal = statisticsProvider.getTakenSkippedData(0);
        requireActivity().runOnUiThread(() -> {
            APIlib.getInstance().setActiveAnyChartView(takenSkippedTotalChartView);
            takenSkippedTotalChart.data(dataTotal);
        });
    }

    private void setupLicense(AnyChartView anyChartView) {
        String licenseKey = requireContext().getString(R.string.AnyChartLicense);
        if (!licenseKey.isEmpty()) {
            anyChartView.setLicenceKey(requireContext().getString(R.string.AnyChartLicense));
        }
    }

    private void setMedicinesPerDayChartData(StatisticsProvider.ColumnChartData columnChartData) {
        APIlib.getInstance().setActiveAnyChartView(medicinesPerDayChartView);
        setupMedicinesPerDayChart();
        Set medicinesPerDayDataSet = Set.instantiate();
        int i = 0;
        for (String series : columnChartData.series()) {
            String valueString = i > 0 ? "value" + i : "value";
            Mapping seriesMapping = medicinesPerDayDataSet.mapAs(" { x: 'x', value: '" + valueString + "' }");
            medicinesPerDayChart.column(seriesMapping).name(series);
            i++;
        }
        medicinesPerDayDataSet.data(columnChartData.seriesData());
    }

    private void setupMedicinesPerDayChart() {
        if (medicinesPerDayChart != null) {
            recreateMedicinesPerDayChartView();
        }
        APIlib.getInstance().setActiveAnyChartView(medicinesPerDayChartView);
        setupLicense(medicinesPerDayChartView);
        medicinesPerDayChart = AnyChart.column();
        medicinesPerDayChart.yScale().stackMode(ScaleStackMode.VALUE);
        medicinesPerDayChart.legend().enabled(true);
        medicinesPerDayChart.legend("Powered by AnyChart");
        medicinesPerDayChartView.setChart(medicinesPerDayChart);
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
    }

    @Override
    public void onResume() {
        super.onResume();

        Handler handler = new Handler(backgroundThread.getLooper());
        handler.post(this::populateStatistics);
    }
}