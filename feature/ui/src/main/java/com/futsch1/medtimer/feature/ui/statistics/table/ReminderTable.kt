package com.futsch1.medtimer.feature.ui.statistics.table

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.component.SortableTable
import com.futsch1.medtimer.core.ui.component.SortableTableColumn
import com.futsch1.medtimer.core.ui.component.SortableTableRow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ReminderTable(
    rows: ImmutableList<SortableTableRow>,
    onEditEvent: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val columns = persistentListOf(
        SortableTableColumn(title = stringResource(R.string.taken), weight = 1.4f),
        SortableTableColumn(title = stringResource(R.string.name), weight = 1.4f),
        SortableTableColumn(title = stringResource(R.string.dosage), weight = 1f),
        SortableTableColumn(title = stringResource(R.string.reminded), weight = 1.4f),
    )

    SortableTable(
        columns = columns,
        rows = rows,
        filterLabel = stringResource(R.string.filter),
        onRowClick = { onEditEvent(it.id.toInt()) },
        modifier = modifier.fillMaxSize(),
    )
}
