package com.futsch1.medtimer.statistics;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.charts.Pie;
import com.anychart.enums.Align;
import com.anychart.enums.LegendLayout;
import com.futsch1.medtimer.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class StatisticsFragment extends Fragment {
    private final HandlerThread backgroundThread;
    private View statisticsView;

    public StatisticsFragment() {
        backgroundThread = new HandlerThread("LoadStatistics");
        backgroundThread.start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        statisticsView = inflater.inflate(R.layout.fragment_statistics, container, false);

        Handler handler = new Handler(backgroundThread.getLooper());
        handler.post(this::populateStatistics);

        MaterialButton reminderTableButton = statisticsView.findViewById(R.id.reminderTableButton);
        reminderTableButton.setOnClickListener(view -> {
            NavController navController = Navigation.findNavController(statisticsView);
            navController.navigate(com.futsch1.medtimer.MainFragmentDirections.actionMainFragmentToReminderTableFragment());
        });

        return statisticsView;
    }

    private void populateStatistics() {
        Context context = statisticsView.getContext();
        AnyChartView anyChartView = statisticsView.findViewById(R.id.takenSkippedChart);

        Pie pie = AnyChart.pie();

        List<DataEntry> data = new ArrayList<>();
        data.add(new ValueDataEntry(context.getString(R.string.taken), 20));
        data.add(new ValueDataEntry(context.getString(R.string.skipped), 10));
        pie.data(data);

/*        pie.title("Fruits imported in 2015 (in kg)");

        pie.labels().position("outside");

        pie.legend().title().enabled(true);
        pie.legend().title()
                .text("Retail channels")
                .padding(0d, 0d, 10d, 0d);*/

        pie.legend()
                .position("center-bottom")
                .itemsLayout(LegendLayout.HORIZONTAL)
                .align(Align.CENTER);

        this.requireActivity().runOnUiThread(() ->
                anyChartView.setChart(pie));
    }
}