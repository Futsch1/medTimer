package com.futsch1.medtimer.feature.ui.overview.model

import com.futsch1.medtimer.feature.ui.overview.TestOverviewEvent
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OverviewGroupTest {

    private val nine = TestOverviewEvent(id = 1, timestamp = 32_400, medicineName = "A")
    private val nineToo = TestOverviewEvent(id = 2, timestamp = 32_400, medicineName = "B")
    private val ten = TestOverviewEvent(id = 3, timestamp = 36_000, medicineName = "C")

    @Test
    fun `should put events of the same time into one group when grouping by reminder time`() {
        val groups = groupOverviewEvents(listOf(nine, nineToo, ten), groupByReminderTime = true)

        assertEquals(listOf(32_400L, 36_000L), groups.map { it.timestamp })
        assertEquals(listOf(listOf(1, 2), listOf(3)), groups.map { group -> group.events.map { it.id } })
    }

    @Test
    fun `should give every event its own group when not grouping by reminder time`() {
        val groups = groupOverviewEvents(listOf(nine, nineToo, ten), groupByReminderTime = false)

        assertEquals(listOf(32_400L, 32_400L, 36_000L), groups.map { it.timestamp })
        assertEquals(listOf(listOf(1), listOf(2), listOf(3)), groups.map { group -> group.events.map { it.id } })
    }

    @Test
    fun `should return no groups for a day without events`() {
        assertTrue(groupOverviewEvents(emptyList(), groupByReminderTime = true).isEmpty())
    }

    @Test
    fun `should order the events of one time by reminder whatever their ids`() {
        val first = TestOverviewEvent(id = 900, timestamp = 32_400, medicineName = "A", reminderId = 1)
        val second = TestOverviewEvent(id = 901, timestamp = 32_400, medicineName = "B", reminderId = 2)
        val third = TestOverviewEvent(id = 41, timestamp = 32_400, medicineName = "C", state = OverviewState.TAKEN, reminderId = 3)

        val sorted = listOf(third, second, first).sortedWith(overviewEventOrder)

        assertEquals(listOf(1, 2, 3), sorted.map { it.reminderId })
    }

    @Test
    fun `should order events of the same reminder and time by id`() {
        val later = TestOverviewEvent(id = 7, timestamp = 32_400, medicineName = "A", reminderId = 1)
        val earlier = TestOverviewEvent(id = 5, timestamp = 32_400, medicineName = "A", reminderId = 1)
        val sooner = TestOverviewEvent(id = 9, timestamp = 28_800, medicineName = "B", reminderId = 2)

        val sorted = listOf(later, earlier, sooner).sortedWith(overviewEventOrder)

        assertEquals(listOf(9, 5, 7), sorted.map { it.id })
    }

    @Test
    fun `should keep a taken dose in its reminder slot inside its group`() {
        val a = TestOverviewEvent(id = 900, timestamp = 32_400, medicineName = "A", reminderId = 1)
        val b = TestOverviewEvent(id = 42, timestamp = 32_400, medicineName = "B", state = OverviewState.TAKEN, reminderId = 2)
        val c = TestOverviewEvent(id = 902, timestamp = 32_400, medicineName = "C", reminderId = 3)
        val events = listOf(c, b, a).sortedWith(overviewEventOrder)

        val groups = groupOverviewEvents(events, groupByReminderTime = true)

        assertEquals(listOf(listOf("A", "B", "C")), groups.map { group -> group.events.map { it.content.medicineName } })
    }
}
