package com.futsch1.medtimer.feature.ui.overview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.futsch1.medtimer.core.ui.list.SelectionListController
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class OverviewEventListSwipeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `swiping left on the overview content selects the next day when the event list is empty`() {
        var selectedDay = LocalDate.of(2026, 5, 28)
        composeTestRule.setContent {
            MedTimerTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .testTag("overview_content")
                        .overviewDaySwipe { selectedDay = selectedDay.plusDays(it.toLong()) },
                ) {
                    OverviewEventList(
                        events = persistentListOf(),
                        selection = SelectionListController { it.id },
                        onEventClick = {},
                        onEnterSelectionMode = {},
                        onAction = { _, _ -> },
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("overview_content").performTouchInput { swipeLeft() }

        assertEquals(LocalDate.of(2026, 5, 29), selectedDay)
    }

}
