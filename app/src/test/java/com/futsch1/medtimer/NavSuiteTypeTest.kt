package com.futsch1.medtimer

import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.window.core.layout.WindowSizeClass.Companion.BREAKPOINTS_V1
import androidx.window.core.layout.computeWindowSizeClass
import org.junit.Assert.assertEquals
import org.junit.Test

class NavSuiteTypeTest {

    private fun infoFor(widthDp: Float, heightDp: Float) =
        WindowAdaptiveInfo(BREAKPOINTS_V1.computeWindowSizeClass(widthDp = widthDp, heightDp = heightDp), Posture())

    @Test
    fun `compact width uses the navigation bar`() {
        assertEquals(NavigationSuiteType.NavigationBar, navSuiteType(infoFor(420f, 900f)))
    }

    @Test
    fun `medium width uses the navigation rail`() {
        // A landscape phone (short height) at >= 600dp width must still get the rail.
        assertEquals(NavigationSuiteType.NavigationRail, navSuiteType(infoFor(720f, 380f)))
    }

    @Test
    fun `expanded width uses the navigation rail`() {
        assertEquals(NavigationSuiteType.NavigationRail, navSuiteType(infoFor(1000f, 800f)))
    }

    @Test
    fun `599dp width is still compact`() {
        assertEquals(NavigationSuiteType.NavigationBar, navSuiteType(infoFor(599f, 900f)))
    }

    @Test
    fun `600dp width crosses into medium`() {
        assertEquals(NavigationSuiteType.NavigationRail, navSuiteType(infoFor(600f, 900f)))
    }
}
