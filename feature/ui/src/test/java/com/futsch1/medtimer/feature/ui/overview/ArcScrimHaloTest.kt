package com.futsch1.medtimer.feature.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [28], qualifiers = "w400dp-h800dp-notnight")
class ArcScrimHaloTest {

    @get:Rule
    val rule = createComposeRule()

    /**
     * The scrim must only ever get lighter as it leaves the anchor. Any stretch that darkens on
     * the way out means a lighter patch sits nearer the anchor than the scrim around it — over a
     * light background that patch reads as a halo ringing the state button.
     */
    @Test
    fun scrimOnlyEverLightensAwayFromTheAnchor() {
        rule.setContent {
            MedTimerTheme(darkTheme = false, dynamicColor = false) {
                Box(
                    modifier = Modifier
                        .testTag(SCRIM)
                        .size(200.dp)
                        .background(Color.White),
                    propagateMinConstraints = true,
                ) {
                    ArcScrim(expanded = true)
                }
            }
        }
        rule.mainClock.advanceTimeBy(2_000)

        val pixels = rule.onNodeWithTag(SCRIM).captureToImage().toPixelMap()
        val center = pixels.width / 2
        val luminanceByRadius = (0 until center).map { radius ->
            val color = pixels[center + radius, center]
            color.red + color.green + color.blue
        }

        val darkeningRadii = luminanceByRadius.zipWithNext()
            .withIndex()
            .filter { (_, pair) -> pair.second < pair.first - TOLERANCE }
            .map { (radius, _) -> radius }

        assertEquals(
            "scrim darkens away from the anchor at radii $darkeningRadii",
            emptyList<Int>(),
            darkeningRadii,
        )
    }

    private companion object {
        const val SCRIM = "scrim"

        /** Ignores single-step dips from 8-bit colour quantisation along the gradient. */
        const val TOLERANCE = 0.02f
    }
}
