package com.futsch1.medtimer

import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND

/**
 * Maps the window to a navigation layout: a side rail once the window is at least Medium width
 * (>= 600dp), a bottom bar below that. This intentionally keys on width only, overriding
 * NavigationSuiteScaffold's default (which falls back to a bottom bar when the height is compact —
 * i.e. a landscape phone — the opposite of what we want here).
 */
fun navSuiteType(windowAdaptiveInfo: WindowAdaptiveInfo): NavigationSuiteType =
    if (windowAdaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)) {
        NavigationSuiteType.NavigationRail
    } else {
        NavigationSuiteType.NavigationBar
    }
