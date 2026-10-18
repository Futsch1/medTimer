package com.futsch1.medtimer.feature.ui.overview

import com.futsch1.medtimer.core.domain.model.OverviewFilter
import com.futsch1.medtimer.feature.ui.overview.model.OverviewState
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OverviewFilterVisibilityTest {

    @Test
    fun `lists every event when no filter is checked`() {
        OverviewState.entries.forEach { assertTrue(isListedWithFilters(it, emptySet()), "$it") }
    }

    @Test
    fun `lists a raised or location event only with the raised filter`() {
        assertTrue(isListedWithFilters(OverviewState.RAISED, setOf(OverviewFilter.RAISED)))
        assertTrue(isListedWithFilters(OverviewState.LOCATION, setOf(OverviewFilter.RAISED)))
        assertFalse(isListedWithFilters(OverviewState.RAISED, setOf(OverviewFilter.TAKEN, OverviewFilter.SKIPPED)))
    }

    @Test
    fun `maps pending, taken and skipped events to their filters`() {
        assertTrue(isListedWithFilters(OverviewState.PENDING, setOf(OverviewFilter.SCHEDULED)))
        assertTrue(isListedWithFilters(OverviewState.TAKEN, setOf(OverviewFilter.TAKEN)))
        assertTrue(isListedWithFilters(OverviewState.SKIPPED, setOf(OverviewFilter.SKIPPED)))
        assertFalse(isListedWithFilters(OverviewState.TAKEN, setOf(OverviewFilter.SCHEDULED)))
    }
}
