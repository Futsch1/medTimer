package com.futsch1.medtimer.new_overview

import com.futsch1.medtimer.R
import com.google.android.material.button.MaterialButtonToggleGroup

class FilterToggleGroup(toggleGroup: MaterialButtonToggleGroup, val overviewViewModel: OverviewViewModel) {
    init {
        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.filterTaken -> overviewViewModel.activeFilters.add(OverviewFilterToggles.TAKEN)
                    R.id.filterSkipped -> overviewViewModel.activeFilters.add(OverviewFilterToggles.SKIPPED)
                    R.id.filterScheduled -> overviewViewModel.activeFilters.add(OverviewFilterToggles.SCHEDULED)
                    R.id.filterRaised -> overviewViewModel.activeFilters.add(OverviewFilterToggles.RAISED)
                }
            } else {
                when (checkedId) {
                    R.id.filterTaken -> overviewViewModel.activeFilters.remove(OverviewFilterToggles.TAKEN)
                    R.id.filterSkipped -> overviewViewModel.activeFilters.remove(OverviewFilterToggles.SKIPPED)
                    R.id.filterScheduled -> overviewViewModel.activeFilters.remove(OverviewFilterToggles.SCHEDULED)
                    R.id.filterRaised -> overviewViewModel.activeFilters.remove(OverviewFilterToggles.RAISED)
                }
            }
            overviewViewModel.update()
        }
    }
}