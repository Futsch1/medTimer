package com.futsch1.medtimer.feature.ui.statistics.table

import com.futsch1.medtimer.core.ui.component.SortableTableRow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext

/**
 * The plain values one filter pass needs, captured off any Compose state so the scan can run on a
 * background dispatcher (reading Compose snapshot state off the main thread would throw).
 */
internal data class ReminderRowFilterInputs(
    val rows: ImmutableList<SortableTableRow>,
    val normalizedQuery: String,
)

/**
 * Pure substring filter over the presented reminder rows — no Compose state, so it can run off the
 * main thread. Callers pass an already-normalized query.
 */
object ReminderRowFilter {
    /** The single source of truth for how a raw query is normalized before matching. */
    fun normalizeQuery(text: String): String = text.trim()

    fun filter(
        rows: ImmutableList<SortableTableRow>,
        normalizedQuery: String,
    ): ImmutableList<SortableTableRow> {
        if (normalizedQuery.isBlank()) return rows
        return rows
            .filter { row -> row.cells.any { it.text.contains(normalizedQuery, ignoreCase = true) } }
            .toImmutableList()
    }
}

/**
 * The Compose-free filter loop: maps a stream of [ReminderRowFilterInputs] to the matched rows,
 * running each pass on [dispatcher] and cancelling a stale pass when newer inputs arrive
 * ([mapLatest]). This is the test surface — feed it a plain `Flow` from a `runTest` and assert the
 * output, with no Compose runtime required.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun Flow<ReminderRowFilterInputs>.filteredRows(
    dispatcher: CoroutineDispatcher,
): Flow<ImmutableList<SortableTableRow>> =
    mapLatest { inputs ->
        withContext(dispatcher) {
            ReminderRowFilter.filter(inputs.rows, inputs.normalizedQuery)
        }
    }
