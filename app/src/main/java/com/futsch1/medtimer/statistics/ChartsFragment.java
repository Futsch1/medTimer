package com.futsch1.medtimer.statistics;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.compose.ui.platform.ComposeView;
import androidx.compose.ui.platform.ViewCompositionStrategy;
import androidx.fragment.app.Fragment;

import com.futsch1.medtimer.R;
import com.futsch1.medtimer.core.ui.MedicinePerDayData;
import com.futsch1.medtimer.core.ui.MedicineSeriesData;
import com.futsch1.medtimer.core.ui.TakenSkippedData;
import com.futsch1.medtimer.database.FullMedicine;
import com.futsch1.medtimer.database.MedicineRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChartsFragment extends Fragment {
    private static final int[] FALLBACK_COLORS = {
            0xFF003f5c, 0xFF2f4b7c, 0xFF665191, 0xFFa05195,
            0xFFd45087, 0xFFf95d6a, 0xFFff7c43, 0xFFffa600,
            0xFF004c6d, 0xFF295d7d, 0xFF436f8e, 0xFF5b829f,
            0xFF7295b0, 0xFF89a8c2, 0xFFa1bcd4, 0xFFb8d0e6,
            0xFFd0e5f8
    };

    private HandlerThread backgroundThread;
    private MedicineRepository medicineRepository;

    private ComposeView takenSkippedChartView;
    private ComposeView takenSkippedTotalChartView;
    private ComposeView medicinesPerDayChartView;

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

        medicinesPerDayChartView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed.INSTANCE);
        takenSkippedChartView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed.INSTANCE);
        takenSkippedTotalChartView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed.INSTANCE);

        return statisticsView;
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
            List<StatisticsProvider.MedicinePerDaySeries> seriesList = statisticsProvider.getLastDaysReminders(days);
            List<FullMedicine> medicines = medicineRepository.getMedicines();
            MedicinePerDayData chartData = buildMedicinePerDayData(seriesList, medicines);
            requireActivity().runOnUiThread(() ->
                    MedicinePerDayChartBridge.setChartContent(medicinesPerDayChartView, chartData));

            StatisticsProvider.TakenSkipped data = statisticsProvider.getTakenSkippedData(days);
            requireActivity().runOnUiThread(() -> updateTakenSkippedChart(takenSkippedChartView, data, days));

            StatisticsProvider.TakenSkipped dataTotal = statisticsProvider.getTakenSkippedData(0);
            requireActivity().runOnUiThread(() -> updateTakenSkippedChart(takenSkippedTotalChartView, dataTotal, 0));
        } catch (IllegalStateException e) {
            // Intentionally empty - just don't do anything
        }
    }

    private MedicinePerDayData buildMedicinePerDayData(
            List<StatisticsProvider.MedicinePerDaySeries> seriesList,
            List<FullMedicine> medicines) {
        List<LocalDate> days = seriesList.isEmpty()
                ? List.of()
                : seriesList.get(0).xValues().stream()
                        .map(LocalDate::ofEpochDay)
                        .collect(Collectors.toList());

        Map<String, FullMedicine> medicineMap = medicines.stream()
                .collect(Collectors.toMap(fm -> fm.medicine.name, fm -> fm, (a, b) -> a));

        List<MedicineSeriesData> series = new ArrayList<>();
        int colorIndex = 0;
        for (StatisticsProvider.MedicinePerDaySeries s : seriesList) {
            Integer color = null;
            FullMedicine fm = medicineMap.get(s.name());
            if (fm != null && fm.medicine.useColor) {
                color = fm.medicine.color;
            }
            if (color == null) {
                color = FALLBACK_COLORS[colorIndex % FALLBACK_COLORS.length];
            }
            colorIndex++;
            series.add(new MedicineSeriesData(s.name(), s.yValues(), color));
        }

        String title = "";
        return new MedicinePerDayData(title, days, series);
    }

    private void updateTakenSkippedChart(ComposeView composeView, StatisticsProvider.TakenSkipped data, int chartDays) {
        String title;
        if (chartDays != 0) {
            title = getResources().getQuantityString(R.plurals.last_n_days, chartDays, chartDays);
        } else {
            title = getString(R.string.total);
        }

        TakenSkippedData chartData = new TakenSkippedData(data.taken(), data.skipped(), title);
        TakenSkippedChartBridge.setChartContent(composeView, chartData);
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
