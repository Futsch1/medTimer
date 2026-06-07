package com.futsch1.medtimer.core.ui.filter

import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.Tag
import org.junit.Test
import kotlin.test.assertEquals

class TagEventFilterTest {

    private val filter = TagEventFilter()

    private fun event(vararg tags: String): ReminderEvent =
        ReminderEvent.default().copy(tags = tags.toList())

    @Test
    fun `empty selection returns all events`() {
        val events = listOf(event("Vitamins"), event("Supplements"))

        val result = filter.filter(events, emptySet(), listOf(Tag("Vitamins", 1)))

        assertEquals(events, result)
    }

    @Test
    fun `selected tag id keeps only events with matching tag name`() {
        val vitaminEvent = event("Vitamins")
        val supplementEvent = event("Supplements")
        val tags = listOf(Tag("Vitamins", 1), Tag("Supplements", 2))

        val result = filter.filter(listOf(vitaminEvent, supplementEvent), setOf(1), tags)

        assertEquals(listOf(vitaminEvent), result)
    }

    @Test
    fun `event with any matching tag is included`() {
        val event = event("Vitamins", "Energy")
        val tags = listOf(Tag("Energy", 1), Tag("Supplements", 2))

        val result = filter.filter(listOf(event), setOf(1), tags)

        assertEquals(listOf(event), result)
    }

    @Test
    fun `event with no matching tag is excluded`() {
        val event = event("Vitamins")
        val tags = listOf(Tag("Energy", 1), Tag("Supplements", 2))

        val result = filter.filter(listOf(event), setOf(1), tags)

        assertEquals(emptyList(), result)
    }

    @Test
    fun `unknown tag id excludes all events`() {
        val events = listOf(event("Vitamins"))
        val tags = listOf(Tag("Vitamins", 1))

        // ID 99 is not in the tag list → selectedTagNames is empty → no event passes
        val result = filter.filter(events, setOf(99), tags)

        assertEquals(emptyList(), result)
    }

    @Test
    fun `multiple selected ids include events matching any`() {
        val vitaminEvent = event("Vitamins")
        val supplementEvent = event("Supplements")
        val untaggedEvent = event("Other")
        val tags = listOf(Tag("Vitamins", 1), Tag("Supplements", 2))

        val result = filter.filter(
            listOf(vitaminEvent, supplementEvent, untaggedEvent),
            setOf(1, 2),
            tags,
        )

        assertEquals(listOf(vitaminEvent, supplementEvent), result)
    }
}
