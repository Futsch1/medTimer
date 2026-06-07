# Adaptive App Navigation (NavigationSuiteScaffold) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the app's bottom `BottomNavigationView` with a Compose `NavigationSuiteScaffold` that shows a bottom bar in compact width and a side navigation rail at Medium width (≥600dp), reclaiming the vertical height the bottom bar costs in landscape.

**Architecture:** `MainActivity` becomes a Compose host (`ComposeView` → `MedTimerTheme` → `NavigationSuiteScaffold`). The existing `Toolbar`, the two warning `CardView`s, and the Fragment `NavHostFragment` stay as Views, embedded via `AndroidViewBinding(ContentMainBinding)`. Only the bottom bar is replaced; the support action bar, up-navigation, warnings, and bottom-nav select/reselect behaviors are preserved. A pure `navSuiteType(WindowAdaptiveInfo)` maps the width breakpoint to bar/rail and is unit-tested.

**Tech Stack:** Jetpack Compose (newly enabled in `:app`), Material 3 (`material3` pinned `1.5.0-alpha20`), `material3-adaptive-navigation-suite` (new), `material3.adaptive` (`currentWindowAdaptiveInfo`), `androidx.compose.ui:ui-viewbinding` (`AndroidViewBinding`), Navigation component (Fragments, unchanged).

**Source spec:** `docs/superpowers/specs/2026-06-03-adaptive-app-navigation-suite-design.md`

**Key facts (verified):**
- Top-level destination ids (in `com.futsch1.medtimer.feature.ui.R.id`): `overviewFragment` (graph start), `medicinesFragment`, `statisticsFragment`; plus `preferencesFragment`.
- Nav icons (in `com.futsch1.medtimer.core.ui.R.drawable`): `calendar_event`, `capsule`, `bar_chart`.
- Nav labels / content descriptions (in `com.futsch1.medtimer.core.ui.R.string`): `tab_overview`/`overview_tab_description`, `tab_medicine`/`medicines_tab_description`, `analysis`/`statistics_tab_description`.
- `:feature:ui` is the reference for Compose wiring (`alias(libs.plugins.kotlin.compose)`, `buildFeatures { compose = true }`, `platform(libs.androidx.compose.bom)` + `libs.androidx.compose.material3` etc.).

---

## File Structure

- **Modify** `gradle/libs.versions.toml` — add two library aliases.
- **Modify** `app/build.gradle.kts` — apply compose-compiler plugin; enable `compose` + `viewBinding`; add Compose deps.
- **Create** `app/src/main/res/layout/content_main.xml` — `activity_main.xml` minus the bottom bar.
- **Delete** `app/src/main/res/layout/activity_main.xml` (Task 4).
- **Create** `app/src/main/java/com/futsch1/medtimer/AppNavigation.kt` — `navSuiteType`, `TopLevelNavItem`, `NAV_ITEMS`, `AppNavigationScaffold`.
- **Create** `app/src/test/java/com/futsch1/medtimer/NavSuiteTypeTest.kt` — breakpoint→type unit test.
- **Modify** `app/src/main/java/com/futsch1/medtimer/MainActivity.kt` — host Compose; replace `setupNavigation()`'s bottom-nav wiring.

---

## Task 1: Enable Compose in the `:app` module

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add library aliases**

In `gradle/libs.versions.toml`, under `[libraries]` (near the existing `androidx-compose-*` entries, lines ~126–139), add:

```toml
androidx-compose-material3-adaptive-navigation-suite = { module = "androidx.compose.material3:material3-adaptive-navigation-suite", version.ref = "material3-expressive" }
androidx-compose-ui-viewbinding = { module = "androidx.compose.ui:ui-viewbinding" }
```

(`ui-viewbinding` is versioned by the Compose BOM, like the other `androidx-compose-ui-*` entries. `material3-adaptive-navigation-suite` is pinned to the same `material3-expressive` ref as `androidx-compose-material3` so `MaterialExpressiveTheme` stays consistent.)

