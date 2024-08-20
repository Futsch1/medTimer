package com.futsch1.medtimer.statistics;

import androidx.annotation.NonNull;

import com.androidplot.xy.SimpleXYSeries;
import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.helpers.MedicineHelper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsProvider {
    private final List<ReminderEvent> reminderEvents;

    public StatisticsProvider(MedicineRepository medicineRepository) {
        reminderEvents = medicineRepository.getAllReminderEventsWithoutDeleted();
    }

    public TakenSkipped getTakenSkippedData(int days) {
        long taken = reminderEvents.stream().filter(event -> eventStatusDaysFilter(event, days, ReminderEvent.ReminderStatus.TAKEN)).count();
        long skipped = reminderEvents.stream().filter(event -> eventStatusDaysFilter(event, days, ReminderEvent.ReminderStatus.SKIPPED)).count();
        return new TakenSkipped(taken, skipped);
    }

    private boolean eventStatusDaysFilter(ReminderEvent event, int days, ReminderEvent.ReminderStatus status) {
        return event.status == status && (days == 0 || wasAfter(event.remindedTimestamp, LocalDate.now().minusDays(days)));
    }

    private boolean wasAfter(long secondsSinceEpoch, LocalDate date) {
        Instant instant = Instant.ofEpochSecond(secondsSinceEpoch);
        return instant.atZone(ZoneId.systemDefault()).toLocalDate().isAfter(date);
    }

    public List<SimpleXYSeries> getLastDaysReminders(int days) {
        Map<String, int[]> medicineToDayCount = calculateMedicineToDayMap(days);
        return calculateDataEntries(days, medicineToDayCount);
    }

    private Map<String, int[]> calculateMedicineToDayMap(int days) {
        Map<String, int[]> medicineToDayCount = new HashMap<>();
        LocalDate earliestDate = LocalDate.now().minusDays(days);
        for (ReminderEvent event : reminderEvents) {
            incrementMedicineToDayCountForEvent(days, event, earliestDate, medicineToDayCount);
        }

        return medicineToDayCount;
    }

    /**
     * @noinspection DataFlowIssue
     */
    @NonNull
    private static List<SimpleXYSeries> calculateDataEntries(int days, Map<String, int[]> medicineToDayCount) {
        List<SimpleXYSeries> data = new ArrayList<>();
        int seriesCount = medicineToDayCount.size();
        if (seriesCount == 0) {
            return data;
        }

        List<String> medicineNames = new ArrayList<>(medicineToDayCount.keySet());
        for (int j = 0; j < seriesCount; j++) {
            List<Number> xValues = new ArrayList<>();
            List<Number> yValues = new ArrayList<>();
            for (int i = days - 1; i >= 0; i--) {
                xValues.add(LocalDate.now().toEpochDay() - i);
                yValues.add(medicineToDayCount.get(medicineNames.get(j))[i]);
            }
            data.add(new SimpleXYSeries(xValues, yValues, medicineNames.get(j)));
        }
        return data;
    }

    /**
     * @noinspection DataFlowIssue
     */
    private void incrementMedicineToDayCountForEvent(int days, ReminderEvent event, LocalDate earliestDate, Map<String, int[]> medicineToDayCount) {
        if (event.status == ReminderEvent.ReminderStatus.TAKEN && wasAfter(event.remindedTimestamp, earliestDate)) {
            String medicineName = MedicineHelper.normalizeMedicineName(event.medicineName);
            medicineToDayCount.computeIfAbsent(medicineName, k -> new int[days]);
            final int daysInThePast = getDaysInThePast(event.remindedTimestamp);
            if (daysInThePast >= 0 && daysInThePast < medicineToDayCount.get(medicineName).length) {
                medicineToDayCount.get(medicineName)[daysInThePast]++;
            }
        }
    }

    private int getDaysInThePast(long secondsSinceEpoch) {
        Instant instant = Instant.ofEpochSecond(secondsSinceEpoch);
        return (int) (LocalDate.now().toEpochDay() - instant.atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay());
    }

    public record TakenSkipped(long taken, long skipped) {
        // Standard record, no content required
    }
}
