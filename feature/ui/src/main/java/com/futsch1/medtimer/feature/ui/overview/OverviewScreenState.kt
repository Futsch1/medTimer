package com.futsch1.medtimer.feature.ui.overview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.futsch1.medtimer.core.domain.model.OverviewFilter
import com.futsch1.medtimer.feature.ui.overview.model.OverviewEvent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import java.time.LocalDate

interface OverviewScreenState {
    val day: LocalDate
    val simulatedThrough: LocalDate
    val activeFilters: ImmutableSet<OverviewFilter>
    val combineNotifications: Boolean
    val events: ImmutableList<OverviewEvent>
    val warnings: OverviewWarnings
}

class MutableOverviewScreenState : OverviewScreenState {
    override var day: LocalDate by mutableStateOf(LocalDate.now())
    override var simulatedThrough: LocalDate by mutableStateOf(LocalDate.now())
    override var activeFilters by mutableStateOf<ImmutableSet<OverviewFilter>>(persistentSetOf())
    override var combineNotifications by mutableStateOf(false)
    override var events by mutableStateOf<ImmutableList<OverviewEvent>>(persistentListOf())
    override val warnings = MutableOverviewWarnings()
}
