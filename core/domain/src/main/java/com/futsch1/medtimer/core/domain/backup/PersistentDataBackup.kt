package com.futsch1.medtimer.core.domain.backup

data class PersistentDataBackup(
    val analysisDays: Int,
    val iconColor: Int,
    val activeStatisticsFragment: String,
    val lastCustomDose: String,
    val lastCustomDoseAmount: String,
    val filterTags: Set<String>,
    val checkedFilters: Set<String>
)
