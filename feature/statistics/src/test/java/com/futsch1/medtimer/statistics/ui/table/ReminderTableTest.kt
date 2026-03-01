package com.futsch1.medtimer.statistics.ui.table

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.futsch1.medtimer.core.designsystem.MedTimerTheme
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.statistics.model.ReminderTableRowData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class ReminderTableTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleRows = persistentListOf(
        ReminderTableRowData(1, LocalDateTime.of(2024, 1, 15, 8, 0), ReminderEvent.ReminderStatus.TAKEN, "Aspirin", "100mg", LocalDateTime.of(2024, 1, 15, 7, 55)),
        ReminderTableRowData(2, null, ReminderEvent.ReminderStatus.RAISED, "Ibuprofen", "200mg", LocalDateTime.of(2024, 1, 15, 9, 0)),
        ReminderTableRowData(3, null, ReminderEvent.ReminderStatus.SKIPPED, "Vitamin D", "1000IU", LocalDateTime.of(2024, 1, 15, 12, 0)),
    )

    private fun setContent(
        rows: ImmutableList<ReminderTableRowData> = sampleRows,
        filterText: String = "",
        onFilterTextChanged: (String) -> Unit = {},
        onEditEvent: (Int) -> Unit = {},
    ) {
        composeTestRule.setContent {
            MedTimerTheme {
                ReminderTable(
                    rows = rows,
                    filterText = filterText,
                    onFilterTextChanged = onFilterTextChanged,
                    onEditEvent = onEditEvent,
                )
            }
        }
    }

    @Test
    fun `table renders with data`() {
        setContent()
        composeTestRule.onNodeWithText("Aspirin").assertExists()
        composeTestRule.onNodeWithText("Ibuprofen").assertExists()
        composeTestRule.onNodeWithText("Vitamin D").assertExists()
    }

    @Test
    fun `table renders empty`() {
        setContent(rows = persistentListOf())
        composeTestRule.onRoot().assertExists()
        composeTestRule.onNodeWithText("Taken").assertExists()
    }

    @Test
    fun `medicine name click calls onEditEvent`() {
        var clickedId = -1
        setContent(onEditEvent = { clickedId = it })
        composeTestRule.onNodeWithText("Aspirin").performClick()
        assertEquals(1, clickedId)
    }

    @Test
    fun `filter narrows rows`() {
        // Pre-filtered rows simulate ViewModel filtering
        val filteredRows = sampleRows.filter {
            it.medicineName.lowercase().contains("asp")
        }.toImmutableList()
        setContent(rows = filteredRows, filterText = "Asp")
        composeTestRule.onNodeWithText("Aspirin").assertExists()
        composeTestRule.onNodeWithText("Ibuprofen").assertDoesNotExist()
    }

    @Test
    fun `sort toggles on header click`() {
        setContent()
        // Click on "Name" header to sort by name
        composeTestRule.onNodeWithText("Name").performClick()
        composeTestRule.onRoot().assertExists()
    }
}
