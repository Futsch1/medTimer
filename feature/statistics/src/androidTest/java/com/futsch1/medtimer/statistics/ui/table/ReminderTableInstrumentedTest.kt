package com.futsch1.medtimer.statistics.ui.table

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import com.futsch1.medtimer.core.designsystem.MedTimerTheme
import com.futsch1.medtimer.core.ui.CoreUiTestTags
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.statistics.model.ReminderTableRowData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime

class ReminderTableInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun onTableDataText(text: String) =
        composeTestRule.onNode(
            hasText(text) and hasAnyAncestor(hasTestTag(CoreUiTestTags.TABLE_DATA_ROW))
        )

    @Test
    fun tableRendersWithData() {
        composeTestRule.setContent {
            MedTimerTheme {
                ReminderTable(
                    rows = persistentListOf(
                        ReminderTableRowData(1, LocalDateTime.of(2024, 1, 15, 8, 0), ReminderEvent.ReminderStatus.TAKEN, "Aspirin", "100mg", LocalDateTime.of(2024, 1, 15, 7, 55)),
                        ReminderTableRowData(2, null, ReminderEvent.ReminderStatus.RAISED, "Ibuprofen", "200mg", LocalDateTime.of(2024, 1, 15, 9, 0)),
                    ),
                    filterText = "",
                    onFilterTextChanged = {},
                    onEditEvent = {}
                )
            }
        }
        onTableDataText("Aspirin").assertIsDisplayed()
        onTableDataText("Ibuprofen").assertIsDisplayed()
    }

    @Test
    fun tableRecomposesOnDataChange() {
        val mutableRows = mutableStateOf<ImmutableList<ReminderTableRowData>>(
            persistentListOf(
                ReminderTableRowData(1, LocalDateTime.of(2024, 1, 15, 8, 0), ReminderEvent.ReminderStatus.TAKEN, "Aspirin", "100mg", LocalDateTime.of(2024, 1, 15, 7, 55)),
            )
        )

        composeTestRule.setContent {
            MedTimerTheme {
                ReminderTable(
                    rows = mutableRows.value,
                    filterText = "",
                    onFilterTextChanged = {},
                    onEditEvent = {},
                )
            }
        }
        onTableDataText("Aspirin").assertIsDisplayed()

        mutableRows.value = persistentListOf(
            ReminderTableRowData(2, null, ReminderEvent.ReminderStatus.SKIPPED, "Vitamin D", "1000IU", LocalDateTime.of(2024, 1, 15, 12, 0)),
        )
        onTableDataText("Vitamin D").assertIsDisplayed()
    }
}