- [ ] **Step 2: Apply the compose-compiler plugin**

In `app/build.gradle.kts`, add to the `plugins { }` block (currently lines 1–8):

```kotlin
    alias(libs.plugins.kotlin.compose)
```

- [ ] **Step 3: Enable compose + viewBinding**

In `app/build.gradle.kts`, change the `buildFeatures { }` block (currently lines 64–66):

```kotlin
    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true
    }
```

- [ ] **Step 4: Add Compose dependencies**

In `app/build.gradle.kts`, in the `dependencies { }` block (after the existing `implementation(project(...))` / androidx lines, e.g. after line 126 `implementation(project(":feature:ui"))`), add:

```kotlin
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.ui.viewbinding)
    debugImplementation(libs.androidx.compose.ui.tooling)
```

- [ ] **Step 5: Verify config resolves and both flavors still build**

Run: `./gradlew :app:assembleFullDebug :app:assembleFossDebug`
Expected: `BUILD SUCCESSFUL`. This confirms the compose-compiler plugin applies, the new artifacts resolve (notably `material3-adaptive-navigation-suite:1.5.0-alpha20`), and nothing else broke. If `material3-adaptive-navigation-suite` fails to resolve at `1.5.0-alpha20`, **STOP and report BLOCKED** (do not change the pin without asking).

- [ ] **Step 6: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "#1234 Enable Compose in the app module for adaptive navigation" -m "Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 2: Add `content_main.xml` (layout split, additive)

**Files:**
- Create: `app/src/main/res/layout/content_main.xml`

This is `activity_main.xml` with the `BottomNavigationView` removed and the `navHost`'s bottom constrained to the parent. `activity_main.xml` is left in place for now (deleted in Task 4) so the app keeps building.

- [ ] **Step 1: Create the layout**

Create `app/src/main/res/layout/content_main.xml` with EXACTLY this content:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fitsSystemWindows="true"
    tools:context=".MainActivity">

    <androidx.appcompat.widget.Toolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        android:elevation="4dp"
        android:theme="@style/ThemeOverlay.AppCompat.ActionBar"
        app:layout_constraintTop_toTopOf="parent" />

    <androidx.cardview.widget.CardView
        android:id="@+id/batteryOptimizationWarning"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_margin="8dp"
        android:visibility="gone"
        app:cardBackgroundColor="?attr/colorTertiaryContainer"
        app:cardCornerRadius="8dp"
        app:cardUseCompatPadding="true"
        app:layout_constraintTop_toBottomOf="@id/toolbar">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/battery_optimization_warning_title"
                android:textAppearance="@style/TextAppearance.Material3.TitleMedium"
                android:textColor="?attr/colorOnTertiaryContainer" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:text="@string/battery_optimization_warning_summary"
                android:textColor="?attr/colorOnTertiaryContainer" />

            <Button
                android:id="@+id/dismissBatteryWarning"
                style="@style/Widget.Material3.Button.TextButton"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="end"
                android:text="@string/ok"
                android:textColor="?attr/colorOnTertiaryContainer" />
        </LinearLayout>
    </androidx.cardview.widget.CardView>

    <androidx.cardview.widget.CardView
        android:id="@+id/exactRemindersWarning"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_margin="8dp"
        android:visibility="gone"
        app:cardBackgroundColor="?attr/colorTertiaryContainer"
        app:cardCornerRadius="8dp"
        app:cardUseCompatPadding="true"
        app:layout_constraintTop_toBottomOf="@id/batteryOptimizationWarning">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/exact_reminders_warning_title"
                android:textAppearance="@style/TextAppearance.Material3.TitleMedium"
                android:textColor="?attr/colorOnTertiaryContainer" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:text="@string/exact_reminders_warning_summary"
                android:textColor="?attr/colorOnTertiaryContainer" />

            <Button
                android:id="@+id/dismissExactReminderWarning"
                style="@style/Widget.Material3.Button.TextButton"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="end"
                android:text="@string/ok"
                android:textColor="?attr/colorOnTertiaryContainer" />
        </LinearLayout>
    </androidx.cardview.widget.CardView>

    <androidx.fragment.app.FragmentContainerView
        android:id="@+id/navHost"
        android:name="androidx.navigation.fragment.NavHostFragment"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        app:defaultNavHost="true"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toBottomOf="@id/exactRemindersWarning"
        app:navGraph="@navigation/navigation" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

