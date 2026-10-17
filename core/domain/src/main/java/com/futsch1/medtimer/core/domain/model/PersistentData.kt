package com.futsch1.medtimer.core.domain.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate

enum class StatisticFragment {
    CHARTS,
    TABLE,
    CALENDAR,
    LEVELS
}

enum class OverviewFilter {
    TAKEN, SKIPPED, SCHEDULED, RAISED
}

class ManualDoses(encodedString: String, val maxDoses: Int = 3) {
    val doses: MutableList<Pair<String, String>> = decode(encodedString).take(maxDoses).toMutableList()

    fun addDose(dose: Pair<String, String>) {
        if (maxDoses <= 0) return
        doses.add(0, dose)
        while (doses.size > maxDoses) {
            doses.removeAt(doses.lastIndex)
        }
    }

    fun encode(): String {
        return Gson().toJson(doses)
    }

    private fun decode(encodedString: String): MutableList<Pair<String, String>> {
        val list: List<Pair<String, String>> = try {
            val type = object : TypeToken<List<Pair<String, String>>>() {}.type
            Gson().fromJson(encodedString, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        return list.toMutableList()
    }
}

data class PersistentData(
    val showNotifications: Boolean,
    val iconColor: Int,
    val activeStatisticsFragment: StatisticFragment,
    val analysisDays: Int,
    val batteryWarningShown: Boolean,
    val exactRemindersWarningShown: Boolean,
    val introShown: Boolean,
    val lastAutomaticBackup: LocalDate,
    val notificationId: Int,
    val lastManualDoses: ManualDoses,
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
                exactRemindersWarningShown = false,
                introShown = false,
                lastAutomaticBackup = LocalDate.EPOCH,
                notificationId = 0,
                lastManualDoses = ManualDoses(""),
                filterTags = emptySet(),
                checkedFilters = emptySet()
            )
        }
    }
}
