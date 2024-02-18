package com.futsch1.medtimer;

import android.content.Context;

import com.futsch1.medtimer.database.ReminderEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.TimeZone;

public class CSVCreator {
    private final List<ReminderEvent> reminderEvents;
    private final Context context;

    public CSVCreator(List<ReminderEvent> reminderEvents, Context context) {
        this.reminderEvents = reminderEvents;
        this.context = context;
    }

    public void create(File file) throws IOException {
        try (FileWriter csvFile = new FileWriter(file)) {
            csvFile.write(String.format("%s;%s;%s;%s\n",
                    context.getString(R.string.time),
                    context.getString(R.string.medicine_name),
                    context.getString(R.string.amount),
                    context.getString(R.string.taken)));

            for (ReminderEvent reminderEvent : reminderEvents) {
                Instant remindedTime = Instant.ofEpochSecond(reminderEvent.remindedTimestamp);
                ZonedDateTime zonedDateTime = remindedTime.atZone(TimeZone.getDefault().toZoneId());
                csvFile.write(String.format("%s;", zonedDateTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))));
                csvFile.write(String.format("%s;", reminderEvent.medicineName));
                csvFile.write(String.format("%s;", reminderEvent.amount));
                csvFile.write(String.format("%s\n", reminderEvent.status == ReminderEvent.ReminderStatus.TAKEN ? "x" : ""));
            }
        }
    }
}
