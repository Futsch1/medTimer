package com.futsch1.medtimer.feature.ui.impl.statistics.charts

import androidx.compose.ui.graphics.toArgb
import com.futsch1.medtimer.core.ui.TimeFormatter
import com.futsch1.medtimer.feature.ui.impl.statistics.ChartsData
import com.futsch1.medtimer.feature.ui.impl.statistics.MedicineDaySeries
import com.futsch1.medtimer.feature.ui.impl.statistics.MedicinePerDayData
import com.futsch1.medtimer.feature.ui.impl.statistics.StatisticsProvider
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import kotlin.test.assertEquals
import org.junit.Test

class ChartsPresenterTest {

    private val timeFormatter: TimeFormatter = mock {
        on { daysSinceEpochToDateString(any()) } doAnswer { "day-${it.arguments[0]}" }
    }
    private val presenter = ChartsPresenter(timeFormatter)

    @Test
    fun `present formats day labels, assigns colors, and copies tallies through`() {
        val data = ChartsData(
            perDay = MedicinePerDayData(
                epochDays = listOf(10L, 11L),
                series = listOf(MedicineDaySeries("Vitamin X", listOf(1, 0)), MedicineDaySeries("Medicine A", listOf(0, 2))),
            ),
            period = StatisticsProvider.TakenSkipped(taken = 3, skipped = 1),
            total = StatisticsProvider.TakenSkipped(taken = 9, skipped = 4),
        )

        val state = presenter.present(data, medicineColorsByName = mapOf("Vitamin X" to 0x12345678), days = 7)

        assertEquals(listOf("day-10", "day-11"), state.dayLabels)
        assertEquals(listOf(0x12345678, ChartSeriesColors.PALETTE[0].toArgb()), state.seriesColors)
        assertEquals(3, state.takenPeriod)
        assertEquals(1, state.skippedPeriod)
        assertEquals(9, state.takenTotal)
        assertEquals(4, state.skippedTotal)
        assertEquals(7, state.days)
        assertEquals(data.perDay, state.perDay)
    }
}
