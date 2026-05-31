package com.futsch1.medtimer.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/** A column header for [SortableTable]: laid out at [minWidth], growing to fill spare space when [fill]. */
data class SortableTableColumn(
    val title: String,
    val minWidth: Dp,
    val fill: Boolean = false,
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

/** Sort cycles through these in header-tap order: [DESCENDING] → [ASCENDING] → [UNSORTED]. */
enum class SortDirection { DESCENDING, ASCENDING, UNSORTED }

@Composable
fun SortableTable(
    columns: ImmutableList<SortableTableColumn>,
    rows: ImmutableList<SortableTableRow>,
    modifier: Modifier = Modifier,
    defaultSortColumnIndex: Int = 0,
    defaultSortDirection: SortDirection = SortDirection.DESCENDING,
    cellContent: (@Composable (row: SortableTableRow, columnIndex: Int, columnWidth: Dp) -> Unit)? = null,
) {
    var sortColumn by rememberSaveable { mutableIntStateOf(defaultSortColumnIndex) }
    var sortDirection by rememberSaveable { mutableStateOf(defaultSortDirection) }

    val sortedRows = remember(rows, sortColumn, sortDirection) {
        sortRows(rows, sortColumn, sortDirection)
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val availableWidth = with(LocalDensity.current) { constraints.maxWidth.toDp() }
        val totalMinWidth = columns.sumOf { it.minWidth.value.toDouble() }.dp
        val columnWidths = remember(columns, availableWidth) {
            columns.map { column ->
                if (column.fill && availableWidth > totalMinWidth) {
                    column.minWidth + (availableWidth - totalMinWidth)
                } else {
                    column.minWidth
                }
            }.toImmutableList()
        }
        val totalWidth = columnWidths.sumOf { it.value.toDouble() }.dp

        Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            HeaderRow(
                columns = columns,
                columnWidths = columnWidths,
                sortColumn = sortColumn,
                sortDirection = sortDirection,
                onHeaderClick = { index ->
                    if (sortColumn == index) {
                        sortDirection = when (sortDirection) {
                            SortDirection.DESCENDING -> SortDirection.ASCENDING
                            SortDirection.ASCENDING -> SortDirection.UNSORTED
                            SortDirection.UNSORTED -> SortDirection.DESCENDING
                        }
                    } else {
                        sortColumn = index
                        sortDirection = SortDirection.DESCENDING
                    }
                },
            )
            HorizontalDivider(modifier = Modifier.width(totalWidth))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = sortedRows, key = { it.id }) { row ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .semantics(mergeDescendants = true) {},
                    ) {
                        columns.forEachIndexed { index, _ ->
                            if (cellContent != null) {
                                cellContent(row, index, columnWidths[index])
                            } else {
                                DefaultDataCell(text = row.cells.getOrNull(index)?.text.orEmpty(), width = columnWidths[index])
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.width(totalWidth))
                }
            }
        }
    }
}

internal fun sortRows(
    rows: ImmutableList<SortableTableRow>,
    sortColumn: Int,
    sortDirection: SortDirection,
): List<SortableTableRow> {
    if (sortDirection == SortDirection.UNSORTED) {
        return rows
    }
    val comparator = Comparator<SortableTableRow> { a, b ->
        val aValue = a.cells.getOrNull(sortColumn)?.let { it.sortValue ?: it.text }
        val bValue = b.cells.getOrNull(sortColumn)?.let { it.sortValue ?: it.text }
        compareValues(aValue, bValue)
    }
    return rows.sortedWith(if (sortDirection == SortDirection.ASCENDING) comparator else comparator.reversed())
}

@Composable
private fun HeaderRow(
    columns: ImmutableList<SortableTableColumn>,
    columnWidths: ImmutableList<Dp>,
    sortColumn: Int,
    sortDirection: SortDirection,
    onHeaderClick: (Int) -> Unit,
) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        columns.forEachIndexed { index, column ->
            Row(
                modifier = Modifier
                    .width(columnWidths[index])
                    .then(if (column.sortable) Modifier.clickable { onHeaderClick(index) } else Modifier)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = column.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (sortColumn == index && sortDirection != SortDirection.UNSORTED) {
                    Icon(
                        painter = painterResource(R.drawable.ic_sort),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer { scaleY = if (sortDirection == SortDirection.ASCENDING) -1f else 1f },
                    )
                }
            }
        }
    }
}

@Composable
fun DefaultDataCell(text: String, width: Dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.width(width).padding(horizontal = 4.dp),
    )
}

@MedTimerPreview
@Composable
private fun SortableTablePreview() {
    MedTimerTheme {
        SortableTable(
            columns = persistentListOf(
                SortableTableColumn("Name", minWidth = 120.dp, fill = true),
                SortableTableColumn("Dosage", minWidth = 80.dp),
            ),
            rows = persistentListOf(
                SortableTableRow(1, persistentListOf(SortableTableCell("Vitamin X 500 mg"), SortableTableCell("1 tablet"))),
                SortableTableRow(2, persistentListOf(SortableTableCell("Medicine A"), SortableTableCell("2 ml"))),
            ),
        )
    }
}