- [ ] **Step 2: Verify it compiles into both flavors**

Run: `./gradlew :app:assembleFullDebug :app:assembleFossDebug`
Expected: `BUILD SUCCESSFUL` and the `ContentMainBinding` view-binding class is generated.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/content_main.xml
git commit -m "#1234 Add content_main layout (shell without the bottom bar)" -m "Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 3: `navSuiteType` breakpoint mapping (TDD)

**Files:**
- Create: `app/src/test/java/com/futsch1/medtimer/NavSuiteTypeTest.kt`
- Create: `app/src/main/java/com/futsch1/medtimer/AppNavigation.kt` (the `navSuiteType` function only in this task; the composable is added in Task 4)

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/futsch1/medtimer/NavSuiteTypeTest.kt` with EXACTLY:

```kotlin
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
}
```

- [ ] **Step 2: Run the test to verify it fails (red)**

Run: `./gradlew :app:testFullDebugUnitTest --tests "com.futsch1.medtimer.NavSuiteTypeTest"`
Expected red state: COMPILATION failure — `navSuiteType` is unresolved (added in Step 3).

- [ ] **Step 3: Implement `navSuiteType`**

Create `app/src/main/java/com/futsch1/medtimer/AppNavigation.kt` with EXACTLY:

```kotlin
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
```

- [ ] **Step 4: Run the test to verify it passes (green)**

Run: `./gradlew :app:testFullDebugUnitTest --tests "com.futsch1.medtimer.NavSuiteTypeTest"`
Expected: all THREE tests PASS. If the test errors because these classes require the Android framework at runtime, re-run is unnecessary — they are pure Kotlin/Compose; if a `Stub!` `RuntimeException` appears, add `@RunWith(RobolectricTestRunner::class)` + `@Config(sdk = [28])` and report it as DONE_WITH_CONCERNS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/futsch1/medtimer/AppNavigation.kt app/src/test/java/com/futsch1/medtimer/NavSuiteTypeTest.kt
git commit -m "#1234 Add width-based navigation-suite type mapping" -m "Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 4: Host `NavigationSuiteScaffold` in `MainActivity`

**Files:**
- Modify: `app/src/main/java/com/futsch1/medtimer/AppNavigation.kt` (add `TopLevelNavItem`, `NAV_ITEMS`, `AppNavigationScaffold`)
- Modify: `app/src/main/java/com/futsch1/medtimer/MainActivity.kt`
- Delete: `app/src/main/res/layout/activity_main.xml`

- [ ] **Step 1: Add the nav items + the scaffold composable**

Append to `app/src/main/java/com/futsch1/medtimer/AppNavigation.kt` (keep the existing `navSuiteType`; add these imports to the top and the declarations below it):

Add imports:
```kotlin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.fragment.app.getFragment
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.fragment.NavHostFragment
import com.futsch1.medtimer.databinding.ContentMainBinding
import com.futsch1.medtimer.core.ui.R as CoreUiR
import com.futsch1.medtimer.feature.ui.R as FeatureUiR
```

Add declarations:
```kotlin
private data class TopLevelNavItem(
    val destinationId: Int,
    val iconRes: Int,
    val labelRes: Int,
    val descriptionRes: Int,
)

