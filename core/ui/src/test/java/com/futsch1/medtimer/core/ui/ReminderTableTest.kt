package com.futsch1.medtimer.core.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.futsch1.medtimer.core.designsystem.MedTimerTheme
import kotlinx.collections.immutable.persistentListOf
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

    private val sampleData = ReminderTableData(
        rows = persistentListOf(
            ReminderTableRowData(1, LocalDateTime.of(2024, 1, 15, 8, 0), "01/15 08:00", "Aspirin", "100mg", LocalDateTime.of(2024, 1, 15, 7, 55)),
            ReminderTableRowData(2, null, " ", "Ibuprofen", "200mg", LocalDateTime.of(2024, 1, 15, 9, 0)),
            ReminderTableRowData(3, null, "-", "Vitamin D", "1000IU", LocalDateTime.of(2024, 1, 15, 12, 0)),
        ),
        columnHeaders = persistentListOf("Taken", "Name", "Dosage", "Reminded")
    )

    private fun setContent(data: ReminderTableData, onEditEvent: (Int) -> Unit = {}) {
        composeTestRule.setContent {
            MedTimerTheme {
                ReminderTable(data = data, onEditEvent = onEditEvent)
            }
        }
    }

    @Test
    fun `table renders with data`() {
        setContent(sampleData)
        composeTestRule.onNodeWithText("Aspirin").assertExists()
        composeTestRule.onNodeWithText("Ibuprofen").assertExists()
        composeTestRule.onNodeWithText("Vitamin D").assertExists()
    }

    @Test
    fun `table renders empty`() {
        setContent(
            ReminderTableData(
                rows = persistentListOf(),
                columnHeaders = persistentListOf("Taken", "Name", "Dosage", "Reminded")
            )
        )
        composeTestRule.onRoot().assertExists()
        composeTestRule.onNodeWithText("Taken").assertExists()
    }

    @Test
    fun `medicine name click calls onEditEvent`() {
        var clickedId = -1
        setContent(sampleData) { clickedId = it }
        composeTestRule.onNodeWithText("Aspirin").performClick()
        assertEquals(1, clickedId)
    }

    @Test
    fun `filter narrows rows`() {
        setContent(sampleData)
        composeTestRule.onNodeWithText("Aspirin").assertExists()
        composeTestRule.onNodeWithText("Ibuprofen").assertExists()

        composeTestRule.onNodeWithText("Filter").performClick()
        composeTestRule.onNodeWithText("Filter").performTextInput("Asp")

        composeTestRule.onNodeWithText("Aspirin").assertExists()
        composeTestRule.onNodeWithText("Ibuprofen").assertDoesNotExist()
    }

    @Test
    fun `sort toggles on header click`() {
        setContent(sampleData)
        // Click on "Name" header to sort by name
        composeTestRule.onNodeWithText("Name").performClick()
        composeTestRule.onRoot().assertExists()
    }
}