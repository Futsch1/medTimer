package com.futsch1.medtimer

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.exporters.CSVEventExport
import com.futsch1.medtimer.exporters.CSVMedicineExport
import com.futsch1.medtimer.exporters.Export.ExporterException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.DateFormat
import java.util.Locale
import java.util.TimeZone

internal class CSVExportUnitTest {
    // create CSV file with correct headers and data for a list of ReminderEvents
    @Test
    fun testCreateCsvFileWithCorrectHeadersAndData() {
        val reminderEvents = mutableListOf(
            ReminderEvent().apply {
                // Set remindedTimestamp to a specific value
                remindedTimestamp = 1620000000
                processedTimestamp = 1620000120
                medicineName = "Medicine 1"
                amount = "10mg"
                status = ReminderEvent.ReminderStatus.TAKEN
                lastIntervalReminderTimeInMinutes = 134
                tags = mutableListOf("Tag1", "Tag2")
                notes = "Notes"
            },
            ReminderEvent().apply {
                // Set remindedTimestamp to a specific value
                remindedTimestamp = 1620001800
                processedTimestamp = 1620001980
                medicineName = "Medicine 2"
                amount = "20mg"
                status = ReminderEvent.ReminderStatus.SKIPPED
                tags = emptyList()
            }
        )

        // Create a mock Context
        val context = Mockito.mock(Context::class.java).apply {
            Mockito.`when`(getString(R.string.reminded)).thenReturn("Reminded")
            Mockito.`when`(getString(R.string.name)).thenReturn("Name")
            Mockito.`when`(getString(R.string.dosage)).thenReturn("Amount")
            Mockito.`when`(getString(R.string.taken)).thenReturn("Taken")
            Mockito.`when`(getString(R.string.tags)).thenReturn("Tags")
            Mockito.`when`(getString(R.string.interval)).thenReturn("Interval")
            Mockito.`when`(getString(R.string.notes)).thenReturn("Notes")
        }

        // Create a mock File
        val file = Mockito.mock(File::class.java)
        val utc = TimeZone.getTimeZone("WET")

        val usDateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US)
        usDateFormat.timeZone = utc