// Mirrors @menu/bottom_nav. Order is the bar/rail order. Ids come from the nav graph in :feature:ui.
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
        navigationSuiteType = navSuiteType(currentWindowAdaptiveInfo()),
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
```

- [ ] **Step 2: Rewrite `MainActivity` to host the scaffold**

In `MainActivity.kt` make these changes:

(a) Remove the import `import com.google.android.material.bottomnavigation.BottomNavigationView`. Add:
```kotlin
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.databinding.ContentMainBinding
```
(Note: `NavHostFragment` is already imported — do not duplicate. `onNavDestinationSelected`/`setupWithNavController` imports become unused; remove `setupWithNavController` and `onNavDestinationSelected` import lines.)

(b) Add a field to hold the bound NavHostFragment alongside the existing fields (e.g. near `appBarConfiguration`):
```kotlin
    private var navHostFragment: NavHostFragment? = null
```

(c) Replace `start()` (currently lines ~151–170) — swap `setContentView(R.layout.activity_main)` + `setupNavigation()` + the `findViewById` warning wiring for the Compose host:
```kotlin
    private suspend fun start() {
        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    MedTimerTheme {
                        AppNavigationScaffold(
                            onContentBound = { binding, navHost -> onContentBound(binding, navHost) },
                            onNavItemClick = { controller, destinationId -> onNavItemClick(controller, destinationId) },
                        )
                    }
                }
            }
        )

        dispatchIntent(this.intent)
        this.intent = Intent()

        checkForceStopped()
    }
```

(d) Replace `setupNavigation()` (currently lines ~186–212) with `onContentBound` + `onNavItemClick`:
```kotlin
    private fun onContentBound(binding: ContentMainBinding, navHostFragment: NavHostFragment) {
        this.navHostFragment = navHostFragment
        val navController = navHostFragment.navController
        setSupportActionBar(binding.toolbar)
        appBarConfiguration = AppBarConfiguration.Builder(
            com.futsch1.medtimer.feature.ui.R.id.overviewFragment,
            com.futsch1.medtimer.feature.ui.R.id.medicinesFragment,
            com.futsch1.medtimer.feature.ui.R.id.statisticsFragment
        ).build()
        setupActionBarWithNavController(this, navController, appBarConfiguration!!)

        batteryOptimizationWarning = binding.batteryOptimizationWarning
        binding.dismissBatteryWarning.setOnClickListener {
            persistentDataDataSource.setBatteryWarningShown(true)
            checkBatteryOptimization()
        }
        exactReminderWarning = binding.exactRemindersWarning
        binding.dismissExactReminderWarning.setOnClickListener {
            persistentDataDataSource.setExactRemindersWarningShown(true)
            checkExactReminders()
        }
    }

    // Mirrors the legacy BottomNavigationView select/reselect behavior.
    private fun onNavItemClick(navController: NavController, destinationId: Int) {
        val isReselected = navController.currentDestination?.let { current ->
            current.id == destinationId || current.parent?.id == destinationId
        } == true
        if (isReselected) {
            navController.popBackStack(destinationId, false)
            val topFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()
            if (topFragment is OnFragmentReselectedListener) {
                topFragment.onFragmentReselected()
            }
        } else {
            navController.popBackStack(com.futsch1.medtimer.feature.ui.R.id.preferencesFragment, true)
            val options = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(navController.graph.startDestinationId, inclusive = false, saveState = true)
                .build()
            navController.navigate(destinationId, null, options)
        }
    }
```

Notes:
- `batteryOptimizationWarning`/`exactReminderWarning` field types are already `CardView?`; `binding.batteryOptimizationWarning`/`binding.exactRemindersWarning` are `CardView` (matching). The `findViewById` ids (`R.id.batteryOptimizationWarning`, etc.) now resolve from `content_main.xml`.
- `onSupportNavigateUp()` (uses `findNavController(R.id.navHost)`) is UNCHANGED — `R.id.navHost` exists in `content_main.xml` and is in the activity's content tree.
- `onResume()` is UNCHANGED — its `checkBatteryOptimization()`/`checkExactReminders()` use the now-`binding`-backed nullable fields (still null-safe, same tolerance as before if composition has not yet bound).

- [ ] **Step 3: Delete the old layout**

```bash
git rm app/src/main/res/layout/activity_main.xml
```

- [ ] **Step 4: Build both flavors + lint**

Run: `./gradlew :app:assembleFullDebug :app:assembleFossDebug :app:lint`
Expected: `BUILD SUCCESSFUL`; no new lint findings; no unused-import warnings (lint is `warningsAsErrors`). Remove any now-unused imports in `MainActivity.kt` (e.g. `setupWithNavController`, `onNavDestinationSelected`, `MenuItem` if no longer referenced) until lint is clean. Do NOT add any `@Suppress`.

- [ ] **Step 5: Re-run the unit test (regression)**

Run: `./gradlew :app:testFullDebugUnitTest --tests "com.futsch1.medtimer.NavSuiteTypeTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/futsch1/medtimer/AppNavigation.kt app/src/main/java/com/futsch1/medtimer/MainActivity.kt
git commit -m "#1234 Replace bottom navigation with adaptive NavigationSuiteScaffold" -m "Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Final Verification

