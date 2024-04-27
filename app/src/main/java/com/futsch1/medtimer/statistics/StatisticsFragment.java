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
import com.anychart.charts.Pie;
import com.anychart.enums.Align;
import com.anychart.enums.LegendLayout;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.MedicineRepository;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class StatisticsFragment extends Fragment {
    private final HandlerThread backgroundThread;
    private Pie takenSkippedChart;
    private AnyChartView anyChartView;

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

        anyChartView = statisticsView.findViewById(R.id.takenSkippedChart);
        takenSkippedChart = AnyChart.pie();
        takenSkippedChart.legend()
                .position("center-bottom")
                .itemsLayout(LegendLayout.HORIZONTAL)
                .align(Align.CENTER);
        anyChartView.setChart(takenSkippedChart);

        return statisticsView;
    }

    @Override
    public void onResume() {
        super.onResume();

        APIlib.getInstance().setActiveAnyChartView(anyChartView);

        Handler handler = new Handler(backgroundThread.getLooper());
        handler.post(this::populateStatistics);
    }

    private void populateStatistics() {
        StatisticsProvider statisticsProvider = new StatisticsProvider(new MedicineRepository(requireActivity().getApplication()), requireContext());

        List<DataEntry> data = statisticsProvider.getTakenSkippedData();
        requireActivity().runOnUiThread(() -> takenSkippedChart.data(data));

    }
}