package com.futsch1.medtimer.remindertable

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.futsch1.medtimer.core.designsystem.MedTimerTheme
import com.futsch1.medtimer.core.ui.ReminderTable
import com.futsch1.medtimer.core.ui.ReminderTableData
import com.futsch1.medtimer.core.ui.ReminderTableRowData
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime

class ReminderTableInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tableRendersWithData() {
        composeTestRule.setContent {
            MedTimerTheme {
                ReminderTable(
                    data = ReminderTableData(
                        rows = persistentListOf(
                            ReminderTableRowData(1, LocalDateTime.of(2024, 1, 15, 8, 0), "01/15 08:00", "Aspirin", "100mg", LocalDateTime.of(2024, 1, 15, 7, 55)),
                            ReminderTableRowData(2, null, " ", "Ibuprofen", "200mg", LocalDateTime.of(2024, 1, 15, 9, 0)),
                        ),
                        columnHeaders = persistentListOf("Taken", "Name", "Dosage", "Reminded")
                    ),
                    onEditEvent = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Aspirin").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ibuprofen").assertIsDisplayed()
    }

    @Test
    fun tableRecomposesOnDataChange() {
        val mutableData = androidx.compose.runtime.mutableStateOf(
            ReminderTableData(
                rows = persistentListOf(
                    ReminderTableRowData(1, LocalDateTime.of(2024, 1, 15, 8, 0), "01/15 08:00", "Aspirin", "100mg", LocalDateTime.of(2024, 1, 15, 7, 55)),
                ),
                columnHeaders = persistentListOf("Taken", "Name", "Dosage", "Reminded")
            )
        )

        composeTestRule.setContent {
            MedTimerTheme {
                ReminderTable(data = mutableData.value, onEditEvent = {})
            }
        }
        composeTestRule.onNodeWithText("Aspirin").assertIsDisplayed()

        mutableData.value = ReminderTableData(
            rows = persistentListOf(
                ReminderTableRowData(2, null, "-", "Vitamin D", "1000IU", LocalDateTime.of(2024, 1, 15, 12, 0)),
            ),
            columnHeaders = persistentListOf("Taken", "Name", "Dosage", "Reminded")
        )
        composeTestRule.onNodeWithText("Vitamin D").assertIsDisplayed()
    }
}