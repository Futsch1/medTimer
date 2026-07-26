package com.futsch1.medtimer

import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.fragment.NavHostFragment
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import com.futsch1.medtimer.databinding.ContentMainBinding
import com.futsch1.medtimer.databinding.ToolbarBinding
import com.futsch1.medtimer.core.ui.R as CoreUiR
import com.futsch1.medtimer.feature.ui.R as FeatureUiR

/** Stable testTags for the top-level nav items; exposed to instrumented tests as resource-ids. */
object NavTestTags {
    const val OVERVIEW = "nav_overview"
    const val MEDICINES = "nav_medicines"
    const val STATISTICS = "nav_statistics"
}

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

/**
 * Whether the shell content has to pad the bottom system-bar inset itself. The NavigationBar consumes
 * it; the rail sits on the side and leaves the gesture bar to the content.
 */
fun consumesBottomInset(navigationSuiteType: NavigationSuiteType): Boolean =
    navigationSuiteType == NavigationSuiteType.NavigationRail

private data class TopLevelNavItem(
    val destinationId: Int,
    val iconRes: Int,
    val labelRes: Int,
    val descriptionRes: Int,
    val testTag: String,
)

// Tab order for the bar and rail. Destination ids come from the nav graph in :feature:ui.
private val NAV_ITEMS = listOf(
    TopLevelNavItem(FeatureUiR.id.overviewFragment, CoreUiR.drawable.calendar_event, CoreUiR.string.tab_overview, CoreUiR.string.overview_tab_description, NavTestTags.OVERVIEW),
    TopLevelNavItem(FeatureUiR.id.medicinesFragment, CoreUiR.drawable.capsule, CoreUiR.string.tab_medicine, CoreUiR.string.medicines_tab_description, NavTestTags.MEDICINES),
    TopLevelNavItem(FeatureUiR.id.statisticsFragment, CoreUiR.drawable.bar_chart, CoreUiR.string.analysis, CoreUiR.string.statistics_tab_description, NavTestTags.STATISTICS),
)

private val TOP_LEVEL_IDS = NAV_ITEMS.map { it.destinationId }.toSet()

/** Resolves any destination up its hierarchy to one of the three top-level ids (0 if none). */
private fun topLevelDestinationId(destination: NavDestination): Int =
    destination.hierarchy.firstOrNull { it.id in TOP_LEVEL_IDS }?.id ?: 0

/**
 * Top-level navigation as an adaptive bar/rail. The Toolbar and the Fragment NavHost stay as Views,
 * embedded either side of the Compose [Warnings]; [onContentBound] hands them back to the activity
 * for wiring. Selection follows the NavController's current destination.
 */
@Composable
fun AppNavigationScaffold(
    state: MainScreenState,
    onDismissBatteryWarning: () -> Unit,
    onDismissExactRemindersWarning: () -> Unit,
    onContentBound: (Toolbar, NavHostFragment) -> Unit,
    onNavItemClick: (NavController, Int) -> Unit,
) {
    var navController by remember { mutableStateOf<NavController?>(null) }
    var currentDestinationId by remember { mutableIntStateOf(0) }
    var toolbar by remember { mutableStateOf<Toolbar?>(null) }
    var navHostFragment by remember { mutableStateOf<NavHostFragment?>(null) }

    // Wire once both nodes exist rather than relying on Column's top-down order: setSupportActionBar
    // must precede setupActionBarWithNavController, and getting that backwards fails silently.
    LaunchedEffect(toolbar, navHostFragment) {
        val boundToolbar = toolbar ?: return@LaunchedEffect
        val boundNavHost = navHostFragment ?: return@LaunchedEffect
        onContentBound(boundToolbar, boundNavHost)
    }

    val navigationSuiteType = navSuiteType(currentWindowAdaptiveInfo())
    val insetSides = if (consumesBottomInset(navigationSuiteType)) {
        WindowInsetsSides.Top + WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
    } else {
        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            NAV_ITEMS.forEach { navItem ->
                item(
                    selected = currentDestinationId == navItem.destinationId,
                    onClick = { navController?.let { onNavItemClick(it, navItem.destinationId) } },
                    icon = { Icon(painterResource(navItem.iconRes), contentDescription = stringResource(navItem.descriptionRes)) },
                    label = { Text(stringResource(navItem.labelRes)) },
                    modifier = Modifier.testTag(navItem.testTag),
                )
            }
        },
        layoutType = navigationSuiteType,
        modifier = Modifier.semantics { testTagsAsResourceId = true },
    ) {
        Column(Modifier.windowInsetsPadding(WindowInsets.systemBars.only(insetSides))) {
            AndroidViewBinding(ToolbarBinding::inflate) {
                toolbar = root
            }
            Warnings(
                state = state,
                onDismissBatteryWarning = onDismissBatteryWarning,
                onDismissExactRemindersWarning = onDismissExactRemindersWarning,
            )
            AndroidViewBinding(ContentMainBinding::inflate, Modifier.weight(1f)) {
                // The update block runs on every recomposition; set up exactly once.
                if (navController == null) {
                    val fragment = root.getFragment<NavHostFragment>()
                    val controller = fragment.navController
                    controller.addOnDestinationChangedListener { _, destination, _ ->
                        currentDestinationId = topLevelDestinationId(destination)
                    }
                    navController = controller
                    navHostFragment = fragment
                }
            }
        }
    }
}
