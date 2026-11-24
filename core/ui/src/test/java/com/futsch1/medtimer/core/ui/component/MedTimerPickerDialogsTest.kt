package com.futsch1.medtimer.core.ui.component

import android.provider.Settings
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MedTimerPickerDialogsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var pickedTime: LocalTime? = null
    private var pickedDate: LocalDate? = null
    private var dismissed = false

    private fun setTimeFormat(format: String) {
        Settings.System.putString(ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver, Settings.System.TIME_12_24, format)
    }

    private fun showTimePicker(initialTime: LocalTime) {
        composeTestRule.setContent {
            MedTimerTheme {
                MedTimerTimePickerDialog(
                    initialTime = initialTime,
                    onConfirm = { pickedTime = it },
                    onDismissRequest = { dismissed = true },
                )
            }
        }
    }

    @Test
    fun `confirming the time picker returns the initial time`() {
        showTimePicker(LocalTime.of(14, 35))

        composeTestRule.onNodeWithText("OK").performClick()

        assertEquals(LocalTime.of(14, 35), pickedTime)
    }

    @Test
    fun `cancelling the time picker dismisses without a time`() {
        showTimePicker(LocalTime.of(14, 35))

        composeTestRule.onNodeWithText("Cancel").performClick()

        assertTrue(dismissed)
        assertNull(pickedTime)
    }

    @Test
    fun `a time typed in keyboard mode is returned`() {
        setTimeFormat("24")
        showTimePicker(LocalTime.of(8, 0))

        composeTestRule.onNodeWithContentDescription("Switch to text input mode").performClick()
        composeTestRule.onNode(hasSetTextAction() and hasContentDescription("for hour", substring = true)).performTextReplacement("21")
        composeTestRule.onNode(hasSetTextAction() and hasContentDescription("for minutes", substring = true)).performTextReplacement("45")
        composeTestRule.onNodeWithText("OK").performClick()

        assertEquals(LocalTime.of(21, 45), pickedTime)
    }

    @Test
    fun `the time picker follows the system 12-hour setting`() {
        setTimeFormat("12")
        showTimePicker(LocalTime.of(14, 35))

        composeTestRule.onNodeWithText("PM").assertExists()
    }

    @Test
    fun `the time picker follows the system 24-hour setting`() {
        setTimeFormat("24")
        showTimePicker(LocalTime.of(14, 35))

        composeTestRule.onNodeWithText("PM").assertDoesNotExist()
    }

    @Test
    fun `confirming the date picker returns the initial date`() {
        composeTestRule.setContent {
            MedTimerTheme {
                MedTimerDatePickerDialog(
                    initialDate = LocalDate.of(2026, 3, 29),
                    onConfirm = { pickedDate = it },
                    onDismissRequest = { dismissed = true },
                )
            }
        }

        composeTestRule.onNodeWithText("OK").performClick()

        assertEquals(LocalDate.of(2026, 3, 29), pickedDate)
    }
}
