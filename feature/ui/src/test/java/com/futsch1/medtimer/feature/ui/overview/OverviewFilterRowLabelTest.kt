package com.futsch1.medtimer.feature.ui.overview

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.futsch1.medtimer.core.domain.model.OverviewFilter
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.futsch1.medtimer.core.ui.R as CoreUiR

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class OverviewFilterRowLabelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val filterLabel: String =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(CoreUiR.string.filter)

    @Test
    fun `should show the filter label above the toggles`() {
        renderRow {}

        composeTestRule.onNodeWithText(filterLabel).assertIsDisplayed()
        val label = composeTestRule.onNodeWithText(filterLabel).getBoundsInRoot()
        val toggle = composeTestRule.onNodeWithTag(OverviewTestTags.filter(OverviewFilter.TAKEN)).getBoundsInRoot()
        assertTrue(label.top < toggle.top, "Expected the label above the toggles")
    }

    @Test
    fun `should draw safely when bounds are narrower than the outline stroke`() {
        composeTestRule.setContent {
            MedTimerTheme {
                CompositionLocalProvider(LocalDensity provides Density(2f)) {
                    OverviewFilterRow(
                        activeFilters = emptySet(),
                        onToggleFilter = {},
                        modifier = Modifier.width(0.5.dp).height(100.dp),
                    )
                }
            }
        }

        composeTestRule.onNode(isRoot()).captureToImage()
    }

    @Test
    fun `should toggle each filter when its button is clicked`() {
        val toggled = mutableListOf<OverviewFilter>()
        renderRow { toggled += it }

        OverviewFilter.entries.forEach { composeTestRule.onNodeWithTag(OverviewTestTags.filter(it)).performClick() }

        assertEquals(OverviewFilter.entries.toList(), toggled)
    }

    private fun renderRow(onToggleFilter: (OverviewFilter) -> Unit) {
        composeTestRule.setContent {
            MedTimerTheme {
                OverviewFilterRow(activeFilters = emptySet(), onToggleFilter = onToggleFilter)
            }
        }
    }
}
