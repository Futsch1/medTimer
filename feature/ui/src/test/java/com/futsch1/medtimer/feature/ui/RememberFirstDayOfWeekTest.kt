package com.futsch1.medtimer.feature.ui

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RememberFirstDayOfWeekTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    @Config(qualifiers = "en-rUS")
    fun `resolves Sunday for a Sunday-first locale`() {
        var result: DayOfWeek? = null
        composeTestRule.setContent {
            result = rememberFirstDayOfWeek()
        }

        assertEquals(DayOfWeek.SUNDAY, result)
    }

    @Test
    @Config(qualifiers = "de-rDE")
    fun `resolves Monday for a Monday-first locale`() {
        var result: DayOfWeek? = null
        composeTestRule.setContent {
            result = rememberFirstDayOfWeek()
        }

        assertEquals(DayOfWeek.MONDAY, result)
    }
}
