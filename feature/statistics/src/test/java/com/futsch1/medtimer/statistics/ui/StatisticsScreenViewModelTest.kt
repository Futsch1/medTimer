package com.futsch1.medtimer.statistics.ui

import androidx.compose.ui.graphics.Color
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.database.ReminderEvent.ReminderStatus
import com.futsch1.medtimer.database.statusValuesWithoutDeletedAndAcknowledged
import com.futsch1.medtimer.statistics.domain.AnalysisDays
import com.futsch1.medtimer.statistics.domain.AnalysisDaysPreference
import com.futsch1.medtimer.statistics.domain.GetCalendarEventsUseCase
import com.futsch1.medtimer.statistics.domain.MedicinePerDaySeries
import com.futsch1.medtimer.statistics.domain.StatisticsTabPreference
import com.futsch1.medtimer.statistics.domain.StatisticsTabType
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class StatisticsScreenViewModelTest {

    private lateinit var viewModel: StatisticsScreenViewModel

    @Before
    fun setUp() {
        val mockAnalysisDaysPreference = mock(AnalysisDaysPreference::class.java)
        `when`(mockAnalysisDaysPreference.analysisDays).thenReturn(AnalysisDays.DEFAULT)

        val mockStatisticsTabPreference = mock(StatisticsTabPreference::class.java)
        `when`(mockStatisticsTabPreference.activeFragment).thenReturn(StatisticsTabType.CHARTS)

        val mockRepository = mock(MedicineRepository::class.java)
        `when`(mockRepository.getReminderEventsFlow(0, statusValuesWithoutDeletedAndAcknowledged))
            .thenReturn(emptyFlow())

        viewModel = StatisticsScreenViewModel(
            repository = mockRepository,
            analysisDaysPreference = mockAnalysisDaysPreference,
            statisticsTabPreference = mockStatisticsTabPreference,
            getCalendarEvents = mock(GetCalendarEventsUseCase::class.java),
        )
    }

    // --- loadTableData tests ---

    @Test
    fun `loadTableData maps ReminderEvent fields correctly`() {
        val zone = ZoneId.of("UTC")
        val remindedTimestamp = 1705312500L // 2024-01-15T08:55:00Z
        val processedTimestamp = 1705312800L // 2024-01-15T09:00:00Z

        val event = ReminderEvent().apply {
            reminderEventId = 42
            medicineName = "Aspirin"
            amount = "100mg"
            status = ReminderStatus.TAKEN
            this.remindedTimestamp = remindedTimestamp
            this.processedTimestamp = processedTimestamp
        }

        invokeLoadTableData(listOf(event), zone)

        val rows = viewModel.state.tableRows
        assertEquals(1, rows.size)

        val row = rows[0]
        assertEquals(42, row.eventId)
        assertEquals("Aspirin", row.medicineName)
        assertEquals("100mg", row.dosage)
        assertEquals(ReminderStatus.TAKEN, row.takenStatus)
        assertEquals(
            Instant.ofEpochSecond(remindedTimestamp).atZone(zone).toLocalDateTime(),
            row.remindedAt
        )
        assertNotNull(row.takenAt)
        assertEquals(
            Instant.ofEpochSecond(processedTimestamp).atZone(zone).toLocalDateTime(),
            row.takenAt
        )
    }

    @Test
    fun `loadTableData sets takenAt to null for non-TAKEN status`() {
        val zone = ZoneId.of("UTC")
        val event = ReminderEvent().apply {
            reminderEventId = 1
            medicineName = "Med"
            amount = "50mg"
            status = ReminderStatus.SKIPPED
            remindedTimestamp = 1705312500L
            processedTimestamp = 1705312800L
        }

        invokeLoadTableData(listOf(event), zone)

        val row = viewModel.state.tableRows[0]
        assertNull(row.takenAt)
        assertEquals(ReminderStatus.SKIPPED, row.takenStatus)
    }

    @Test
    fun `loadTableData sets takenAt to null for RAISED status`() {
        val zone = ZoneId.of("UTC")
        val event = ReminderEvent().apply {
            reminderEventId = 1
            medicineName = "Med"
            amount = "50mg"
            status = ReminderStatus.RAISED
            remindedTimestamp = 1705312500L
            processedTimestamp = 0L
        }

        invokeLoadTableData(listOf(event), zone)

        assertNull(viewModel.state.tableRows[0].takenAt)
    }

    // --- buildMedicinePerDayData tests (indirectly via companion/helper) ---

    @Test
    fun `buildMedicinePerDayData uses medicine color when useColor is true`() {
        val medicine = Medicine().apply {
            name = "Aspirin"
            color = 0xFFFF0000.toInt()
            useColor = true
        }
        val fullMedicine = FullMedicine().apply { this.medicine = medicine }

        val series = listOf(
            MedicinePerDaySeries("Aspirin", listOf(LocalDate.now().toEpochDay()), listOf(1))
        )

        val result = invokeBuildMedicinePerDayData(series, listOf(fullMedicine))
        assertEquals(1, result.series.size)
        assertEquals(Color(0xFFFF0000.toInt()), result.series[0].color)
    }

    @Test
    fun `buildMedicinePerDayData uses fallback color when useColor is false`() {
        val medicine = Medicine().apply {
            name = "Aspirin"
            color = 0xFFFF0000.toInt()
            useColor = false
        }
        val fullMedicine = FullMedicine().apply { this.medicine = medicine }

        val series = listOf(
            MedicinePerDaySeries("Aspirin", listOf(LocalDate.now().toEpochDay()), listOf(1))
        )

        val result = invokeBuildMedicinePerDayData(series, listOf(fullMedicine))
        assertEquals(1, result.series.size)
        assertEquals(StatisticsScreenViewModel.FALLBACK_COLORS[0], result.series[0].color)
    }

    @Test
    fun `buildMedicinePerDayData fallback colors cycle with modulo`() {
        val seriesList = StatisticsScreenViewModel.FALLBACK_COLORS.indices.map { index ->
            MedicinePerDaySeries("Med$index", listOf(LocalDate.now().toEpochDay()), listOf(1))
        } + MedicinePerDaySeries("MedExtra", listOf(LocalDate.now().toEpochDay()), listOf(1))

        val result = invokeBuildMedicinePerDayData(seriesList, emptyList())

        // Last series should wrap around to first fallback color
        val lastSeries = result.series.last()
        assertEquals(StatisticsScreenViewModel.FALLBACK_COLORS[0], lastSeries.color)
    }

    @Test
    fun `buildMedicinePerDayData with empty series returns empty data`() {
        val result = invokeBuildMedicinePerDayData(emptyList(), emptyList())
        assertEquals(0, result.series.size)
        assertEquals(0, result.days.size)
    }

    /**
     * Uses reflection to call the private loadTableData method.
     */
    private fun invokeLoadTableData(events: List<ReminderEvent>, zone: ZoneId) {
        val method = StatisticsScreenViewModel::class.java.getDeclaredMethod(
            "loadTableData",
            List::class.java,
            ZoneId::class.java,
        )
        method.isAccessible = true
        method.invoke(viewModel, events, zone)
    }

    /**
     * Uses reflection to call the private buildMedicinePerDayData method.
     */
    private fun invokeBuildMedicinePerDayData(
        seriesList: List<MedicinePerDaySeries>,
        medicines: List<FullMedicine>,
    ): com.futsch1.medtimer.statistics.model.MedicinePerDayData {
        val method = StatisticsScreenViewModel::class.java.getDeclaredMethod(
            "buildMedicinePerDayData",
            List::class.java,
            List::class.java,
        )
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(viewModel, seriesList, medicines)
                as com.futsch1.medtimer.statistics.model.MedicinePerDayData
    }
}