        val usTimeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US)
        usTimeFormat.timeZone = utc

        val fragmentManager =
            Mockito.mock(FragmentManager::class.java)

        // Create the CSVCreator object
        val csvEventExport =
            CSVEventExport(reminderEvents, fragmentManager, context)

        Mockito.mockConstruction(FileWriter::class.java).use { fileWriterMockedConstruction ->
            Mockito.mockStatic(android.text.format.DateFormat::class.java)
                .use { dateAccessMockedStatic ->
                    dateAccessMockedStatic.`when`<Any> {
                        android.text.format.DateFormat.getDateFormat(
                            ArgumentMatchers.any()
                        )
                    }.thenReturn(usDateFormat)
                    dateAccessMockedStatic.`when`<Any> {
                        android.text.format.DateFormat.getTimeFormat(
                            ArgumentMatchers.any()
                        )
                    }.thenReturn(usTimeFormat)


                    try {
                        // Call the create method
                        csvEventExport.exportInternal(file)

                        val fileWriter = fileWriterMockedConstruction.constructed().first()

                        // Verify that the FileWriter wrote the correct data to the file
                        Mockito.verify(fileWriter)
                            .write("Reminded;Name;Amount;Taken;Tags;Interval;Notes;Reminded (ISO 8601);Taken (ISO 8601)\n")
                        Mockito.verify(fileWriter)
                            .write("5/3/21 1:00\u202FAM;Medicine 1;10mg;5/3/21 1:02\u202FAM;Tag1, Tag2;2:14;Notes;2021-05-03T00:00:00Z;2021-05-03T00:02:00Z\n")
                        Mockito.verify(fileWriter)
                            .write("5/3/21 1:30\u202FAM;Medicine 2;20mg;;;0:00;;2021-05-03T00:30:00Z;\n")
                    } catch (_: ExporterException) {
                        Assertions.fail(EXCEPTION_OCCURRED)
                    } catch (_: IOException) {
                        Assertions.fail(EXCEPTION_OCCURRED)
                    }
                }
        }
    }

    // create CSV file with correct headers and data for a list of ReminderEvents
    @Test
    fun testCreateMedicineCsvFileWithCorrectHeadersAndData() {
        val thirdReminder = Reminder(2).apply {
            timeInMinutes = 62
            amount = "three"
        }

        // Create a list of Medicines
        val medicines = listOf(
            FullMedicine().apply {
                medicine = Medicine("Medicine 1")
                reminders = mutableListOf(
                    Reminder(1).apply {
                        timeInMinutes = 60
                        amount = "1"
                    },
                    thirdReminder
                )
            },
            FullMedicine().apply {
                medicine = Medicine("Medicine 2")
                reminders = mutableListOf(
                    Reminder(2).apply {
                        timeInMinutes = 61
                        amount = "2"
                    },
                    thirdReminder
                )
            }
        )

        // Create a mock Context
        val context = Mockito.mock(Context::class.java).apply {
            Mockito.`when`(getString(R.string.tab_medicine)).thenReturn("Medicine")
            Mockito.`when`(getString(R.string.dosage)).thenReturn("Amount")
            Mockito.`when`(getString(R.string.time)).thenReturn("Time")
            Mockito.`when`(getString(R.string.every_day)).thenReturn("Every day")
        }

        // Create a mock File
        val file = Mockito.mock(File::class.java)
        val utc = TimeZone.getTimeZone("UTC")
        val usTimeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US)
        usTimeFormat.timeZone = utc

        Mockito.mockConstruction(FileWriter::class.java).use { fileWriterMockedConstruction ->
            Mockito.mockStatic(android.text.format.DateFormat::class.java)
                .use { dateAccessMockedStatic ->
                    Mockito.mockStatic(
                        TimeZone::class.java
                    ).use { timeZoneMockedStatic ->
                        val fragmentManager =
                            Mockito.mock(FragmentManager::class.java)
                        dateAccessMockedStatic.`when`<Any> {
                            android.text.format.DateFormat.getTimeFormat(
                                ArgumentMatchers.any()
                            )
                        }.thenReturn(usTimeFormat)
                        timeZoneMockedStatic.`when`<Any> { TimeZone.getDefault() }
                            .thenReturn(utc)

                        // Create the CSVCreator object
                        val csvExport = CSVMedicineExport(medicines, fragmentManager, context)
                        try {
                            // Call the create method
                            csvExport.exportInternal(file)

                            val fileWriter =
                                fileWriterMockedConstruction.constructed().first()

                            // Verify that the FileWriter wrote the correct data to the file
                            Mockito.verify(fileWriter)
                                .write("Medicine;Amount;Time\n")
                            Mockito.verify(fileWriter)
                                .write("Medicine 1;1;1:00\u202FAM, Every day\n")
                            Mockito.verify(fileWriter)
                                .write("Medicine 2;2;1:01\u202FAM, Every day\n")
                            Mockito.verify(fileWriter)
                                .write("Medicine 2;three;1:02\u202FAM, Every day\n")
                        } catch (_: ExporterException) {
                            Assertions.fail(EXCEPTION_OCCURRED)
                        } catch (_: IOException) {
                            Assertions.fail(EXCEPTION_OCCURRED)
                        }
                    }
                }
        }
    }


    // handle empty list of ReminderEvents
    @Test
    fun testHandleEmptyListOfReminderEvents() {
        // Create a mock Context
        val context = Mockito.mock(Context::class.java).apply {
            Mockito.`when`(getString(R.string.reminded)).thenReturn("Reminded")
            Mockito.`when`(getString(R.string.name)).thenReturn("Name")
            Mockito.`when`(getString(R.string.dosage)).thenReturn("Amount")
            Mockito.`when`(getString(R.string.taken)).thenReturn("Taken")
            Mockito.`when`(getString(R.string.tags)).thenReturn("Tags")
            Mockito.`when`(getString(R.string.interval)).thenReturn("Interval")
            Mockito.`when`(getString(R.string.notes)).thenReturn("Notes")
        }

        // Create a mock File
        val file = Mockito.mock(File::class.java)

        Mockito.mockConstruction(FileWriter::class.java)
            .use { fileWriterMockedConstruction ->
                val fragmentManager = Mockito.mock(
                    FragmentManager::class.java
                )
                // Create the CSVCreator object
                val csvEventExport = CSVEventExport(emptyList(), fragmentManager, context)
                try {
                    // Call the create method
                    csvEventExport.exportInternal(file)

                    val fileWriter = fileWriterMockedConstruction.constructed().first()

                    // Verify that the FileWriter wrote the correct data to the file
                    Mockito.verify(fileWriter)
                        .write("Reminded;Name;Amount;Taken;Tags;Interval;Notes;Reminded (ISO 8601);Taken (ISO 8601)\n")
                } catch (_: ExporterException) {
                    Assertions.fail(EXCEPTION_OCCURRED)
                } catch (_: IOException) {
                    Assertions.fail(EXCEPTION_OCCURRED)
                }
            }
    }

    companion object {
        const val EXCEPTION_OCCURRED: String = "Exception occurred"
    }
}