package com.futsch1.medtimer.feature.ui.statistics.levels

import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.ui.TimeFormatter
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LevelsPresenterTest {

    private val timeFormatter: TimeFormatter = mock {
        on { daysSinceEpochToDateString(any()) } doAnswer { "day-${it.arguments[0]}" }
    }
    private val presenter = LevelsPresenter(timeFormatter)

    private val zone: ZoneId = ZoneOffset.UTC
    private val now: Instant = Instant.parse("2026-03-10T12:00:00Z")

    private val medicineA = Medicine.default().copy(name = "Medicine A", halfLifeHours = 4.8, timeToPeakHours = 1.5)

    private fun taken(name: String, time: Instant, amount: String = "1mg") = ReminderEvent.default().copy(
        medicineName = name,
        status = ReminderEvent.ReminderStatus.TAKEN,
        remindedTimestamp = time,
        processedTimestamp = time,
        amount = amount,
    )

    private fun present(events: List<ReminderEvent>, medicines: List<Medicine> = listOf(medicineA), days: Int = 2) =
        presenter.present(events, medicines, days, now, zone)

    private fun LevelSeries.points(): List<Pair<Double, Double>> =
        past.hours.zip(past.percent) + projection.hours.zip(projection.percent)

    private fun assertSameCurve(expected: LevelSeries, actual: LevelSeries) {
        assertEquals(expected.points().map { it.first }, actual.points().map { it.first })
        expected.points().zip(actual.points()).forEach { (e, a) -> assertEquals(e.second, a.second, 1e-9) }
    }

    private fun LevelSeries.peakHour(): Double = points().maxBy { it.second }.first

    private fun LevelSeries.percentAt(hour: Double): Double = points().minBy { kotlin.math.abs(it.first - hour) }.second

    @Test
    fun `medicines without a half-life are excluded`() {
        val state = present(emptyList(), listOf(Medicine.default().copy(name = "Vitamin X")))

        assertTrue(state.series.isEmpty())
        assertEquals(2, state.days)
        assertEquals(0, state.projectionDays)
    }

    @Test
    fun `window starts at local midnight, splits at now and projects to show a peak`() {
        val state = present(emptyList())

        val firstDay = LocalDate.of(2026, 3, 9).toEpochDay()
        // Two days of history plus one projected day, as a 1.5 h time to peak fits into one day
        assertEquals(1, state.projectionDays)
        assertEquals((0L..3L).map { "day-${firstDay + it}" }, state.midnightLabels)
        assertEquals(72.0, state.windowHours)
        // Now is noon on the second day
        assertEquals(36.0, state.nowHours, 1e-9)
        val series = state.series.single()
        assertEquals(0.0, series.past.hours.first())
        assertEquals(36.0, series.past.hours.last(), 1e-9)
        assertEquals(36.0, series.projection.hours.first(), 1e-9)
        assertEquals(72.0, series.projection.hours.last(), 1e-9)
    }

    @Test
    fun `projection covers the peak of a slowly absorbed dose`() {
        val slow = Medicine.default().copy(name = "Medicine A", halfLifeHours = 120.0, timeToPeakHours = 48.0)
        val state = present(emptyList(), listOf(slow))

        assertEquals(2, state.projectionDays)
    }

    @Test
    fun `projection extends until the last dose is no longer relevant`() {
        val slow = Medicine.default().copy(name = "Medicine A", halfLifeHours = 120.0, timeToPeakHours = 48.0)
        val state = present(listOf(taken("Medicine A", now)), listOf(slow))
        val model = slow.pharmacokineticModel()!!

        // Relevant for 10 half-lives plus the time to peak after now (noon), counted in whole days after the end of today
        val expectedDays = kotlin.math.ceil((model.relevanceHours - 12.0) / 24.0).toInt()
        assertEquals(expectedDays, state.projectionDays)
        assertEquals(2, state.peakProjectionDays)
        // The initially visible span is unchanged: the range plus enough to see the peak
        assertEquals((2 + 2) * 24.0, state.visibleHours)
        assertEquals((2 + 2) * 24.0, state.initialEndHours)
        val projection = state.series.single().projection
        assertEquals(state.windowHours, projection.hours.last(), 1e-9)
        assertTrue(projection.percent.last() < 0.2)
    }

    @Test
    fun `a dose taken just now shows up in the projection`() {
        val series = present(listOf(taken("Medicine A", now))).series.single()

        assertEquals(0.0, series.past.percent.last(), 1e-9)
        assertEquals(100.0, series.projection.percent.max(), 1e-9)
        assertEquals(37.5, series.peakHour(), 1e-3)
    }

    @Test
    fun `curve is normalized to a maximum of 100`() {
        val state = present(listOf(taken("Medicine A", Instant.parse("2026-03-09T08:00:00Z"), "2.5 mg")))

        val series = state.series.single()
        assertEquals(100.0, series.points().maxOf { it.second }, 1e-9)
        assertEquals(0.0, series.past.percent.first(), 1e-9)
        // The peak is at the dose's time to peak: 08:00 + 1.5 h
        assertEquals(9.5, series.peakHour(), 1e-3)
    }

    @Test
    fun `dose amounts weight the curve`() {
        val small = Instant.parse("2026-03-09T02:00:00Z")
        val large = Instant.parse("2026-03-10T02:00:00Z")
        val series = present(listOf(taken("Medicine A", small, "1 mg"), taken("Medicine A", large, "3mg"))).series.single()

        val smallPeak = series.percentAt(3.5)
        val largePeak = series.percentAt(27.5)
        assertEquals(100.0, largePeak, 1e-9)
        assertEquals(1.0 / 3.0, smallPeak / largePeak, 0.01)
    }

    @Test
    fun `unreadable doses are left out and counted`() {
        val counted = listOf(
            taken("Medicine A", Instant.parse("2026-03-09T06:00:00Z"), "2.5mg"),
            taken("Medicine A", Instant.parse("2026-03-09T08:00:00Z"), "5 MG"),
        )
        val events = counted + listOf(
            taken("Medicine A", Instant.parse("2026-03-09T10:00:00Z"), "?"),
            taken("Medicine A", Instant.parse("2026-03-09T14:00:00Z"), "some"),
            // Not modelled, so not reported either
            taken("Vitamin X", Instant.parse("2026-03-09T14:00:00Z"), "some"),
            taken("Vitamin X", Instant.parse("2026-03-09T15:00:00Z"), "2 x 1"),
        )
        val state = present(events)

        assertEquals(2, state.ignoredDoses)
        assertTrue(state.amountConflicts.isEmpty())
        assertTrue(state.unitConflicts.isEmpty())
        assertEquals(present(counted).series, state.series)
    }

    @Test
    fun `doses are marked on the curve with their recorded amount`() {
        val first = Instant.parse("2026-03-09T08:00:00Z")
        val second = Instant.parse("2026-03-10T08:00:00Z")
        val series = present(listOf(taken("Medicine A", first, " 2,5 mg "), taken("Medicine A", second, "5mg"))).series.single()

        assertEquals(listOf("2,5 mg", "5mg"), series.doses.map { it.label })
        assertEquals(listOf(8.0, 32.0), series.doses.map { it.hours })
        // Each marker sits on the curve at the time of the dose
        series.doses.forEach { assertEquals(series.percentAt(it.hours), it.percent, 1e-6) }
        assertEquals(0.0, series.doses.first().percent, 1e-9)
    }

    @Test
    fun `doses without a unit are labeled with the stock unit`() {
        val withStockUnit = medicineA.copy(unit = "mg")
        val events = listOf(
            taken("Medicine A", Instant.parse("2026-03-09T08:00:00Z"), "12.5"),
            taken("Medicine A", Instant.parse("2026-03-10T08:00:00Z"), "10"),
        )

        assertEquals(listOf("12.5 mg", "10 mg"), present(events, listOf(withStockUnit)).series.single().doses.map { it.label })
        // Without a stock unit, the amount is shown as recorded
        assertEquals(listOf("12.5", "10"), present(events).series.single().doses.map { it.label })
    }

    @Test
    fun `a unit in the dose is not replaced by the stock unit`() {
        val withStockUnit = medicineA.copy(unit = "IU")
        val events = listOf(taken("Medicine A", Instant.parse("2026-03-09T08:00:00Z"), "2 mg"))

        assertEquals(listOf("2 mg"), present(events, listOf(withStockUnit)).series.single().doses.map { it.label })
    }

    @Test
    fun `a medicine with amounts containing several numbers is not estimated but reported`() {
        val medicineB = Medicine.default().copy(name = "Medicine B", halfLifeHours = 10.0, timeToPeakHours = 1.0)
        val events = listOf(
            taken("Medicine A", Instant.parse("2026-03-09T06:00:00Z"), "2mg"),
            taken("Medicine A", Instant.parse("2026-03-09T16:00:00Z"), " 1-2 tablets "),
            taken("Medicine A", Instant.parse("2026-03-09T14:00:00Z"), "2 x 5mg"),
            taken("Medicine A", Instant.parse("2026-03-09T17:00:00Z"), "?"),
            taken("Medicine B", Instant.parse("2026-03-09T08:00:00Z"), "1 tablet"),
        )
        val state = present(events, listOf(medicineA, medicineB))

        assertEquals(listOf("Medicine B"), state.series.map { it.medicineName })
        // The most recent offending amount is shown, so it can be found and corrected
        assertEquals(listOf(AmountConflict("Medicine A", "1-2 tablets", AmountProblem.SEVERAL_NUMBERS)), state.amountConflicts)
        assertTrue(state.unitConflicts.isEmpty())
        // The unreadable dose of the hidden medicine is not reported separately
        assertEquals(0, state.ignoredDoses)
    }

    @Test
    fun `a medicine with numbers that cannot be read completely is not estimated but reported`() {
        val events = listOf(
            taken("Medicine A", Instant.parse("2026-03-09T06:00:00Z"), "2mg"),
            // Whatever the decimal separator, the number format stops at the separator after it
            taken("Medicine A", Instant.parse("2026-03-09T08:00:00Z"), "1.5,5.5 mg"),
            taken("Medicine A", Instant.parse("2026-03-09T09:00:00Z"), "2 x 5mg"),
        )
        val state = present(events)

        assertTrue(state.series.isEmpty())
        assertEquals(
            listOf(
                AmountConflict("Medicine A", "1.5,5.5 mg", AmountProblem.INCOMPLETE_NUMBER),
                AmountConflict("Medicine A", "2 x 5mg", AmountProblem.SEVERAL_NUMBERS),
            ),
            state.amountConflicts.sortedBy { it.problem.ordinal }.reversed(),
        )
    }

    @Test
    fun `a medicine with mixed units is not estimated but reported`() {
        val medicineB = Medicine.default().copy(name = "Medicine B", halfLifeHours = 10.0, timeToPeakHours = 1.0)
        val events = listOf(
            taken("Medicine A", Instant.parse("2026-03-09T06:00:00Z"), "2.5mg"),
            taken("Medicine A", Instant.parse("2026-03-09T08:00:00Z"), "12.5"),
            taken("Medicine A", Instant.parse("2026-03-09T10:00:00Z"), "5 mg"),
            taken("Medicine A", Instant.parse("2026-03-09T11:00:00Z"), "?"),
            taken("Medicine B", Instant.parse("2026-03-09T08:00:00Z"), "1 tablet"),
        )
        val state = present(events, listOf(medicineA, medicineB))

        assertEquals(listOf("Medicine B"), state.series.map { it.medicineName })
        assertEquals(listOf(UnitConflict("Medicine A", persistentListOf(UnitCount("mg", 2), UnitCount("", 1)))), state.unitConflicts)
        // The unreadable dose of the hidden medicine is not reported separately
        assertEquals(0, state.ignoredDoses)
    }

    @Test
    fun `any consistent unit works, including none`() {
        val time = Instant.parse("2026-03-09T08:00:00Z")
        val inMg = present(listOf(taken("Medicine A", time, "2mg"), taken("Medicine A", time.plusSeconds(7200), "4mg")))
        val unitless = present(listOf(taken("Medicine A", time, "2"), taken("Medicine A", time.plusSeconds(7200), "4")))
        val tablets = present(listOf(taken("Medicine A", time, "1 tablets"), taken("Medicine A", time.plusSeconds(7200), "2 tablets")))

        // Normalization removes the unit's scale, so all three give the same curve
        assertSameCurve(inMg.series.single(), unitless.series.single())
        assertSameCurve(inMg.series.single(), tablets.series.single())
        assertEquals(0, tablets.ignoredDoses)
    }

    @Test
    fun `only taken events of the matching medicine count, including cycle suffixes`() {
        val time = Instant.parse("2026-03-09T08:00:00Z")
        val events = listOf(
            taken("Medicine A (2/21)", time),
            taken("Vitamin X", time.plusSeconds(3600)),
            taken("Medicine A", time.plusSeconds(7200)).copy(status = ReminderEvent.ReminderStatus.SKIPPED),
        )
        val series = present(events).series.single()

        // Any of the other events would have moved the peak later
        assertEquals(9.5, series.peakHour(), 1e-3)
    }

    @Test
    fun `history extends back to the first dose while the visible span follows the range`() {
        val state = present(listOf(taken("Medicine A", Instant.parse("2026-03-05T08:00:00Z"))))

        // 5th to 10th of March, of which the last 2 days plus the projected day are visible at once
        assertEquals(6, state.historyDays)
        assertEquals(2, state.days)
        assertEquals((6 + 1) * 24.0, state.windowHours)
        assertEquals((2 + 1) * 24.0, state.visibleHours)
        assertEquals("day-${LocalDate.of(2026, 3, 5).toEpochDay()}", state.midnightLabels.first())
        assertEquals(8 + 1.5, state.series.single().peakHour(), 1e-3)
    }

    @Test
    fun `history is limited to a year, but older doses still contribute`() {
        val veryslow = Medicine.default().copy(name = "Medicine A", halfLifeHours = 2000.0, timeToPeakHours = 48.0)
        val state = present(listOf(taken("Medicine A", Instant.parse("2025-01-01T08:00:00Z"))), listOf(veryslow))

        assertEquals(365, state.historyDays)
        assertTrue(state.series.single().past.percent.first() > 0.0)
    }

    @Test
    fun `only custom colors are passed on`() {
        val colored = Medicine.default().copy(name = "Medicine B", halfLifeHours = 10.0, timeToPeakHours = 1.0, useColor = true, color = 0x12345678)
        val state = present(emptyList(), listOf(medicineA, colored))

        assertEquals(listOf(null, 0x12345678), state.series.map { it.color })
    }

    @Test
    fun `local hours ignore daylight saving shifts`() {
        val berlin = ZoneId.of("Europe/Berlin")
        val start = LocalDateTime.of(2026, 3, 29, 0, 0)
        // Clocks go forward at 02:00, so noon local time is only 11 real hours after midnight
        val noon = LocalDateTime.of(2026, 3, 29, 12, 0).atZone(berlin).toInstant()

        assertEquals(12.0, LevelsPresenter.localHoursBetween(start, noon, berlin), 1e-9)
    }
}
