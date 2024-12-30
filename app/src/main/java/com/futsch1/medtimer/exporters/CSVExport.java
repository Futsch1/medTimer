package com.futsch1.medtimer.exporters;

import android.content.Context;

import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.helpers.TableHelper;
import com.futsch1.medtimer.helpers.TimeHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CSVExport implements Exporter {
    private final List<ReminderEvent> reminderEvents;
    private final Context context;

    public CSVExport(List<ReminderEvent> reminderEvents, Context context) {
        this.reminderEvents = reminderEvents;
        this.context = context;
    }

    public void export(File file) throws ExporterException {
        try (FileWriter csvFile = new FileWriter(file)) {
            List<String> headerTexts = TableHelper.getTableHeaders(context);
            csvFile.write(String.join(";", headerTexts) + "\n");

            for (ReminderEvent reminderEvent : reminderEvents) {
                String line = String.format("%s;%s;%s;%s;%s;%s\n",
                        TimeHelper.toLocalizedDatetimeString(context, reminderEvent.remindedTimestamp),
                        reminderEvent.medicineName,
                        reminderEvent.amount,
                        reminderEvent.status == ReminderEvent.ReminderStatus.TAKEN ?
                                TimeHelper.toLocalizedDatetimeString(context, reminderEvent.processedTimestamp) : "",
                        TimeHelper.toISO8601DatetimeString(reminderEvent.remindedTimestamp),
                        reminderEvent.status == ReminderEvent.ReminderStatus.TAKEN ?
                                TimeHelper.toISO8601DatetimeString(reminderEvent.processedTimestamp) : ""
                );
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
