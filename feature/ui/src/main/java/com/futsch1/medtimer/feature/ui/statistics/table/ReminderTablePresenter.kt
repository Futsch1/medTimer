package com.futsch1.medtimer.feature.ui.statistics.table

import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.ui.TimeFormatter
import com.futsch1.medtimer.core.ui.component.SortableTableCell
import com.futsch1.medtimer.core.ui.component.SortableTableRow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

/**
 * Turns filtered [ReminderEvent]s into render-ready [SortableTableRow]s for the Reminder Table. Owns
 * the cell contract the table renders by index — taken timestamp, medicine name, amount, reminded
 * timestamp — and formats the two timestamps (a skipped event shows "-" for taken). Pure and
 * JVM-testable, mirroring the Charts presenter.
 */
class ReminderTablePresenter @Inject constructor(
    private val timeFormatter: TimeFormatter,
) {
    fun present(events: List<ReminderEvent>): ImmutableList<SortableTableRow> =
        events.map { it.toTableRow() }.toImmutableList()

    private fun ReminderEvent.toTableRow(): SortableTableRow {
        val takenText = if (status == ReminderEvent.ReminderStatus.TAKEN) {
            timeFormatter.toDateTimeString(processedTimestamp)
        } else {
            "-"
        }
        return SortableTableRow(
            id = reminderEventId.toLong(),
            cells = persistentListOf(
                SortableTableCell(takenText, processedTimestamp),
                SortableTableCell(medicineName),
                SortableTableCell(amount),
                SortableTableCell(timeFormatter.toDateTimeString(remindedTimestamp), remindedTimestamp),
            ),
        )
    }
}
