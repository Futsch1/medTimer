package com.futsch1.medtimer;

// Generated by CodiumAI

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.exporters.CSVExport;
import com.futsch1.medtimer.exporters.Exporter;

import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class CSVExportUnitTest {


    // create CSV file with correct headers and data for a list of ReminderEvents
    @Test
    public void test_create_csv_file_with_correct_headers_and_data() {
        // Create a list of ReminderEvents
        List<ReminderEvent> reminderEvents = new ArrayList<>();
        ReminderEvent reminderEvent1 = new ReminderEvent();
        reminderEvent1.remindedTimestamp = 1620000000; // Set remindedTimestamp to a specific value
        reminderEvent1.medicineName = "Medicine 1";
        reminderEvent1.amount = "10mg";
        reminderEvent1.status = ReminderEvent.ReminderStatus.TAKEN;
        reminderEvents.add(reminderEvent1);

        ReminderEvent reminderEvent2 = new ReminderEvent();
        reminderEvent2.remindedTimestamp = 1620001800; // Set remindedTimestamp to a specific value
        reminderEvent2.medicineName = "Medicine 2";
        reminderEvent2.amount = "20mg";
        reminderEvent2.status = ReminderEvent.ReminderStatus.SKIPPED;
        reminderEvents.add(reminderEvent2);

        // Create a mock Context
        Context context = mock(Context.class);
        when(context.getString(R.string.time)).thenReturn("Time");
        when(context.getString(R.string.medicine_name)).thenReturn("Medicine Name");
        when(context.getString(R.string.dosage)).thenReturn("Amount");
        when(context.getString(R.string.taken)).thenReturn("Taken");

        // Create a mock File
        File file = mock(File.class);

        try (MockedConstruction<FileWriter> fileWriterMockedConstruction = Mockito.mockConstruction(FileWriter.class)) {
            // Create the CSVCreator object
            CSVExport csvExport = new CSVExport(reminderEvents, context, ZoneId.of("Z"));

            try {
                // Call the create method
                csvExport.export(file);

                FileWriter fileWriter = fileWriterMockedConstruction.constructed().get(0);

                // Verify that the FileWriter wrote the correct data to the file
                verify(fileWriter).write("Time;Medicine Name;Amount;Taken\n");
                verify(fileWriter).write("5/3/21 12:00 AM;Medicine 1;10mg;x\n");
                verify(fileWriter).write("5/3/21 12:30 AM;Medicine 2;20mg;\n");
            } catch (Exporter.ExporterException | IOException e) {
                fail("Exception occurred");
            }
        }
    }


    // handle empty list of ReminderEvents
    @Test
    public void test_handle_empty_list_of_reminder_events() {
        // Create an empty list of ReminderEvents
        List<ReminderEvent> reminderEvents = new ArrayList<>();

        // Create a mock Context
        Context context = mock(Context.class);
        when(context.getString(R.string.time)).thenReturn("Time");
        when(context.getString(R.string.medicine_name)).thenReturn("Medicine Name");
        when(context.getString(R.string.dosage)).thenReturn("Amount");
        when(context.getString(R.string.taken)).thenReturn("Taken");

        // Create a mock File
        File file = mock(File.class);

        try (MockedConstruction<FileWriter> fileWriterMockedConstruction = Mockito.mockConstruction(FileWriter.class)) {
            // Create the CSVCreator object
            CSVExport csvExport = new CSVExport(reminderEvents, context, ZoneId.of("Z"));

            try {
                // Call the create method
                csvExport.export(file);

                FileWriter fileWriter = fileWriterMockedConstruction.constructed().get(0);

                // Verify that the FileWriter wrote the correct data to the file
                verify(fileWriter).write("Time;Medicine Name;Amount;Taken\n");
            } catch (Exporter.ExporterException | IOException e) {
                fail("Exception occurred");
            }
        }
    }

}