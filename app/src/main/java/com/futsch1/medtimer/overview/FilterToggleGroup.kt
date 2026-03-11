package com.futsch1.medtimer.overview

import android.content.SharedPreferences
import androidx.core.content.edit
import com.futsch1.medtimer.R
import com.google.android.material.button.MaterialButtonToggleGroup

class FilterToggleGroup(
    private val toggleGroup: MaterialButtonToggleGroup,
    private val overviewViewModel: OverviewViewModel,
    private val sharedPreferences: SharedPreferences // Consider using SharedPreferences from androidx.preference
) {
    private val preferencesKey = "checkedFilters"

    // Map button IDs to a Pair: the filter enum and its bitmask for SharedPreferences
    private val filterMap: Map<Int, Pair<OverviewFilterToggles, Int>> = mapOf(
        R.id.filterTaken to Pair(OverviewFilterToggles.TAKEN, 1),
        R.id.filterSkipped to Pair(OverviewFilterToggles.SKIPPED, 2),
        R.id.filterScheduled to Pair(OverviewFilterToggles.SCHEDULED, 4),
        R.id.filterRaised to Pair(OverviewFilterToggles.RAISED, 8)
    )

    init {
        restoreCheckedFilters(sharedPreferences.getInt(preferencesKey, 0))

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            filterMap[checkedId]?.let { (filterEnum, _) ->
                if (isChecked) {
                    overviewViewModel.addFilter(filterEnum)
                } else {
                    overviewViewModel.removeFilter(filterEnum)
                }
                saveCheckedFilters(checkedId, isChecked)
            }
        }
    }

    private fun saveCheckedFilters(checkedId: Int, isChecked: Boolean) {
        var checkedFilters = sharedPreferences.getInt(preferencesKey, 0)
        // Get the Pair (enum, mask) from the map
        filterMap[checkedId]?.let { (_, mask) -> // We only need the mask here
            checkedFilters = if (isChecked) {
                checkedFilters or mask
            } else {
                checkedFilters and mask.inv()
            }
        }
        sharedPreferences.edit { putInt(preferencesKey, checkedFilters) }
    }

    fun restoreCheckedFilters(filtersMask: Int) {
        if (filtersMask == 0) {
            toggleGroup.clearChecked()
            overviewViewModel.setFilters(emptySet())
        } else {
            val restoredFilters = mutableSetOf<OverviewFilterToggles>()
            filterMap.forEach { (buttonId, entry) ->
                val filterEnum = entry.first
                val mask = entry.second
                if (filtersMask and mask == mask) {
                    toggleGroup.check(buttonId)
                    restoredFilters.add(filterEnum)
                }
            }
            overviewViewModel.setFilters(restoredFilters)
        }
    }
}
