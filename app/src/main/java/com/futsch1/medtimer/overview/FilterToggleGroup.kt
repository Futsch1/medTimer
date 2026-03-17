package com.futsch1.medtimer.overview

import com.futsch1.medtimer.R
import com.futsch1.medtimer.model.OverviewFilter
import com.futsch1.medtimer.preferences.PersistentDataDataSource
import com.google.android.material.button.MaterialButtonToggleGroup

class FilterToggleGroup(
    private val toggleGroup: MaterialButtonToggleGroup,
    private val overviewViewModel: OverviewViewModel,
    private val persistentDataDataSource: PersistentDataDataSource
) {
    private val filterMap: Map<Int, OverviewFilter> = mapOf(
        R.id.filterTaken to OverviewFilter.TAKEN,
        R.id.filterSkipped to OverviewFilter.SKIPPED,
        R.id.filterScheduled to OverviewFilter.SCHEDULED,
        R.id.filterRaised to OverviewFilter.RAISED
    )

    init {
        restoreCheckedFilters(persistentDataDataSource.data.value.checkedFilters)

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            filterMap[checkedId]?.let { filterEnum ->
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
        val checkedFilters = persistentDataDataSource.data.value.checkedFilters.toMutableSet()
        // Get the Pair (enum, mask) from the map
        filterMap[checkedId]?.let { filterEnum ->
            if (isChecked) {
                checkedFilters.add(filterEnum)
            } else {
                checkedFilters.remove(filterEnum)
            }
        }
        persistentDataDataSource.setCheckedFilters(checkedFilters)
    }

    fun restoreCheckedFilters(checkedFilters: Set<OverviewFilter>) {
        if (checkedFilters.isEmpty()) {
            toggleGroup.clearChecked()
            overviewViewModel.setFilters(emptySet())
        } else {
            val restoredFilters = mutableSetOf<OverviewFilter>()
            filterMap.forEach { (buttonId, filterEnum) ->
                if (checkedFilters.contains(filterEnum)) {
                    toggleGroup.check(buttonId)
                    restoredFilters.add(filterEnum)
                }
            }
            overviewViewModel.setFilters(restoredFilters)
        }
    }
}
