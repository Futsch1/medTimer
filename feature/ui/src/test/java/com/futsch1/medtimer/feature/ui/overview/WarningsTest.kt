package com.futsch1.medtimer.feature.ui.overview

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class WarningsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var enableClicks = 0
    private var dismissClicks = 0

    private fun showExactRemindersWarning() {
        composeTestRule.setContent {
            MedTimerTheme {
                Warnings(
                    state = MutableOverviewWarnings().apply { showExactRemindersWarning = true },
                    onDismissBatteryWarning = {},
                    onDismissExactRemindersWarning = { dismissClicks++ },
                    onEnableExactReminders = { enableClicks++ },
                )
            }
        }
    }

    @Test
    fun `should enable exact reminders when Enable is tapped on the warning`() {
        showExactRemindersWarning()

        composeTestRule.onNodeWithText("Enable").performClick()

        assertEquals(1, enableClicks)
        assertEquals(0, dismissClicks)
    }

    @Test
    fun `should only dismiss the warning when OK is tapped`() {
        showExactRemindersWarning()

        composeTestRule.onNodeWithText("OK").performClick()

        assertEquals(0, enableClicks)
        assertEquals(1, dismissClicks)
    }
}
