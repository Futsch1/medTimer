package com.futsch1.medtimer.model

import android.graphics.Color
import android.net.Uri
import java.time.Instant

enum class StatisticFragment {
    CHARTS,
    TABLE,
    CALENDAR
}

data class MedTimerPersistentData(
    val showNotifications: Boolean,
    val iconColor: Color,
    val activeStatisticsFragment: StatisticFragment,
    val analysisDays: Int,
    val batteryWarningDismissed: Boolean,
    val lastAutomaticBackup: Instant,
    val automaticBackupDirectory: Uri?,
    val notificationId: Int,
    val lastCustomDose: String,
    val lastCustomDoseAmount: String
) {
    companion object {
        fun default(): MedTimerPersistentData {
            return MedTimerPersistentData(
                showNotifications = true,
                iconColor = Color.valueOf(Color.BLACK),
                activeStatisticsFragment = StatisticFragment.CHARTS,
                analysisDays = 7,
                batteryWarningDismissed = false,
                lastAutomaticBackup = Instant.EPOCH,
                automaticBackupDirectory = null,
                notificationId = 0,
                lastCustomDose = "",
                lastCustomDoseAmount = ""
            )
        }
    }
}
