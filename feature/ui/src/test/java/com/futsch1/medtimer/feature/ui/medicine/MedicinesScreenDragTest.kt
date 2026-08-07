package com.futsch1.medtimer.feature.ui.medicine

import androidx.compose.material3.Surface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import com.futsch1.medtimer.core.ui.R as CoreUiR

/**
 * The drag handle sits at the right edge of a clickable card. The card merges the handle's content
 * description into itself, so a merged-tree match is the whole card and a touch at its centre never
 * reaches the handle - which silently turns the reorder gesture into a no-op.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MedicinesScreenDragTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val moves = mutableListOf<Pair<Int, Int>>()

    private fun showTwoMedicines() {
        val state = MutableMedicineScreenState().apply {
            medicines = persistentListOf(
                medicineItem(1, "First"),
                medicineItem(2, "Second"),
            )
        }
        composeTestRule.setContent {
            MedTimerTheme {
                Surface {
                    MedicinesScreen(state, onMedicineMove = { id, position -> moves += id to position })
                }
            }
        }
    }

    private fun medicineItem(id: Int, name: String) = MedicineScreenItem(
        id = id,
        name = name,
        reminderTimes = persistentListOf("8:00"),
        stockState = StockState(null, false, null),
        icon = null,
        color = null,
        tags = persistentListOf(),
    )

    private fun dragHandles(useUnmergedTree: Boolean): SemanticsNodeInteractionCollection =
        composeTestRule.onAllNodesWithContentDescription(
            ApplicationProvider.getApplicationContext<android.content.Context>()
                .getString(CoreUiR.string.move_medicine),
            useUnmergedTree = useUnmergedTree,
        )

    /** Mirrors MedicinesRobot.dragItem: a slow, many-step drag down the handle column. */
    private fun dragFirstOntoSecond(useUnmergedTree: Boolean) {
        val handles = dragHandles(useUnmergedTree).fetchSemanticsNodes()
            .withIndex()
            .map { it.index to it.value.boundsInRoot.center.y }
            .sortedBy { it.second }
        val distance = handles[1].second - handles[0].second
        dragHandles(useUnmergedTree)[handles[0].first].performTouchInput {
            down(center)
            repeat(DRAG_STEPS) { moveBy(Offset(0f, distance / DRAG_STEPS)) }
            up()
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun dragOnUnmergedHandleReordersTheList() {
        showTwoMedicines()

        dragFirstOntoSecond(useUnmergedTree = true)

        assertEquals(listOf(1 to 1), moves, "dragging the handle must move medicine 1 to position 1")
    }

    @Test
    fun dragOnTheMergedCardDoesNotReachTheHandle() {
        showTwoMedicines()

        dragFirstOntoSecond(useUnmergedTree = false)

        assertEquals(emptyList(), moves, "the merged match is the whole card, so its centre misses the handle")
    }

    private companion object {
        const val DRAG_STEPS = 10
    }
}
