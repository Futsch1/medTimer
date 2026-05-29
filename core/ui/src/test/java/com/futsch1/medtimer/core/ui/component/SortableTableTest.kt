package com.futsch1.medtimer.core.ui.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SortableTableTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val columns = persistentListOf(
        SortableTableColumn("Name"),
        SortableTableColumn("Dosage"),
    )

    private val rows = persistentListOf(
        SortableTableRow(1, persistentListOf(SortableTableCell("Vitamin X 500 mg"), SortableTableCell("1 tablet"))),
        SortableTableRow(2, persistentListOf(SortableTableCell("Medicine A"), SortableTableCell("2 ml"))),
        SortableTableRow(3, persistentListOf(SortableTableCell("Supplement B"), SortableTableCell("500 mg"))),
    )

    @Test
    fun `all rows are shown by default`() {
        composeTestRule.setContent {
            SortableTable(columns = columns, rows = rows)
        }

        composeTestRule.onNode(hasText("Vitamin X 500 mg"), useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNode(hasText("Medicine A"), useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNode(hasText("Supplement B"), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `filter reduces visible rows`() {
        composeTestRule.setContent {
            SortableTable(columns = columns, rows = rows, filterLabel = "Filter")
        }

        composeTestRule.onNode(hasSetTextAction()).performTextInput("Vitam")

        composeTestRule.onNode(hasText("Vitamin X 500 mg"), useUnmergedTree = true).assertIsDisplayed()
        assertTrue(composeTestRule.onAllNodes(hasText("Medicine A"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        assertTrue(composeTestRule.onAllNodes(hasText("Supplement B"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `clear filter restores all rows`() {
        composeTestRule.setContent {
            SortableTable(columns = columns, rows = rows, filterLabel = "Filter")
        }

        composeTestRule.onNode(hasSetTextAction()).performTextInput("Vitam")
        composeTestRule.onNodeWithText("✕").performClick()

        composeTestRule.onNode(hasText("Vitamin X 500 mg"), useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNode(hasText("Medicine A"), useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNode(hasText("Supplement B"), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `tapping sortable header shows ascending indicator`() {
        // Default sortColumn=0 ("Name"), so "Dosage" (column 1) starts without an arrow.
        composeTestRule.setContent {
            SortableTable(columns = columns, rows = rows)
        }

        composeTestRule.onNodeWithText("Dosage").performClick()

        composeTestRule.onNodeWithText("Dosage ↑").assertIsDisplayed()
    }

    @Test
    fun `tapping header twice shows descending indicator`() {
        composeTestRule.setContent {
            SortableTable(columns = columns, rows = rows)
        }

        composeTestRule.onNodeWithText("Dosage").performClick()
        composeTestRule.onNodeWithText("Dosage ↑").performClick()

        composeTestRule.onNodeWithText("Dosage ↓").assertIsDisplayed()
    }

    @Test
    fun `row click invokes callback with the row id`() {
        var clickedId: Long? = null

        composeTestRule.setContent {
            SortableTable(
                columns = columns,
                rows = rows,
                onRowClick = { clickedId = it.id },
            )
        }

        composeTestRule.onNode(hasText("Medicine A"), useUnmergedTree = true).performClick()

        assertEquals(2L, clickedId)
    }

    @Test
    fun `non-sortable column header is not clickable`() {
        val fixedColumns = persistentListOf(
            SortableTableColumn("Name", sortable = true),
            SortableTableColumn("Fixed", sortable = false),
        )

        composeTestRule.setContent {
            SortableTable(columns = fixedColumns, rows = rows)
        }

        // Clicking non-sortable header should not change any sort indicator
        composeTestRule.onNodeWithText("Fixed").performClick()

        assertTrue(composeTestRule.onAllNodes(hasText("Fixed ↑")).fetchSemanticsNodes().isEmpty())
        assertTrue(composeTestRule.onAllNodes(hasText("Fixed ↓")).fetchSemanticsNodes().isEmpty())
    }
}