- [ ] `./gradlew :app:testFullDebugUnitTest :app:testFossDebugUnitTest --tests "com.futsch1.medtimer.NavSuiteTypeTest"` — passes on both flavors.
- [ ] `./gradlew assembleFullDebug assembleFossDebug` — both flavors build.
- [ ] `./gradlew lint` — clean, no new suppressions.
- [ ] **Manual (device/emulator), the risk items from the spec:**
  - Portrait phone: bottom nav bar shows the 3 items; selection + navigation work; the action-bar title updates per destination; up-navigation works; battery/exact-reminder warnings still appear and dismiss.
  - Landscape phone (≥600dp wide): nav becomes a **side rail**; the Analysis screen gets the reclaimed vertical height; rotating preserves the selected destination.
  - **Insets:** status bar / `Toolbar` spacing correct; the rail does not overlap system bars in landscape; the bottom bar sits above the system navigation bar in portrait. If wrong, fix inset handling on the `content_main.xml` root (the highest-risk item) — do not paper over with `@Suppress`.
  - Reselecting the current tab pops to its root and forwards `onFragmentReselected()` (e.g. scrolls/refreshes the current fragment).

---

## Self-Review Notes

- **Spec coverage:** build enablement + new dep (Task 1) ✓; layout split keeping IDs (Task 2) ✓; width-based `navSuiteType` overriding the height-compact default, unit-tested (Task 3) ✓; Compose host embedding the Views via `AndroidViewBinding`, preserving action bar / warnings / select+reselect (Task 4) ✓; `activity_main.xml` removed (Task 4) ✓; insets called out as the explicit manual risk ✓; non-goals (no Fragment/graph migration, no Compose TopAppBar, no in-screen Analysis chip change) respected ✓.
- **Type/name consistency:** `navSuiteType`, `AppNavigationScaffold(onContentBound, onNavItemClick)`, `onContentBound`, `onNavItemClick`, `topLevelDestinationId`, `TOP_LEVEL_IDS`, `NAV_ITEMS`, `TopLevelNavItem`, and `ContentMainBinding` are used identically across `AppNavigation.kt` and `MainActivity.kt`. Destination ids use `feature.ui.R.id.*`; icons/labels use `core.ui.R.*`.
- **Known risks, surfaced not hidden:**
  1. **Insets/edge-to-edge** with the embedded Toolbar — explicit manual verification step (Task 4 Step 4 / Final Verification); the most likely place to need iteration.
  2. **`navHost.getFragment<NavHostFragment>()` timing** in the `AndroidViewBinding` update block — the documented interop pattern; setup is guarded to run once. If the fragment is not yet available on first update, report BLOCKED rather than polling.
  3. **`navSuiteType` test runtime** — pure Compose/Kotlin classes; if the JVM stub throws, switch to Robolectric (noted in Task 3 Step 4) and report DONE_WITH_CONCERNS.
- **Consistency with prior work:** the breakpoint test mirrors `:feature:ui`'s `ChartsContentLayoutTest`/`CalendarContentLayoutTest` (`BREAKPOINTS_V1.computeWindowSizeClass`, injected `WindowAdaptiveInfo`), keeping the adaptive code uniform across modules.
