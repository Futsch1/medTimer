package com.futsch1.medtimer

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import androidx.fragment.app.FragmentManager
import com.futsch1.medtimer.database.ReminderRepository
import com.futsch1.medtimer.exporters.CSVEventExport
import com.futsch1.medtimer.exporters.CSVMedicineExport
import com.futsch1.medtimer.exporters.Export.ExporterException
import com.futsch1.medtimer.helpers.LocaleContextAccessor
import com.futsch1.medtimer.helpers.ReminderSummaryFormatter
import com.futsch1.medtimer.helpers.TimeFormatter
import com.futsch1.medtimer.medicine.LinkedReminderAlgorithms
import com.futsch1.medtimer.model.Medicine
import com.futsch1.medtimer.model.Reminder
import com.futsch1.medtimer.model.ReminderEvent
import com.futsch1.medtimer.model.UserPreferences
import com.futsch1.medtimer.preferences.PreferencesDataSource
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.time.LocalTime
import kotlin.test.fail

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@HiltAndroidTest
class CSVExportUnitTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @BindValue
    val mockPreferenceDataSource: PreferencesDataSource = mock()

    @BindValue
    val boundAlarmManager: AlarmManager = mock()

    @BindValue
    val boundNotificationManager: NotificationManager = mock()

    private lateinit var mockReminderRepository: ReminderRepository
    private lateinit var timeFormatter: TimeFormatter
    private lateinit var reminderSummaryFormatter: ReminderSummaryFormatter
    private lateinit var context: Context

    @Before
    fun setUp() {
        hiltRule.inject()
        mockReminderRepository = mock()
        context = RuntimeEnvironment.getApplication()
        val preferences = MutableStateFlow(UserPreferences.default())
        `when`(mockPreferenceDataSource.preferences).thenReturn(preferences)
        timeFormatter = TimeFormatter(context, mockPreferenceDataSource, LocaleContextAccessor(context))
        reminderSummaryFormatter = ReminderSummaryFormatter(context, mockReminderRepository, timeFormatter)
    }

    // create CSV file with correct headers and data for a list of ReminderEvents
    @Test
    fun testCreateCsvFileWithCorrectHeadersAndData() {
        val reminderEvents = listOf(
            ReminderEvent.default().copy(
                remindedTimestamp = java.time.Instant.ofEpochSecond(1620000000),
                processedTimestamp = java.time.Instant.ofEpochSecond(1620000120),
                medicineName = "Medicine 1",
                amount = "10mg",
                status = ReminderEvent.ReminderStatus.TAKEN,
                lastIntervalReminderTimeInMinutes = 134,
                tags = listOf("Tag1", "Tag2"),
                notes = "Notes"
            ),
            ReminderEvent.default().copy(
                remindedTimestamp = java.time.Instant.ofEpochSecond(1620001800),
                processedTimestamp = java.time.Instant.ofEpochSecond(1620001980),
                medicineName = "Medicine 2",
                amount = "20mg",
                status = ReminderEvent.ReminderStatus.SKIPPED
            )
        )
        // Create a mock File
        val file = mock<File>()

        val fragmentManager =
            mock<FragmentManager>()

        // Create the CSVCreator object
        val csvEventExport =
            CSVEventExport(
                reminderEvents,
                fragmentManager,
                context,
                Dispatchers.Unconfined,
                timeFormatter
            )

        Mockito.mockConstruction(FileWriter::class.java).use { fileWriterMockedConstruction ->
            try {
                // Call the create method
                runBlocking { csvEventExport.exportInternal(file) }

                val fileWriter = fileWriterMockedConstruction.constructed().first()

                // Verify that the FileWriter wrote the correct data to the file
                Mockito.verify(fileWriter)
                    .write("Reminded;Name;Dosage;Taken;Tags;Interval;Notes;Reminded (ISO 8601);Taken (ISO 8601)\n")
                Mockito.verify(fileWriter)
                    .write("5/3/21 2:00 AM;Medicine 1;10mg;5/3/21 2:02 AM;Tag1, Tag2;2:14;Notes;2021-05-03T00:00:00Z;2021-05-03T00:02:00Z\n")
                Mockito.verify(fileWriter)
                    .write("5/3/21 2:30 AM;Medicine 2;20mg;;;0:00;;2021-05-03T00:30:00Z;\n")
            } catch (_: ExporterException) {
                fail(EXCEPTION_OCCURRED)
            } catch (_: IOException) {
                fail(EXCEPTION_OCCURRED)
            }
        }
    }

    // create CSV file with correct headers and data for a list of ReminderEvents
    @Test
    fun testCreateMedicineCsvFileWithCorrectHeadersAndData() {
        val firstReminder = Reminder.default().copy(medicineRelId = 1, time = LocalTime.of(1, 0), amount = "1")
        val secondReminder = Reminder.default().copy(medicineRelId = 1, time = LocalTime.of(1, 1), amount = "2")
        val thirdReminder = Reminder.default().copy(medicineRelId = 2, time = LocalTime.of(1, 2), amount = "three")

        // Create a list of Medicines
        val medicines = listOf(
            Medicine.default().copy(
                name = "Medicine 1",
                reminders = listOf(
                    firstReminder,
                    thirdReminder
                )
            ),
            Medicine.default().copy(
                name = "Medicine 2",
                reminders = listOf(
                    secondReminder,
                    thirdReminder
                )
            )
        )

        // Create a mock File
        val file = mock<File>()
        val fragmentManager =
            mock<FragmentManager>()

        Mockito.mockConstruction(FileWriter::class.java).use { fileWriterMockedConstruction ->

            val csvExport =
                CSVMedicineExport(medicines, fragmentManager, context, reminderSummaryFormatter, Dispatchers.Unconfined, LinkedReminderAlgorithms())
            try {
                // Call the create method
                runBlocking { csvExport.exportInternal(file) }

                val fileWriter =
                    fileWriterMockedConstruction.constructed().first()

                // Verify that the FileWriter wrote the correct data to the file
                Mockito.verify(fileWriter)
                    .write("Medicine;Dosage;Time\n")
                Mockito.verify(fileWriter)
                    .write("Medicine 1;1;1:00 AM, Every day\n")
                Mockito.verify(fileWriter)
                    .write("Medicine 2;2;1:01 AM, Every day\n")
                Mockito.verify(fileWriter)
                    .write("Medicine 2;three;1:02 AM, Every day\n")
            } catch (_: ExporterException) {
                fail(EXCEPTION_OCCURRED)
            } catch (_: IOException) {
                fail(EXCEPTION_OCCURRED)
            }
        }
    }


    // handle empty list of ReminderEvents
    @Test
    fun testHandleEmptyListOfReminderEvents() {

        // Create a mock File
        val file = mock<File>()

        Mockito.mockConstruction(FileWriter::class.java)
            .use { fileWriterMockedConstruction ->
                val fragmentManager = mock<FragmentManager>()
                // Create the CSVCreator object
                val csvEventExport = CSVEventExport(
                    emptyList(),
                    fragmentManager,
                    context,
                    Dispatchers.Unconfined,
                    timeFormatter
                )
                try {
                    // Call the create method
                    runBlocking { csvEventExport.exportInternal(file) }

                    val fileWriter = fileWriterMockedConstruction.constructed().first()

                    // Verify that the FileWriter wrote the correct data to the file
                    Mockito.verify(fileWriter)
                        .write("Reminded;Name;Dosage;Taken;Tags;Interval;Notes;Reminded (ISO 8601);Taken (ISO 8601)\n")
                } catch (_: ExporterException) {
                    fail(EXCEPTION_OCCURRED)
                } catch (_: IOException) {
                    fail(EXCEPTION_OCCURRED)
                }
            }
    }

    companion object {
        const val EXCEPTION_OCCURRED: String = "Exception occurred"
    }
}
