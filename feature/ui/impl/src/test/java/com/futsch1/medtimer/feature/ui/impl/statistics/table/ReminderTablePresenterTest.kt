package com.futsch1.medtimer.feature.ui.impl.statistics.table

import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.ui.TimeFormatter
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import java.time.Instant
import kotlin.test.assertEquals

// Pure JVM test — no Robolectric. Extracting the presenter turns the table's cell contract into
// something assertable by column meaning rather than only by index/count as the view-model test did.
class ReminderTablePresenterTest {

    private val timeFormatter: TimeFormatter = mock {
        on { toDateTimeString(any<Instant>()) } doAnswer { "fmt-${(it.arguments[0] as Instant).epochSecond}" }
    }
    private val presenter = ReminderTablePresenter(timeFormatter)

    @Test
    fun `present lays cells out in column order and formats both timestamps`() {
        val event = ReminderEvent.default().copy(
            reminderEventId = 42,
            status = ReminderEvent.ReminderStatus.TAKEN,
            medicineName = "Vitamin X 500 mg",
            amount = "1 tablet",
            processedTimestamp = Instant.ofEpochSecond(2000),
            remindedTimestamp = Instant.ofEpochSecond(1000),
        )

        val rows = presenter.present(listOf(event))

        assertEquals(1, rows.size)
        assertEquals(42L, rows[0].id)
        val cells = rows[0].cells
        assertEquals("fmt-2000", cells[0].text)         // taken timestamp (processed)
        assertEquals(Instant.ofEpochSecond(2000), cells[0].sortValue)
        assertEquals("Vitamin X 500 mg", cells[1].text) // medicine name
        assertEquals("1 tablet", cells[2].text)         // amount
        assertEquals("fmt-1000", cells[3].text)         // reminded timestamp
        assertEquals(Instant.ofEpochSecond(1000), cells[3].sortValue)
    }

    @Test
    fun `a non-taken event shows a dash for the taken cell`() {
        val event = ReminderEvent.default().copy(
            status = ReminderEvent.ReminderStatus.SKIPPED,
            medicineName = "Medicine A",
            processedTimestamp = Instant.ofEpochSecond(2000),
        )

        val rows = presenter.present(listOf(event))

        assertEquals("-", rows[0].cells[0].text)
    }

    @Test
    fun `present maps every event to a row, preserving order`() {
        val first = ReminderEvent.default().copy(reminderEventId = 1, medicineName = "Vitamin X")
        val second = ReminderEvent.default().copy(reminderEventId = 2, medicineName = "Medicine A")

        val rows = presenter.present(listOf(first, second))

        assertEquals(listOf(1L, 2L), rows.map { it.id })
        assertEquals(listOf("Vitamin X", "Medicine A"), rows.map { it.cells[1].text })
    }
}
