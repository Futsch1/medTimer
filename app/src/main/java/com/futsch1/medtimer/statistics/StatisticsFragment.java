package com.futsch1.medtimer.statistics;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
    private Pie takenSkippedChart;
    private Cartesian medicinesPerDayChart;
    private AnyChartView takenSkippedChartView;
    private AnyChartView medicinesPerDayChartView;
    private boolean firstRun = true;

    public StatisticsFragment() {
        backgroundThread = new HandlerThread("LoadStatistics");
        backgroundThread.start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View statisticsView;
        statisticsView = inflater.inflate(R.layout.fragment_statistics, container, false);

        MaterialButton reminderTableButton = statisticsView.findViewById(R.id.reminderTableButton);
        reminderTableButton.setOnClickListener(view -> {
            NavController navController = Navigation.findNavController(statisticsView);
            navController.navigate(com.futsch1.medtimer.MainFragmentDirections.actionMainFragmentToReminderTableFragment());
        });

        medicinesPerDayChartView = statisticsView.findViewById(R.id.medicinesPerDayChart);
        medicinesPerDayChartView.setProgressBar(statisticsView.findViewById(R.id.progressBar));
        takenSkippedChartView = statisticsView.findViewById(R.id.takenSkippedChart);

        APIlib.getInstance().setActiveAnyChartView(medicinesPerDayChartView);
        medicinesPerDayChart = AnyChart.column();
        medicinesPerDayChart.yScale().stackMode(ScaleStackMode.VALUE);
        medicinesPerDayChart.legend().enabled(true);
        medicinesPerDayChartView.setChart(medicinesPerDayChart);

        APIlib.getInstance().setActiveAnyChartView(takenSkippedChartView);
        takenSkippedChart = AnyChart.pie();
        takenSkippedChart.legend()
                .position("center-bottom")
                .itemsLayout(LegendLayout.HORIZONTAL)
                .align(Align.CENTER);
        takenSkippedChartView.setChart(takenSkippedChart);

        // TODO: Change number of days back
        // TODO: Add second pie chart showing taken/skipped for number of days back

        return statisticsView;
    }

    @Override
    public void onResume() {
        super.onResume();

        Handler handler = new Handler(backgroundThread.getLooper());
        handler.post(this::populateStatistics);
    }

    private void populateStatistics() {
        StatisticsProvider statisticsProvider = new StatisticsProvider(new MedicineRepository(requireActivity().getApplication()), requireContext());

        StatisticsProvider.ColumnChartData columnChartData = statisticsProvider.getLastDaysReminders(7);
        requireActivity().runOnUiThread(() -> {
            APIlib.getInstance().setActiveAnyChartView(medicinesPerDayChartView);
            Set set = Set.instantiate();
            set.data(columnChartData.seriesData());
            if (firstRun) {
                int i = 0;
                for (String series : columnChartData.series()) {
                    String valueString = i > 0 ? "value" + i : "value";
                    Mapping seriesMapping = set.mapAs(" { x: 'x', value: '" + valueString + "' }");
                    medicinesPerDayChart.column(seriesMapping).name(series);
                    i++;
                }
                firstRun = false;
            }
        });

        List<DataEntry> data = statisticsProvider.getTakenSkippedData();
        requireActivity().runOnUiThread(() -> {
            APIlib.getInstance().setActiveAnyChartView(takenSkippedChartView);
            takenSkippedChart.data(data);
        });
    }
}