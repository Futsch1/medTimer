package com.futsch1.medtimer.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.designsystem.MedTimerTheme
import com.futsch1.medtimer.database.ReminderEvent
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val DATE_TIME_FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)

private val columns = persistentListOf(
    ColumnDefinition<ReminderTableRowData>(
        header = "",
        minWidth = 120.dp,
        text = {
            it.takenAt?.format(DATE_TIME_FORMATTER) ?: when (it.takenStatus) {
                ReminderEvent.ReminderStatus.RAISED -> " "
                else -> "-"
            }
        },
        sortKey = { it.takenAt ?: LocalDateTime.MIN },
    ),
    ColumnDefinition(
        header = "",
        minWidth = 120.dp,
        text = { it.medicineName },
        sortKey = { it.normalizedMedicineName },
        fill = true,
    ),
    ColumnDefinition(
        header = "",
        minWidth = 80.dp,
        text = { it.dosage },
        sortKey = { it.dosage },
    ),
    ColumnDefinition(
        header = "",
        minWidth = 120.dp,
        text = { it.remindedAt.format(DATE_TIME_FORMATTER) },
        sortKey = { it.remindedAt },
    ),
)

@Composable
fun ReminderTable(
    data: ReminderTableData,
    onEditEvent: (eventId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val columnsWithHeaders = remember(data.columnHeaders) {
        data.columnHeaders.mapIndexed { index, header ->
            columns[index].copy(header = header)
        }.toImmutableList()
    }

    SortableFilterTable(
        rows = data.rows,
        columns = columnsWithHeaders,
        rowKey = { it.eventId },
        modifier = modifier,
    ) { row, columnIndex, columnWidth ->
        if (columnIndex == 1) {
            Text(
                text = row.medicineName,
                style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .width(columnWidth)
                    .clickable { onEditEvent(row.eventId) }
                    .padding(horizontal = 4.dp)
            )
        } else {
            DefaultDataCell(
                text = columnsWithHeaders[columnIndex].text(row),
                width = columnWidth
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ReminderTablePreview() {
    MedTimerTheme {
        Surface {
            ReminderTable(
                data = ReminderTableData(
                    rows = persistentListOf(
                        ReminderTableRowData(
                            eventId = 1,
                            takenAt = LocalDateTime.of(2024, 1, 15, 8, 0),
                            takenStatus = ReminderEvent.ReminderStatus.TAKEN,
                            medicineName = "Aspirin",
                            dosage = "100mg",
                            remindedAt = LocalDateTime.of(2024, 1, 15, 7, 55),
                        ),
                        ReminderTableRowData(
                            eventId = 2,
                            takenAt = null,
                            takenStatus = ReminderEvent.ReminderStatus.RAISED,
                            medicineName = "Ibuprofen",
                            dosage = "200mg",
                            remindedAt = LocalDateTime.of(2024, 1, 15, 9, 0),
                        ),
                        ReminderTableRowData(
                            eventId = 3,
                            takenAt = null,
                            takenStatus = ReminderEvent.ReminderStatus.SKIPPED,
                            medicineName = "Vitamin D",
                            dosage = "1000IU",
                            remindedAt = LocalDateTime.of(2024, 1, 15, 12, 0),
                        ),
                    ),
                    columnHeaders = persistentListOf("Taken", "Name", "Dosage", "Reminded")
                ),
                onEditEvent = {}
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ReminderTableEmptyPreview() {
    MedTimerTheme {
        Surface {
            ReminderTable(
                data = ReminderTableData(
                    rows = persistentListOf(),
                    columnHeaders = persistentListOf("Taken", "Name", "Dosage", "Reminded")
                ),
                onEditEvent = {}
            )
        }
    }
}