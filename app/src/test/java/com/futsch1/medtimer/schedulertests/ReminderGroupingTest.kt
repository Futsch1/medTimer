package com.futsch1.medtimer.schedulertests

import com.futsch1.medtimer.feature.reminders.api.scheduling.groupBySameTime
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals

class ReminderGroupingTest {

    private data class TimedItem(val name: String, val at: Instant)

    private fun itemAt(name: String, epochSecond: Long) = TimedItem(name, Instant.ofEpochSecond(epochSecond))

    private fun groupNames(items: List<TimedItem>) =
        groupBySameTime(items) { it.at.epochSecond }.map { group -> group.map { it.name } }

    @Test
    fun `should put items with an identical time into one group`() {
        val items = listOf(itemAt("A", 1_000), itemAt("B", 1_000), itemAt("C", 1_000))

        val groups = groupNames(items)

        assertEquals(listOf(listOf("A", "B", "C")), groups)
    }

    @Test
    fun `should separate items one second apart`() {
        val items = listOf(itemAt("A", 1_000), itemAt("B", 1_001))

        val groups = groupNames(items)

        assertEquals(listOf(listOf("A"), listOf("B")), groups)
    }

    @Test
    fun `should group unsorted input without reordering it`() {
        val items = listOf(itemAt("A", 2_000), itemAt("B", 1_000), itemAt("C", 2_000), itemAt("D", 1_000))

        val groups = groupNames(items)

        assertEquals(listOf(listOf("A", "C"), listOf("B", "D")), groups)
    }

    @Test
    fun `should return no groups for an empty input`() {
        val groups = groupNames(emptyList())

        assertEquals(emptyList(), groups)
    }

    @Test
    fun `should group by the epoch second a caller already holds`() {
        val timestamps = listOf(1_000L, 1_000L, 1_060L)

        val groups = groupBySameTime(timestamps) { it }

        assertEquals(listOf(listOf(1_000L, 1_000L), listOf(1_060L)), groups)
    }
}
