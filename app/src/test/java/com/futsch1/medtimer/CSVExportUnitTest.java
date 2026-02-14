package com.futsch1.medtimer;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.text.format.DateFormat;

import androidx.fragment.app.FragmentManager;

import com.futsch1.medtimer.database.FullMedicine;
import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.exporters.CSVEventExport;
import com.futsch1.medtimer.exporters.CSVMedicineExport;
import com.futsch1.medtimer.exporters.Export;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

class CSVExportUnitTest {

    public static final String EXCEPTION_OCCURRED = "Exception occurred";

    // create CSV file with correct headers and data for a list of ReminderEvents
    @Test
    void testCreateCsvFileWithCorrectHeadersAndData() {
        // Create a list of ReminderEvents
        List<ReminderEvent> reminderEvents = new ArrayList<>();
        ReminderEvent reminderEvent1 = new ReminderEvent();
        reminderEvent1.remindedTimestamp = 1620000000; // Set remindedTimestamp to a specific value
        reminderEvent1.processedTimestamp = 1620000120;
        reminderEvent1.medicineName = "Medicine 1";
        reminderEvent1.amount = "10mg";
        reminderEvent1.status = ReminderEvent.ReminderStatus.TAKEN;
        reminderEvent1.lastIntervalReminderTimeInMinutes = 134;
        reminderEvent1.tags = Arrays.asList("Tag1", "Tag2");
        reminderEvent1.notes = "Notes";
        reminderEvents.add(reminderEvent1);

        ReminderEvent reminderEvent2 = new ReminderEvent();
        reminderEvent2.remindedTimestamp = 1620001800; // Set remindedTimestamp to a specific value
        reminderEvent2.processedTimestamp = 1620001980;
        reminderEvent2.medicineName = "Medicine 2";
        reminderEvent2.amount = "20mg";
        reminderEvent2.status = ReminderEvent.ReminderStatus.SKIPPED;
        reminderEvent2.tags = new ArrayList<>();
        reminderEvents.add(reminderEvent2);

        // Create a mock Context
        Context context = mock(Context.class);
        when(context.getString(R.string.reminded)).thenReturn("Reminded");
        when(context.getString(R.string.name)).thenReturn("Name");
        when(context.getString(R.string.dosage)).thenReturn("Amount");
        when(context.getString(R.string.taken)).thenReturn("Taken");
        when(context.getString(R.string.tags)).thenReturn("Tags");
        when(context.getString(R.string.interval)).thenReturn("Interval");
        when(context.getString(R.string.notes)).thenReturn("Notes");

        // Create a mock File
        File file = mock(File.class);
        TimeZone utc = TimeZone.getTimeZone("WET");
        java.text.DateFormat usDateFormat = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, Locale.US);
        java.text.DateFormat usTimeFormat = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, Locale.US);
        usTimeFormat.setTimeZone(utc);
        usDateFormat.setTimeZone(utc);

        try (MockedConstruction<FileWriter> fileWriterMockedConstruction = Mockito.mockConstruction(FileWriter.class);
             MockedStatic<DateFormat> dateAccessMockedStatic = mockStatic(DateFormat.class)) {
            dateAccessMockedStatic.when(() -> DateFormat.getDateFormat(any())).thenReturn(usDateFormat);
            dateAccessMockedStatic.when(() -> DateFormat.getTimeFormat(any())).thenReturn(usTimeFormat);
            FragmentManager fragmentManager = mock(FragmentManager.class);

            // Create the CSVCreator object
            CSVEventExport csvEventExport = new CSVEventExport(reminderEvents, fragmentManager, context);

            try {
                // Call the create method
                csvEventExport.exportInternal(file);

                FileWriter fileWriter = fileWriterMockedConstruction.constructed().get(0);

                // Verify that the FileWriter wrote the correct data to the file
                verify(fileWriter).write("Reminded;Name;Amount;Taken;Tags;Interval;Notes;Reminded (ISO 8601);Taken (ISO 8601)\n");
                verify(fileWriter).write("5/3/21 1:00 AM;Medicine 1;10mg;5/3/21 1:02 AM;Tag1, Tag2;2:14;Notes;2021-05-03T00:00:00Z;2021-05-03T00:02:00Z\n");
                verify(fileWriter).write("5/3/21 1:30 AM;Medicine 2;20mg;;;0:00;;2021-05-03T00:30:00Z;\n");
            } catch (Export.ExporterException | IOException e) {
                fail(EXCEPTION_OCCURRED);
            }
        }
    }

    // create CSV file with correct headers and data for a list of ReminderEvents
    @Test
    void testCreateMedicineCsvFileWithCorrectHeadersAndData() {
        // Create a list of Medicines
        List<FullMedicine> medicines = new ArrayList<>();
        FullMedicine medicine1 = new FullMedicine();
        medicine1.medicine = new Medicine("Medicine 1");
        medicine1.reminders = new ArrayList<>();
        Reminder reminder1 = new Reminder(1);
        reminder1.timeInMinutes = 60;
        reminder1.amount = "1";
        medicine1.reminders.add(reminder1);
        medicines.add(medicine1);

        FullMedicine medicine2 = new FullMedicine();
        medicine2.medicine = new Medicine("Medicine 2");
        medicine2.reminders = new ArrayList<>();
        Reminder reminder2 = new Reminder(2);
        reminder2.timeInMinutes = 61;
        reminder2.amount = "2";
        medicine2.reminders.add(reminder2);
        Reminder reminder3 = new Reminder(2);
        reminder3.timeInMinutes = 62;
        reminder3.amount = "three";
        medicine2.reminders.add(reminder3);
        medicines.add(medicine2);

        // Create a mock Context
        Context context = mock(Context.class);
        when(context.getString(R.string.tab_medicine)).thenReturn("Medicine");
        when(context.getString(R.string.dosage)).thenReturn("Amount");
        when(context.getString(R.string.time)).thenReturn("Time");
        when(context.getString(R.string.every_day)).thenReturn("Every day");

        // Create a mock File
        File file = mock(File.class);
        TimeZone utc = TimeZone.getTimeZone("UTC");
        java.text.DateFormat usTimeFormat = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, Locale.US);
        usTimeFormat.setTimeZone(utc);

        try (MockedConstruction<FileWriter> fileWriterMockedConstruction = Mockito.mockConstruction(FileWriter.class);
             MockedStatic<DateFormat> dateAccessMockedStatic = mockStatic(DateFormat.class);
             MockedStatic<TimeZone> timeZoneMockedStatic = mockStatic(TimeZone.class)) {
            FragmentManager fragmentManager = mock(FragmentManager.class);
            dateAccessMockedStatic.when(() -> DateFormat.getTimeFormat(any())).thenReturn(usTimeFormat);
            timeZoneMockedStatic.when(TimeZone::getDefault).thenReturn(utc);

            // Create the CSVCreator object
            CSVMedicineExport csvExport = new CSVMedicineExport(medicines, fragmentManager, context);

            try {
                // Call the create method
                csvExport.exportInternal(file);

                FileWriter fileWriter = fileWriterMockedConstruction.constructed().get(0);

                // Verify that the FileWriter wrote the correct data to the file
                verify(fileWriter).write("Medicine;Amount;Time\n");
                verify(fileWriter).write("Medicine 1;1;1:00 AM, Every day\n");
                verify(fileWriter).write("Medicine 2;2;1:01 AM, Every day\n");
                verify(fileWriter).write("Medicine 2;three;1:02 AM, Every day\n");
            } catch (Export.ExporterException | IOException e) {
                fail(EXCEPTION_OCCURRED);
            }
        }
    }


    // handle empty list of ReminderEvents
    @Test
    void testHandleEmptyListOfReminderEvents() {
        // Create an empty list of ReminderEvents
        List<ReminderEvent> reminderEvents = new ArrayList<>();

        // Create a mock Context
        Context context = mock(Context.class);
        when(context.getString(R.string.reminded)).thenReturn("Reminded");
        when(context.getString(R.string.name)).thenReturn("Name");
        when(context.getString(R.string.dosage)).thenReturn("Amount");
        when(context.getString(R.string.taken)).thenReturn("Taken");
        when(context.getString(R.string.tags)).thenReturn("Tags");
        when(context.getString(R.string.interval)).thenReturn("Interval");
        when(context.getString(R.string.notes)).thenReturn("Notes");

        // Create a mock File
        File file = mock(File.class);

        try (MockedConstruction<FileWriter> fileWriterMockedConstruction = Mockito.mockConstruction(FileWriter.class)) {
            FragmentManager fragmentManager = mock(FragmentManager.class);
            // Create the CSVCreator object
            CSVEventExport csvEventExport = new CSVEventExport(reminderEvents, fragmentManager, context);

            try {
                // Call the create method
                csvEventExport.exportInternal(file);

                FileWriter fileWriter = fileWriterMockedConstruction.constructed().get(0);

                // Verify that the FileWriter wrote the correct data to the file
                verify(fileWriter).write("Reminded;Name;Amount;Taken;Tags;Interval;Notes;Reminded (ISO 8601);Taken (ISO 8601)\n");
            } catch (Export.ExporterException | IOException e) {
                fail(EXCEPTION_OCCURRED);
            }
        }
    }

}