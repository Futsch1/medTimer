package com.futsch1.medtimer;

import android.content.Context;

import com.futsch1.medtimer.database.ReminderEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

public class CSVCreator {
    private final List<ReminderEvent> reminderEvents;
    private final Context context;
    private final ZoneId defaultZoneId;

    public CSVCreator(List<ReminderEvent> reminderEvents, Context context, ZoneId zoneId) {
        this.reminderEvents = reminderEvents;
        this.context = context;
        this.defaultZoneId = zoneId;
    }

    public void create(File file) throws IOException {
        try (FileWriter csvFile = new FileWriter(file)) {
            csvFile.write(String.format("%s;%s;%s;%s\n",
                    context.getString(R.string.time),
                    context.getString(R.string.medicine_name),
                    context.getString(R.string.amount),
                    context.getString(R.string.taken)));

            Instant remindedTime;
            ZonedDateTime zonedDateTime;
            for (ReminderEvent reminderEvent : reminderEvents) {
                remindedTime = Instant.ofEpochSecond(reminderEvent.remindedTimestamp);
                zonedDateTime = remindedTime.atZone(defaultZoneId);
                String line = String.format("%s %s;%s;%s;%s\n",
                        zonedDateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)),
                        zonedDateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
                        reminderEvent.medicineName,
                        reminderEvent.amount,
                        reminderEvent.status == ReminderEvent.ReminderStatus.TAKEN ? "x" : "");
                csvFile.write(line);
            }
        }
    }
}
