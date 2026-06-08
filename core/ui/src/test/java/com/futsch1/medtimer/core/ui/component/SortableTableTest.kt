package com.futsch1.medtimer.core.ui.component

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import androidx.compose.ui.unit.dp

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SortableTableTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val columns = persistentListOf(
        SortableTableColumn("Name", minWidth = 120.dp, fill = true),
        SortableTableColumn("Dosage", minWidth = 80.dp),
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
    fun `custom cell content is used for rendering rows`() {
        composeTestRule.setContent {
            SortableTable(columns = columns, rows = rows) { row, columnIndex, _ ->
                Text(text = "cell-${row.id}-$columnIndex")
            }
        }

        composeTestRule.onNode(hasText("cell-1-0"), useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNode(hasText("cell-2-1"), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `non-sortable column header is not clickable`() {
        val fixedColumns = persistentListOf(
            SortableTableColumn("Name", minWidth = 120.dp, fill = true, sortable = true),
            SortableTableColumn("Fixed", minWidth = 80.dp, sortable = false),
        )

        composeTestRule.setContent {
            SortableTable(columns = fixedColumns, rows = rows)
        }

        // Tapping a non-sortable header must not throw and the header stays present.
        composeTestRule.onNodeWithText("Fixed").performClick()
        composeTestRule.onNodeWithText("Fixed").assertIsDisplayed()
    }

    @Test
    fun `default sort is descending on the first column`() {
        val sorted = sortRows(rows, sortColumn = 0, sortDirection = SortDirection.DESCENDING)

        assertEquals(listOf("Vitamin X 500 mg", "Supplement B", "Medicine A"), sorted.map { it.cells[0].text })
    }

    @Test
    fun `ascending sort reverses the order`() {
        val sorted = sortRows(rows, sortColumn = 0, sortDirection = SortDirection.ASCENDING)

        assertEquals(listOf("Medicine A", "Supplement B", "Vitamin X 500 mg"), sorted.map { it.cells[0].text })
    }

    @Test
    fun `unsorted preserves the input order`() {
        val sorted = sortRows(rows, sortColumn = 0, sortDirection = SortDirection.UNSORTED)

        assertEquals(rows.toList(), sorted)
    }

    @Test
    fun `sort uses the cell sort value when provided`() {
        val byNumericSortValue = persistentListOf(
            SortableTableRow(1, persistentListOf(SortableTableCell("ten", sortValue = 10))),
            SortableTableRow(2, persistentListOf(SortableTableCell("two", sortValue = 2))),
            SortableTableRow(3, persistentListOf(SortableTableCell("thirty", sortValue = 30))),
        )

        val sorted = sortRows(byNumericSortValue, sortColumn = 0, sortDirection = SortDirection.DESCENDING)

        assertTrue(sorted.map { it.cells[0].text } == listOf("thirty", "ten", "two"))
    }
}
