package com.futsch1.medtimer

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.fragment.NavHostFragment
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import com.futsch1.medtimer.databinding.ContentMainBinding
import com.futsch1.medtimer.core.ui.R as CoreUiR
import com.futsch1.medtimer.feature.ui.R as FeatureUiR

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

private data class TopLevelNavItem(
    val destinationId: Int,
    val iconRes: Int,
    val labelRes: Int,
    val descriptionRes: Int,
)

// Tab order for the bar and rail. Destination ids come from the nav graph in :feature:ui.
private val NAV_ITEMS = listOf(
    TopLevelNavItem(FeatureUiR.id.overviewFragment, CoreUiR.drawable.calendar_event, CoreUiR.string.tab_overview, CoreUiR.string.overview_tab_description),
    TopLevelNavItem(FeatureUiR.id.medicinesFragment, CoreUiR.drawable.capsule, CoreUiR.string.tab_medicine, CoreUiR.string.medicines_tab_description),
    TopLevelNavItem(FeatureUiR.id.statisticsFragment, CoreUiR.drawable.bar_chart, CoreUiR.string.analysis, CoreUiR.string.statistics_tab_description),
)

private val TOP_LEVEL_IDS = NAV_ITEMS.map { it.destinationId }.toSet()

/** Resolves any destination up its hierarchy to one of the three top-level ids (0 if none). */
private fun topLevelDestinationId(destination: NavDestination): Int =
    destination.hierarchy.firstOrNull { it.id in TOP_LEVEL_IDS }?.id ?: 0

/**
 * Top-level navigation as an adaptive bar/rail. The Toolbar, warnings and the Fragment NavHost stay as
 * Views, embedded via AndroidViewBinding; [onContentBound] hands them back to the activity for wiring
 * (support action bar, warning buttons). Selection follows the NavController's current destination.
 */
@Composable
fun AppNavigationScaffold(
    onContentBound: (ContentMainBinding, NavHostFragment) -> Unit,
    onNavItemClick: (NavController, Int) -> Unit,
) {
    var navController by remember { mutableStateOf<NavController?>(null) }
    var currentDestinationId by remember { mutableIntStateOf(0) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            NAV_ITEMS.forEach { navItem ->
                item(
                    selected = currentDestinationId == navItem.destinationId,
                    onClick = { navController?.let { onNavItemClick(it, navItem.destinationId) } },
                    icon = { Icon(painterResource(navItem.iconRes), contentDescription = stringResource(navItem.descriptionRes)) },
                    label = { Text(stringResource(navItem.labelRes)) },
                )
            }
        },
        layoutType = navSuiteType(currentWindowAdaptiveInfo()),
    ) {
        AndroidViewBinding(ContentMainBinding::inflate) {
            // The update block runs on every recomposition; set up exactly once.
            if (navController == null) {
                val navHostFragment = navHost.getFragment<NavHostFragment>()
                val controller = navHostFragment.navController
                controller.addOnDestinationChangedListener { _, destination, _ ->
                    currentDestinationId = topLevelDestinationId(destination)
                }
                navController = controller
                onContentBound(this, navHostFragment)
            }
        }
    }
}
