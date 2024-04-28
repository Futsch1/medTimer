package com.futsch1.medtimer.statistics;

import android.content.Context;

import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.helpers.TimeHelper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class StatisticsProvider {
    private static final Pattern CYCLIC_COUNT = Pattern.compile(" (\\(\\d?/\\d?)\\)");
    private final List<ReminderEvent> reminderEvents;
    private final Context context;

    public StatisticsProvider(MedicineRepository medicineRepository, Context context) {
        reminderEvents = medicineRepository.getAllReminderEvents();
        this.context = context;
    }

    public List<DataEntry> getTakenSkippedData() {
        List<DataEntry> data = new ArrayList<>();
        long taken = reminderEvents.stream().filter(event -> event.status == ReminderEvent.ReminderStatus.TAKEN).count();
        long skipped = reminderEvents.stream().filter(event -> event.status == ReminderEvent.ReminderStatus.SKIPPED).count();
        data.add(new ValueDataEntry(context.getString(R.string.taken), taken));
        data.add(new ValueDataEntry(context.getString(R.string.skipped), skipped));
        return data;
    }

    /**
     * @noinspection DataFlowIssue
     */
    public ColumnChartData getLastDaysReminders(int days) {
        List<DataEntry> data = new ArrayList<>();
        // First, build a hash map of the medicines taken in the last days
        LocalDate earliestData = LocalDate.now().minusDays(days);
        Map<String, int[]> medicineToDayCount = new HashMap<>();
        for (ReminderEvent event : reminderEvents) {
            if (wasAfter(event.remindedTimestamp, earliestData)) {
                String medicineName = normalizeMedicineName(event.medicineName);
                medicineToDayCount.computeIfAbsent(medicineName, k -> new int[days]);
                medicineToDayCount.get(medicineName)[getDaysInThePast(event.remindedTimestamp)]++;
            }
        }
        int seriesCount = medicineToDayCount.size();
        List<String> medicineNames = new ArrayList<>(medicineToDayCount.keySet());
        for (int i = days - 1; i >= 0; i--) {
            ValueDataEntry dataEntry = new ValueDataEntry(TimeHelper.daysSinceEpochToDateString(LocalDate.now().toEpochDay() - i), medicineToDayCount.get(medicineNames.get(0))[i]);
            for (int j = 1; j < seriesCount; j++) {
                dataEntry.setValue("value" + j, medicineToDayCount.get(medicineNames.get(j))[i]);
            }
            data.add(dataEntry);
        }

        return new ColumnChartData(data, medicineNames);

    }

    private boolean wasAfter(long secondsSinceEpoch, LocalDate date) {
        Instant instant = Instant.ofEpochSecond(secondsSinceEpoch);
        return instant.atZone(ZoneId.systemDefault()).toLocalDate().isAfter(date);
    }

    private String normalizeMedicineName(String medicineName) {
        return CYCLIC_COUNT.matcher(medicineName).replaceAll("");
    }

    private int getDaysInThePast(long secondsSinceEpoch) {
        Instant instant = Instant.ofEpochSecond(secondsSinceEpoch);
        return (int) (LocalDate.now().toEpochDay() - instant.atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay());
    }

    public record ColumnChartData(List<DataEntry> seriesData, List<String> series) {
    }
}
