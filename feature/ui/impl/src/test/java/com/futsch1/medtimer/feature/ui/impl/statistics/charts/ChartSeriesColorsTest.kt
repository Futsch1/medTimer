package com.futsch1.medtimer.feature.ui.impl.statistics.charts

import androidx.compose.ui.graphics.toArgb
import kotlin.test.assertEquals
import org.junit.Test

// Pure JVM test — no Robolectric, no Medicine fixtures. The whole point of extracting the rule.
class ChartSeriesColorsTest {

    @Test
    fun `empty series yields no colors`() {
        assertEquals(emptyList(), ChartSeriesColors.assign(emptyList(), emptyMap()))
    }

    @Test
    fun `series without a custom color cycle through the palette in order`() {
        val colors = ChartSeriesColors.assign(listOf("Vitamin X", "Medicine A"), emptyMap())

        assertEquals(
            listOf(ChartSeriesColors.PALETTE[0].toArgb(), ChartSeriesColors.PALETTE[1].toArgb()),
            colors,
        )
    }

    @Test
    fun `a custom medicine color wins over the palette`() {
        val colors = ChartSeriesColors.assign(listOf("Vitamin X"), mapOf("Vitamin X" to 0x12345678))

        assertEquals(listOf(0x12345678), colors)
    }

    @Test
    fun `the fallback index only advances for series without a custom color`() {
        // "Medicine A" takes its custom color; the palette must NOT be consumed for it, so the
        // surrounding series still pick palette slots 0 and 1 rather than skipping to 1 and 2.
        val colors = ChartSeriesColors.assign(
            listOf("Vitamin X", "Medicine A", "Medicine B"),
            mapOf("Medicine A" to 0x12345678),
        )

        assertEquals(
            listOf(ChartSeriesColors.PALETTE[0].toArgb(), 0x12345678, ChartSeriesColors.PALETTE[1].toArgb()),
            colors,
        )
    }

    @Test
    fun `the palette wraps once exhausted`() {
        val names = List(ChartSeriesColors.PALETTE.size + 1) { "series-$it" }

        val colors = ChartSeriesColors.assign(names, emptyMap())

        assertEquals(ChartSeriesColors.PALETTE[0].toArgb(), colors.last())
    }
}
