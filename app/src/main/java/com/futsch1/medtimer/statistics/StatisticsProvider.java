package com.futsch1.medtimer.statistics;

import android.content.Context;

import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.ReminderEvent;

import java.util.ArrayList;
import java.util.List;

public class StatisticsProvider {
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
}
