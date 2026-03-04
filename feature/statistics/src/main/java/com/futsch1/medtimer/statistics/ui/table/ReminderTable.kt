package com.futsch1.medtimer.statistics.ui.table

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.designsystem.MedTimerTheme
import com.futsch1.medtimer.core.ui.ColumnDefinition
import com.futsch1.medtimer.core.ui.DefaultDataCell
import com.futsch1.medtimer.core.ui.SortableTable
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.statistics.R
import com.futsch1.medtimer.statistics.model.ReminderTableRowData
import com.futsch1.medtimer.statistics.ui.StatisticsTestTags
import com.futsch1.medtimer.statistics.ui.preview.PreviewData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import com.futsch1.medtimer.core.ui.R as CoreUiR

private val DATE_TIME_FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)

private enum class ReminderTableColumn {
    TAKEN, NAME, DOSAGE, REMINDED
}

@Composable
private fun rememberReminderTableColumns(): ImmutableList<ColumnDefinition<ReminderTableRowData>> {
    val takenHeader = stringResource(R.string.taken)
    val nameHeader = stringResource(R.string.name)
    val dosageHeader = stringResource(R.string.dosage)
    val remindedHeader = stringResource(R.string.reminded)

    return remember(takenHeader, nameHeader, dosageHeader, remindedHeader) {
        persistentListOf(
            ColumnDefinition(
                header = takenHeader,
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
                header = nameHeader,
                minWidth = 120.dp,
                text = { it.medicineName },
                sortKey = { it.normalizedMedicineName },
                fill = true,
            ),
            ColumnDefinition(
                header = dosageHeader,
                minWidth = 80.dp,
                text = { it.dosage },
                sortKey = { it.dosage },
            ),
            ColumnDefinition(
                header = remindedHeader,
                minWidth = 120.dp,
                text = { it.remindedAt.format(DATE_TIME_FORMATTER) },
                sortKey = { it.remindedAt },
            ),
        )
    }
}

@Composable
fun ReminderTable(
    rows: ImmutableList<ReminderTableRowData>,
    filterText: String,
    onFilterTextChanged: (String) -> Unit,
    onEditEvent: (eventId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val columns = rememberReminderTableColumns()
    val hasFilter = remember(filterText) { filterText.isNotEmpty() }

    Card(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = filterText,
                onValueChange = onFilterTextChanged,
                label = { Text(stringResource(CoreUiR.string.filter)) },
                singleLine = true,
                trailingIcon = {
                    AnimatedVisibility(hasFilter) {
                        IconButton(onClick = { onFilterTextChanged("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Cancel,
                                contentDescription = null
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(StatisticsTestTags.TABLE_FILTER)
            )

            Card(
                modifier = modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                )
            ) {
                SortableTable(
                    rows = rows,
                    columns = columns,
                    rowKey = { it.eventId },
                    modifier = Modifier.padding(8.dp)
                ) { row, columnIndex, columnWidth ->
                    if (columnIndex == ReminderTableColumn.NAME.ordinal) {
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
                            text = columns[columnIndex].text(row),
                            width = columnWidth
                        )
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ReminderTablePreview() {
    MedTimerTheme {
        Surface {
            ReminderTable(
                rows = PreviewData.sampleTableRows,
                filterText = "",
                onFilterTextChanged = {},
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
                rows = persistentListOf(),
                filterText = "",
                onFilterTextChanged = {},
                onEditEvent = {}
            )
        }
    }
}
