package com.futsch1.medtimer.feature.ui.impl.statistics.table

import com.futsch1.medtimer.core.ui.component.SortableTableCell
import com.futsch1.medtimer.core.ui.component.SortableTableRow
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Exercises the Compose-free filter loop ([filteredRows]) directly with a plain `Flow` and a test
 * dispatcher — no Compose runtime, no ViewModel. The `snapshotFlow` wiring that feeds this loop in
 * production lives in [com.futsch1.medtimer.feature.ui.impl.statistics.StatisticsScreenViewModel]; here we
 * pin the loop's behaviour and the pure matcher.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReminderRowFilterTest {

    private fun row(id: Long, vararg cells: String) =
        SortableTableRow(id = id, cells = cells.map { SortableTableCell(it) }.toImmutableList())

    private fun inputs(rows: List<SortableTableRow>, query: String) =
        ReminderRowFilterInputs(rows.toImmutableList(), ReminderRowFilter.normalizeQuery(query))

    private val rows = listOf(
        row(1, "May 28, 09:00", "Vitamin X 500 mg", "1 tablet"),
        row(2, "-", "Medicine A", "2 ml"),
    )

    @Test
    fun `blank query returns every row`() = runTest {
        val result = flowOf(inputs(rows, "   "))
            .filteredRows(UnconfinedTestDispatcher(testScheduler))
            .toList()

        assertEquals(rows, result.single())
    }

    @Test
    fun `query matches any cell case-insensitively`() = runTest {
        val result = flowOf(inputs(rows, "vitamin"))
            .filteredRows(UnconfinedTestDispatcher(testScheduler))
            .toList()

        assertEquals(1, result.single().size)
        assertEquals(1L, result.single().single().id)
    }

    @Test
    fun `non-matching query yields no rows`() = runTest {
        val result = flowOf(inputs(rows, "aspirin"))
            .filteredRows(UnconfinedTestDispatcher(testScheduler))
            .toList()

        assertTrue(result.single().isEmpty())
    }

    @Test
    fun `each emitted input produces its own filtered result`() = runTest {
        val result = flowOf(inputs(rows, "vitamin"), inputs(rows, "medicine"))
            .filteredRows(UnconfinedTestDispatcher(testScheduler))
            .toList()

        assertEquals(2, result.size)
        assertEquals(1L, result[0].single().id)
        assertEquals(2L, result[1].single().id)
    }

    @Test
    fun `normalizeQuery trims surrounding whitespace`() {
        assertEquals("vitamin", ReminderRowFilter.normalizeQuery("  vitamin  "))
    }
}
