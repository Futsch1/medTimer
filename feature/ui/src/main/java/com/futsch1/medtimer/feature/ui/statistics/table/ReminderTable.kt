package com.futsch1.medtimer.feature.ui.statistics.table

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.component.DefaultDataCell
import com.futsch1.medtimer.core.ui.component.SortableTable
import com.futsch1.medtimer.core.ui.component.SortableTableCell
import com.futsch1.medtimer.core.ui.component.SortableTableColumn
import com.futsch1.medtimer.core.ui.component.SortableTableRow
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

private const val NAME_COLUMN_INDEX = 1

@Composable
fun ReminderTable(
    rows: ImmutableList<SortableTableRow>,
    onEditEvent: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val columns = persistentListOf(
        SortableTableColumn(title = stringResource(R.string.taken), minWidth = 120.dp),
        SortableTableColumn(title = stringResource(R.string.name), minWidth = 120.dp, fill = true),
        SortableTableColumn(title = stringResource(R.string.dosage), minWidth = 80.dp),
        SortableTableColumn(title = stringResource(R.string.reminded), minWidth = 120.dp),
    )

    var query by rememberSaveable { mutableStateOf("") }
    val visibleRows = remember(rows, query) {
        if (query.isBlank()) rows
        else rows.filter { row -> row.cells.any { it.text.contains(query, ignoreCase = true) } }.toImmutableList()
    }

    Card(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.filter)) },
                singleLine = true,
                trailingIcon = {
                    AnimatedVisibility(
                        visible = query.isNotEmpty(),
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut(),
                    ) {
                        IconButton(onClick = { query = "" }) {
                            Icon(painterResource(R.drawable.ic_cancel), contentDescription = stringResource(R.string.cancel))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            ) {
                SortableTable(
                    columns = columns,
                    rows = visibleRows,
                    modifier = Modifier.padding(8.dp),
                ) { row, columnIndex, columnWidth ->
                    if (columnIndex == NAME_COLUMN_INDEX) {
                        Text(
                            text = row.cells.getOrNull(columnIndex)?.text.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.Underline),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .width(columnWidth)
                                .clickable { onEditEvent(row.id.toInt()) }
                                .padding(horizontal = 4.dp),
                        )
                    } else {
                        DefaultDataCell(text = row.cells.getOrNull(columnIndex)?.text.orEmpty(), width = columnWidth)
                    }
                }
            }
        }
    }
}

private fun sampleRow(id: Long, taken: String, name: String, dosage: String, reminded: String) =
    SortableTableRow(
        id = id,
        cells = persistentListOf(
            SortableTableCell(taken),
            SortableTableCell(name),
            SortableTableCell(dosage),
            SortableTableCell(reminded),
        ),
    )

@MedTimerPreview
@Composable
private fun ReminderTablePreview() {
    MedTimerTheme {
        ReminderTable(
            rows = persistentListOf(
                sampleRow(1, "May 28, 09:00", "Vitamin X 500 mg", "1 tablet", "May 28, 08:00"),
                sampleRow(2, "-", "Medicine A", "2 ml", "May 28, 12:30"),
                sampleRow(3, "May 27, 21:00", "Supplement B", "1 capsule", "May 27, 20:00"),
            ),
            onEditEvent = {},
        )
    }
}
