package com.futsch1.medtimer.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pins the mask alphas to the look they were fitted against: masking surfaceContainer with the
 * content colour must land on surfaceContainerHighest, which is what the icon plate used to be.
 * Only the static schemes are checked — dynamic colour is device-supplied and not reproducible here.
 */
class ContainerColorsTest {

    @Test
    fun maskOverSurfaceContainerMatchesSurfaceContainerHighestInLightScheme() {
        assertMaskReproducesSurfaceContainerHighest(LightColorScheme)
    }

    @Test
    fun maskOverSurfaceContainerMatchesSurfaceContainerHighestInDarkScheme() {
        assertMaskReproducesSurfaceContainerHighest(DarkColorScheme)
    }

    private fun assertMaskReproducesSurfaceContainerHighest(scheme: ColorScheme) {
        val masked = containerMask(scheme.onSurface).compositeOver(scheme.surfaceContainer)
        val deltas = listOf(
            channelDelta(masked.red, scheme.surfaceContainerHighest.red),
            channelDelta(masked.green, scheme.surfaceContainerHighest.green),
            channelDelta(masked.blue, scheme.surfaceContainerHighest.blue),
        )
        assertTrue(
            "masked $masked differs from ${scheme.surfaceContainerHighest} by $deltas /255",
            deltas.all { it <= MAX_CHANNEL_DELTA }
        )
    }

    private fun channelDelta(actual: Float, expected: Float) =
        abs((actual * 255).roundToInt() - (expected * 255).roundToInt())

    @Test
    fun maskLightensDarkContainersAndDarkensLightOnes() {
        val onDark = containerMask(Color.White).compositeOver(Color(0xFF102030))
        val onLight = containerMask(Color.Black).compositeOver(Color(0xFFF0E8E0))
        assertTrue(onDark.red > 0x10 / 255f)
        assertTrue(onLight.red < 0xF0 / 255f)
    }
}

private const val MAX_CHANNEL_DELTA = 2
