package com.futsch1.medtimer.statistics.ui.preview

import androidx.compose.ui.graphics.Color
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.statistics.domain.AnalysisDays
import com.futsch1.medtimer.statistics.domain.StatisticsTabType
import com.futsch1.medtimer.statistics.model.MedicinePerDayData
import com.futsch1.medtimer.statistics.model.MedicineSeriesData
import com.futsch1.medtimer.statistics.model.ReminderTableRowData
import com.futsch1.medtimer.statistics.model.TakenSkippedData
import com.futsch1.medtimer.statistics.ui.StatisticsScreenState
import com.futsch1.medtimer.statistics.ui.calendar.CalendarDayEvent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object PreviewData {
    val baseDate: LocalDate = LocalDate.of(2024, 1, 15)

    // Calendar events
    val aspirinTaken = CalendarDayEvent(
        LocalDateTime.of(baseDate, LocalTime.of(8, 0)),
        "100mg",
        "Aspirin",
        CalendarDayEvent.Status.TAKEN,
    )

    val ibuprofenSkipped = CalendarDayEvent(
        LocalDateTime.of(baseDate, LocalTime.of(12, 0)),
        "200mg",
        "Ibuprofen",
        CalendarDayEvent.Status.SKIPPED,
    )

    val vitaminDTaken = CalendarDayEvent(
        LocalDateTime.of(baseDate.minusDays(1), LocalTime.of(9, 0)),
        "50mg",
        "Vitamin D",
        CalendarDayEvent.Status.TAKEN,
    )

    val vitaminDRaised = CalendarDayEvent(
        LocalDateTime.of(baseDate, LocalTime.of(18, 0)),
        "50mg",
        "Vitamin D",
        CalendarDayEvent.Status.RAISED,
    )

    val aspirinScheduled = CalendarDayEvent(
        LocalDateTime.of(baseDate.plusDays(1), LocalTime.of(8, 0)),
        "100mg",
        "Aspirin",
        CalendarDayEvent.Status.SCHEDULED,
    )

    val melatoninScheduled = CalendarDayEvent(
        LocalDateTime.of(baseDate, LocalTime.of(20, 0)),
        "10mg",
        "Melatonin",
        CalendarDayEvent.Status.SCHEDULED,
    )

    // Day events map
    val sampleDayEvents: ImmutableMap<LocalDate, List<CalendarDayEvent>> = persistentMapOf(
        baseDate to listOf(aspirinTaken, ibuprofenSkipped),
        baseDate.minusDays(1) to listOf(vitaminDTaken),
        baseDate.plusDays(1) to listOf(aspirinScheduled),
    )

    // Reminder table rows
    val aspirinRow = ReminderTableRowData(
        eventId = 1,
        takenAt = LocalDateTime.of(2024, 1, 15, 8, 0),
        takenStatus = ReminderEvent.ReminderStatus.TAKEN,
        medicineName = "Aspirin",
        dosage = "100mg",
        remindedAt = LocalDateTime.of(2024, 1, 15, 7, 55),
    )

    val ibuprofenRow = ReminderTableRowData(
        eventId = 2,
        takenAt = null,
        takenStatus = ReminderEvent.ReminderStatus.RAISED,
        medicineName = "Ibuprofen",
        dosage = "200mg",
        remindedAt = LocalDateTime.of(2024, 1, 15, 9, 0),
    )

    val vitaminDRow = ReminderTableRowData(
        eventId = 3,
        takenAt = null,
        takenStatus = ReminderEvent.ReminderStatus.SKIPPED,
        medicineName = "Vitamin D",
        dosage = "1000IU",
        remindedAt = LocalDateTime.of(2024, 1, 15, 12, 0),
    )

    val sampleTableRows: ImmutableList<ReminderTableRowData> = persistentListOf(
        aspirinRow,
        ibuprofenRow,
        vitaminDRow,
    )

    // Chart data
    val sampleMedicinePerDayData = MedicinePerDayData(
        title = "7 days",
        days = listOf(
            LocalDate.of(2023, 12, 1),
            LocalDate.of(2023, 12, 2),
            LocalDate.of(2023, 12, 3),
            LocalDate.of(2023, 12, 4),
            LocalDate.of(2023, 12, 5),
        ),
        series = listOf(
            MedicineSeriesData("Aspirin", listOf(1, 2, 1, 0, 2), Color(0xFF003f5c)),
            MedicineSeriesData("Ibuprofen", listOf(0, 1, 1, 1, 0), Color(0xFF2f4b7c)),
        ),
    )

    val sampleTakenSkippedData = TakenSkippedData(
        taken = 7,
        skipped = 3,
        title = "7 days",
    )

    val sampleTakenSkippedTotalData = TakenSkippedData(
        taken = 42,
        skipped = 8,
        title = "Total",
    )

    // Statistics screen states
    val emptyStatisticsScreenState: StatisticsScreenState = object : StatisticsScreenState {
        override val medicinePerDayData: MedicinePerDayData? = null
        override val takenSkippedData: TakenSkippedData? = null
        override val takenSkippedTotalData: TakenSkippedData? = null
        override val filterText: String = ""
        override val tableRows: ImmutableList<ReminderTableRowData> = persistentListOf()
        override val selectedTab: StatisticsTabType = StatisticsTabType.CHARTS
        override val selectedDays: AnalysisDays = AnalysisDays.SEVEN_DAYS
    }

    val chartsStatisticsScreenState: StatisticsScreenState = object : StatisticsScreenState {
        override val medicinePerDayData: MedicinePerDayData = sampleMedicinePerDayData
        override val takenSkippedData: TakenSkippedData = sampleTakenSkippedData
        override val takenSkippedTotalData: TakenSkippedData = sampleTakenSkippedTotalData
        override val filterText: String = ""
        override val tableRows: ImmutableList<ReminderTableRowData> = sampleTableRows
        override val selectedTab: StatisticsTabType = StatisticsTabType.CHARTS
        override val selectedDays: AnalysisDays = AnalysisDays.SEVEN_DAYS
    }
}
