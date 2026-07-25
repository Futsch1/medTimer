package com.futsch1.medtimer.core.ui.component

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SelectableCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `long press enters selection mode instead of activating the item`() {
        var entered = false
        var clicked = false
        composeTestRule.setContent {
            MedTimerTheme {
                SelectableCard(
                    isSelected = false,
                    isInSelectionMode = false,
                    onClick = { clicked = true },
                    onToggleSelection = {},
                    onEnterSelectionMode = { entered = true },
                ) { Text("item") }
            }
        }

        composeTestRule.onNodeWithText("item").performTouchInput { longClick() }

        assertTrue(entered, "long press must enter selection mode")
        assertTrue(!clicked, "long press must not also activate the item")
    }

    /**
     * Entering selection mode selects the pressed item itself, so long press must not also toggle it
     * — doing both cancels out and drops straight back out of selection mode.
     */
    @Test
    fun `long press outside selection mode does not toggle as well as enter`() {
        var toggles = 0
        var entered = 0
        composeTestRule.setContent {
            MedTimerTheme {
                SelectableCard(
                    isSelected = false,
                    isInSelectionMode = false,
                    onClick = {},
                    onToggleSelection = { toggles++ },
                    onEnterSelectionMode = { entered++ },
                ) { Text("item") }
            }
        }

        composeTestRule.onNodeWithText("item").performTouchInput { longClick() }

        assertEquals(1, entered)
        assertEquals(0, toggles)
    }

    @Test
    fun `long press inside selection mode toggles the item`() {
        var toggles = 0
        composeTestRule.setContent {
            MedTimerTheme {
                SelectableCard(
                    isSelected = false,
                    isInSelectionMode = true,
                    onClick = {},
                    onToggleSelection = { toggles++ },
                    onEnterSelectionMode = {},
                ) { Text("item") }
            }
        }

        composeTestRule.onNodeWithText("item").performTouchInput { longClick() }

        assertEquals(1, toggles)
    }
}
