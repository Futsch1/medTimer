package com.futsch1.medtimer.model

import java.time.LocalDate

enum class StatisticFragment {
    CHARTS,
    TABLE,
    CALENDAR
}

enum class OverviewFilter {
    TAKEN, SKIPPED, SCHEDULED, RAISED
}

data class PersistentData(
    val showNotifications: Boolean,
    val iconColor: Int,
    val activeStatisticsFragment: StatisticFragment,
    val analysisDays: Int,
    val batteryWarningShown: Boolean,
    val introShown: Boolean,
    val lastAutomaticBackup: LocalDate,
    val notificationId: Int,
    val lastCustomDose: String,
    val lastCustomDoseAmount: String,
    val filterTags: Set<String>,
    val checkedFilters: Set<OverviewFilter>
) {
    companion object {
        fun default(): PersistentData {
            return PersistentData(
                showNotifications = true,
                iconColor = 0,
                activeStatisticsFragment = StatisticFragment.CHARTS,
                analysisDays = 7,
                batteryWarningShown = false,
                introShown = false,
                lastAutomaticBackup = LocalDate.EPOCH,
                notificationId = 0,
                lastCustomDose = "",
                lastCustomDoseAmount = "",
                filterTags = emptySet(),
                checkedFilters = emptySet()
            )
        }
    }
}
