package com.futsch1.medtimer.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** A column header for [SortableTable]. */
data class SortableTableColumn(
    val title: String,
    val weight: Float = 1f,
    val sortable: Boolean = true,
)

/** A single cell: [text] is displayed; [sortValue] drives sorting (defaults to [text] when null). */
data class SortableTableCell(
    val text: String,
    val sortValue: Comparable<*>? = null,
)

/** A table row, identified by a stable [id] for `LazyColumn` keying. */
data class SortableTableRow(
    val id: Long,
    val cells: ImmutableList<SortableTableCell>,
)

@Composable
fun SortableTable(
    columns: ImmutableList<SortableTableColumn>,
    rows: ImmutableList<SortableTableRow>,
    modifier: Modifier = Modifier,
    filterLabel: String = "",
    onRowClick: ((SortableTableRow) -> Unit)? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var sortColumn by rememberSaveable { mutableIntStateOf(0) }
    var ascending by rememberSaveable { mutableStateOf(true) }

    val visibleRows = remember(rows, query, sortColumn, ascending) {
        deriveVisibleRows(rows, query, sortColumn, ascending)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(filterLabel) },
            singleLine = true,
            trailingIcon = if (query.isEmpty()) null else {
                { Text(text = "✕", modifier = Modifier.clickable { query = "" }.padding(12.dp)) }
            },
            modifier = Modifier.fillMaxWidth().padding(8.dp),
        )

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            columns.forEachIndexed { index, column ->
                val arrow = if (index == sortColumn) (if (ascending) " ↑" else " ↓") else ""
                Text(
                    text = column.title + arrow,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(column.weight)
                        .then(
                            if (!column.sortable) Modifier
                            else Modifier.clickable {
                                if (sortColumn == index) ascending = !ascending else {
                                    sortColumn = index
                                    ascending = true
                                }
                            },
                        )
                        .padding(end = 4.dp),
                )
            }
        }
        HorizontalDivider()

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(items = visibleRows, key = { it.id }) { row ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (onRowClick == null) Modifier else Modifier.clickable { onRowClick(row) })
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                        .semantics(mergeDescendants = true) {},
                ) {
                    columns.forEachIndexed { index, column ->
                        Text(
                            text = row.cells.getOrNull(index)?.text.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(column.weight),
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

private fun deriveVisibleRows(
    rows: ImmutableList<SortableTableRow>,
    query: String,
    sortColumn: Int,
    ascending: Boolean,
): List<SortableTableRow> {
    val filtered = if (query.isBlank()) {
        rows
    } else {
        rows.filter { row -> row.cells.any { it.text.contains(query, ignoreCase = true) } }
    }
    val comparator = Comparator<SortableTableRow> { a, b ->
        val aValue = a.cells.getOrNull(sortColumn)?.let { it.sortValue ?: it.text }
        val bValue = b.cells.getOrNull(sortColumn)?.let { it.sortValue ?: it.text }
        compareValues(aValue, bValue)
    }
    return filtered.sortedWith(if (ascending) comparator else comparator.reversed())
}

@MedTimerPreview
@Composable
private fun SortableTablePreview() {
    MedTimerTheme {
        SortableTable(
            columns = persistentListOf(
                SortableTableColumn("Name"),
                SortableTableColumn("Dosage"),
            ),
            rows = persistentListOf(
                SortableTableRow(1, persistentListOf(SortableTableCell("Vitamin X 500 mg"), SortableTableCell("1 tablet"))),
                SortableTableRow(2, persistentListOf(SortableTableCell("Medicine A"), SortableTableCell("2 ml"))),
            ),
            filterLabel = "Filter",
        )
    }
}
