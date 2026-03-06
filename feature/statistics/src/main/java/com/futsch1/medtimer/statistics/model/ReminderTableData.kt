package com.futsch1.medtimer.statistics.model

import kotlinx.collections.immutable.ImmutableList

data class ReminderTableData(
    val rows: ImmutableList<ReminderTableRowData>,
    val columnHeaders: ImmutableList<String>
)