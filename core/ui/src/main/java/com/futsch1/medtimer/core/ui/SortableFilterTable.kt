package com.futsch1.medtimer.core.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.designsystem.MedTimerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

data class ColumnDefinition<T>(
    val header: String,
    val minWidth: Dp,
    val text: (T) -> String,
    val sortKey: (T) -> Comparable<*>,
    val fill: Boolean = false,
)

enum class SortDirection { DESCENDING, ASCENDING, UNSORTED }

@Composable
fun <T : Any> SortableTable(
    rows: ImmutableList<T>,
    columns: ImmutableList<ColumnDefinition<T>>,
    rowKey: (T) -> Any,
    modifier: Modifier = Modifier,
    defaultSortColumnIndex: Int = 0,
    defaultSortDirection: SortDirection = SortDirection.DESCENDING,
    cellContent: @Composable ((row: T, columnIndex: Int, columnWidth: Dp) -> Unit)? = null,
) {
    var sortColumnIndex by remember { mutableIntStateOf(defaultSortColumnIndex) }
    var sortDirection by remember { mutableStateOf(defaultSortDirection) }

    val sorted = remember(rows, sortColumnIndex, sortDirection) {
        if (sortDirection == SortDirection.UNSORTED) {
            rows
        } else {
            @Suppress("UNCHECKED_CAST")
            val selector = columns[sortColumnIndex].sortKey as (T) -> Comparable<Any>
            val base = compareBy(selector)
            val comparator =
                if (sortDirection == SortDirection.DESCENDING) base.reversed() else base
            rows.sortedWith(comparator)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
    ) {
        val availableWidth = with(LocalDensity.current) { constraints.maxWidth.toDp() }
        val totalMinWidth = columns.sumOf { it.minWidth.value.toDouble() }.dp
        val columnWidths = remember(columns, availableWidth) {
            columns.map { col ->
                if (col.fill && availableWidth > totalMinWidth) {
                    col.minWidth + (availableWidth - totalMinWidth)
                } else {
                    col.minWidth
                }
            }.toImmutableList()
        }

        Column(
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            val totalWidth = columnWidths.sumOf { it.value.toDouble() }.dp

            HeaderRow(
                columns = columns,
                columnWidths = columnWidths,
                sortColumnIndex = sortColumnIndex,
                sortDirection = sortDirection,
                onHeaderClick = { index ->
                    if (sortColumnIndex == index) {
                        sortDirection = when (sortDirection) {
                            SortDirection.DESCENDING -> SortDirection.ASCENDING
                            SortDirection.ASCENDING -> SortDirection.UNSORTED
                            SortDirection.UNSORTED -> SortDirection.DESCENDING
                        }
                    } else {
                        sortColumnIndex = index
                        sortDirection = SortDirection.DESCENDING
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.width(totalWidth))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sorted, key = rowKey) { row ->
                    Row(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .testTag(CoreUiTestTags.TABLE_DATA_ROW),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        columns.forEachIndexed { colIndex, col ->
                            if (cellContent != null) {
                                cellContent(row, colIndex, columnWidths[colIndex])
                            } else {
                                DefaultDataCell(
                                    text = col.text(row),
                                    width = columnWidths[colIndex]
                                )
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.width(totalWidth))
                }
            }
        }
    }
}

@Composable
private fun <T> HeaderRow(
    columns: ImmutableList<ColumnDefinition<T>>,
    columnWidths: ImmutableList<Dp>,
    sortColumnIndex: Int,
    sortDirection: SortDirection,
    onHeaderClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .testTag(CoreUiTestTags.TABLE_HEADER_ROW),
        verticalAlignment = Alignment.CenterVertically
    ) {
        columns.forEachIndexed { index, col ->
            HeaderCell(
                text = col.header,
                isSorted = sortColumnIndex == index,
                sortDirection = sortDirection,
                onClick = { onHeaderClick(index) },
                width = columnWidths[index]
            )
        }
    }
}

@Composable
private fun HeaderCell(
    text: String,
    isSorted: Boolean,
    sortDirection: SortDirection,
    onClick: () -> Unit,
    width: Dp
) {
    Row(
        modifier = Modifier
            .width(width)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (isSorted && sortDirection != SortDirection.UNSORTED) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Sort,
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer {
                        scaleY = if (sortDirection == SortDirection.ASCENDING) -1f else 1f
                    },
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun DefaultDataCell(text: String, width: Dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 4.dp)
    )
}

private data class SampleRow(val id: Int, val name: String, val value: String)

@PreviewLightDark
@Composable
private fun SortableTablePreview() {
    MedTimerTheme {
        Surface {
            SortableTable(
                rows = persistentListOf(
                    SampleRow(1, "Alpha", "100"),
                    SampleRow(2, "Beta", "200"),
                    SampleRow(3, "Gamma", "50"),
                ),
                columns = persistentListOf(
                    ColumnDefinition(
                        header = "ID",
                        minWidth = 60.dp,
                        text = { it.id.toString() },
                        sortKey = { it.id },
                    ),
                    ColumnDefinition(
                        header = "Name",
                        minWidth = 120.dp,
                        text = { it.name },
                        sortKey = { it.name },
                        fill = true,
                    ),
                    ColumnDefinition(
                        header = "Value",
                        minWidth = 80.dp,
                        text = { it.value },
                        sortKey = { it.value },
                    ),
                ),
                rowKey = { it.id },
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}
