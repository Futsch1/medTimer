package com.futsch1.medtimer.exporters;

import android.content.Context;

import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.helpers.TimeHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.ZoneId;
import java.util.List;

public class CSVExport implements Exporter {
    private final List<ReminderEvent> reminderEvents;
    private final Context context;
    private final ZoneId defaultZoneId;

    public CSVExport(List<ReminderEvent> reminderEvents, Context context, ZoneId zoneId) {
        this.reminderEvents = reminderEvents;
        this.context = context;
        this.defaultZoneId = zoneId;
    }

    public void export(File file) throws ExporterException {
        try (FileWriter csvFile = new FileWriter(file)) {
            csvFile.write(String.format("%s;%s;%s;%s\n",
                    context.getString(R.string.time),
                    context.getString(R.string.medicine_name),
                    context.getString(R.string.dosage),
                    context.getString(R.string.taken)));

            for (ReminderEvent reminderEvent : reminderEvents) {
                String line = String.format("%s;%s;%s;%s\n",
                        TimeHelper.toLocalizedTimeString(reminderEvent.remindedTimestamp, defaultZoneId),
                        reminderEvent.medicineName,
                        reminderEvent.amount,
                        reminderEvent.status == ReminderEvent.ReminderStatus.TAKEN ? "x" : "");
                csvFile.write(line);
            }
        } catch (IOException e) {
            throw new ExporterException();
        }
    }

    @Override
    public String getExtension() {
        return "csv";
    }
}
