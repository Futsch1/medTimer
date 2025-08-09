package com.futsch1.medtimer.exporters;

import android.content.Context;

import androidx.fragment.app.FragmentManager;

import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.helpers.TableHelper;
import com.futsch1.medtimer.helpers.TimeHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CSVExport extends Exporter {
    private final List<ReminderEvent> reminderEvents;
    private final Context context;

    public CSVExport(List<ReminderEvent> reminderEvents, FragmentManager fragmentManager, Context context) {
        super(fragmentManager);
        this.reminderEvents = reminderEvents;
        this.context = context;
    }

    @SuppressWarnings({"java:S6300", "java:S3457"}) // Unencrypted file is intended here and not a mistake. We need the \n linebreak explicitly
    public void exportInternal(File file) throws ExporterException {
        try (FileWriter csvFile = new FileWriter(file)) {
            List<String> headerTexts = TableHelper.getTableHeadersForExport(context);
            csvFile.write(String.join(";", headerTexts) + "\n");

            for (ReminderEvent reminderEvent : reminderEvents) {
                String line = String.format("%s;%s;%s;%s;%s;%s;%s;%s;%s\n",
                        TimeHelper.toLocalizedDatetimeString(context, reminderEvent.remindedTimestamp),
                        reminderEvent.medicineName,
                        reminderEvent.amount,
                        reminderEvent.status == ReminderEvent.ReminderStatus.TAKEN ?
                                TimeHelper.toLocalizedDatetimeString(context, reminderEvent.processedTimestamp) : "",
                        String.join(", ", reminderEvent.tags),
                        TimeHelper.minutesToDurationString(reminderEvent.lastIntervalReminderTimeInMinutes),
                        reminderEvent.notes,
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
