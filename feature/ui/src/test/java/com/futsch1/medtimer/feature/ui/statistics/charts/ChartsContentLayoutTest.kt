package com.futsch1.medtimer.feature.ui.statistics.charts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowSizeClass.Companion.BREAKPOINTS_V1
import androidx.window.core.layout.computeWindowSizeClass
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.feature.ui.statistics.ChartsState
import com.futsch1.medtimer.feature.ui.statistics.MedicineDaySeries
import com.futsch1.medtimer.feature.ui.statistics.MedicinePerDayData
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

/**
 * Verifies the adaptive arrangement of [ChartsContent]: the two pie charts stack vertically on a
 * tablet in landscape and sit side by side otherwise. The bar chart is forced down its synchronous
 * inspection-mode path so the Vico producer's async work can't make the Robolectric render flaky;
 * the pie title texts ("Last 7 days" / "Total") are used as position anchors.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ChartsContentLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    @Config(qualifiers = "land")
    fun `tablet landscape stacks the two pie charts vertically`() {
        setCharts(width = 800.dp, height = 400.dp, windowSizeClass = BREAKPOINTS_V1.computeWindowSizeClass(widthDp = 800f, heightDp = 400f))

        val period = composeTestRule.onNodeWithText("Last 7 days").getBoundsInRoot()
        val total = composeTestRule.onNodeWithText("Total").getBoundsInRoot()

        assertTrue(
            total.top > period.top,
            "Expected the Total pie stacked below the period pie; period.top=${period.top}, total.top=${total.top}",
        )
    }

    @Test
    fun `phone portrait places the two pie charts side by side`() {
        setCharts(width = 400.dp, height = 800.dp, windowSizeClass = BREAKPOINTS_V1.computeWindowSizeClass(widthDp = 400f, heightDp = 800f))

        val period = composeTestRule.onNodeWithText("Last 7 days").getBoundsInRoot()
        val total = composeTestRule.onNodeWithText("Total").getBoundsInRoot()

        assertTrue(
            total.left > period.left,
            "Expected the Total pie to the right of the period pie; period.left=${period.left}, total.left=${total.left}",
        )
    }

    @Test
    @Config(qualifiers = "port")
    fun `wide tablet in portrait places the two pie charts side by side`() {
        // Width passes the medium breakpoint, so only the orientation guard keeps the layout side-by-side.
        setCharts(width = 800.dp, height = 1200.dp, windowSizeClass = BREAKPOINTS_V1.computeWindowSizeClass(widthDp = 800f, heightDp = 1200f))

        val period = composeTestRule.onNodeWithText("Last 7 days").getBoundsInRoot()
        val total = composeTestRule.onNodeWithText("Total").getBoundsInRoot()

        assertTrue(
            total.left > period.left,
            "Wide tablet in portrait should keep the pies side by side; period.left=${period.left}, total.left=${total.left}",
        )
    }

    @Test
    fun `a single-category pie still renders (a full-circle slice would otherwise be blank)`() {
        // Only taken, no skipped: each pie is one full-circle slice. Vico draws a 360-degree arc as
        // nothing, so the fix renders a solid "100%" circle instead. Assert that label is present.
        setCharts(
            width = 400.dp,
            height = 800.dp,
            windowSizeClass = BREAKPOINTS_V1.computeWindowSizeClass(widthDp = 400f, heightDp = 800f),
            state = chartsState(takenPeriod = 5, skippedPeriod = 0, takenTotal = 12, skippedTotal = 0),
        )

        assertTrue(
            composeTestRule.onAllNodesWithText("100%").fetchSemanticsNodes().isNotEmpty(),
            "Expected a single-category pie to render a 100% circle, but no \"100%\" label was found",
        )
    }

    private fun setCharts(width: Dp, height: Dp, windowSizeClass: WindowSizeClass, state: ChartsState = chartsState()) {
        composeTestRule.setContent {
            MedTimerTheme {
                // Injecting WindowAdaptiveInfo removes any dependency on the host's window metrics for the
                // width signal (orientation still comes from the reliable `land`/portrait config qualifier).
                // LocalInspectionMode forces the bar chart's synchronous path so the render stays deterministic.
                CompositionLocalProvider(LocalInspectionMode provides true) {
                    Box(modifier = Modifier.size(width, height)) {
                        ChartsContent(
                            state = state,
                            windowAdaptiveInfo = WindowAdaptiveInfo(
                                windowSizeClass = windowSizeClass,
                                windowPosture = Posture(),
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun chartsState(
        takenPeriod: Long = 7,
        skippedPeriod: Long = 3,
        takenTotal: Long = 42,
        skippedTotal: Long = 8,
    ) = ChartsState(
        perDay = MedicinePerDayData(
            epochDays = listOf(20200L, 20201L, 20202L),
            series = listOf(
                MedicineDaySeries("Vitamin X 500 mg", listOf(1, 2, 1)),
                MedicineDaySeries("Medicine A", listOf(0, 1, 2)),
            ),
        ),
        dayLabels = persistentListOf("May 26", "May 27", "May 28"),
        seriesColors = persistentListOf(0xFF003F5C.toInt(), 0xFFFF7C43.toInt()),
        takenPeriod = takenPeriod,
        skippedPeriod = skippedPeriod,
        takenTotal = takenTotal,
        skippedTotal = skippedTotal,
        days = 7,
    )
}
