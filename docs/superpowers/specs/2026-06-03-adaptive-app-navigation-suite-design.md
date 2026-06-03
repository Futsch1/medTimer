# Adaptive app navigation (Compose NavigationSuiteScaffold)

**Date:** 2026-06-03
**Branch:** `feature/1234-analysis-screen-jetpack-compose`
**Status:** Draft design — awaiting user review

## Goal

Replace the app's bottom `BottomNavigationView` with a Compose
`NavigationSuiteScaffold` so the top-level navigation (Overview / Medicines /
Analysis) renders as a **bottom bar in portrait/compact** and a **side
navigation rail in landscape/wide (≥ Medium width, 600dp)**. The rail reclaims
the vertical height the bottom bar costs, fixing the cramped Analysis screen in
landscape phone.

## Problem

`MainActivity` is a Views `AppCompatActivity`. `app/src/main/res/layout/activity_main.xml`
is a `ConstraintLayout` with a top `Toolbar` (the support action bar), two warning
`CardView`s, a `FragmentContainerView` (`navHost`, the Navigation `NavHostFragment`),
and a bottom `BottomNavigationView` (`@menu/bottom_nav`). In landscape phone the
top `Toolbar` and the bottom bar both consume vertical space, squeezing the
destination content (most visibly the Analysis screen).

The `app` module currently has **no Compose** (`buildFeatures { buildConfig = true }`
only; no compose/viewBinding; no Compose deps; no compose-compiler plugin).

## Scope

- **Build:** enable Compose + viewBinding in `app`; add Compose deps + the
  navigation-suite dependency.
- **Layout:** split `activity_main.xml` → `content_main.xml` (everything except
  the bottom bar).
- **Code:** rewrite `MainActivity.start()`/`setupNavigation()` to host a
  `NavigationSuiteScaffold` and drive the existing `NavController`.
- **Test:** a unit test for the breakpoint→`NavigationSuiteType` mapping.

**Unchanged:** the navigation graph, the destination Fragments, the Toolbar /
action-bar behavior, the two warnings, biometrics/intro/edge-to-edge bootstrap,
and all already-shipped Analysis Compose work.

## Decisions

1. **Embed Views, don't reimplement.** Keep the `Toolbar`, the two warning
   `CardView`s, and the Fragment `NavHostFragment` as Views inside an
   `AndroidViewBinding`. Only the `BottomNavigationView` is replaced. This
   preserves `setSupportActionBar` + `setupActionBarWithNavController` +
   `onSupportNavigateUp`, the warning visibility toggles, and the bottom-nav
   custom listeners without rebuilding them in Compose.
2. **Breakpoint = Medium (≥ 600dp) → rail; below → bar.** Explicitly override
   `NavigationSuiteScaffold`'s default, which returns a **bottom bar** when the
   window **height** is compact (i.e. landscape phone) — the opposite of the
   goal. Detection uses `currentWindowAdaptiveInfo().windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)`.
3. **New dependency:** `androidx.compose.material3:material3-adaptive-navigation-suite`
   pinned to the hand-pinned `material3-expressive` version (`1.5.0-alpha20`),
   matching `:feature:ui`'s `material3`. Compose `material3` in `app` is pinned
   the same way so `MaterialExpressiveTheme`/`MedTimerTheme` resolves identically.

## Design

### 1. Build wiring (`app/build.gradle.kts`, `gradle/libs.versions.toml`)

- `app/build.gradle.kts`:
  - Apply the compose-compiler plugin (`alias(libs.plugins.kotlin.compose)`).
  - `buildFeatures { buildConfig = true; compose = true; viewBinding = true }`.
  - Add deps: `platform(libs.androidx.compose.bom)`, `libs.androidx.compose.material3`
    (the `1.5.0-alpha20` pin), `libs.androidx.activity.compose`,
    `libs.androidx.compose.material3.adaptive`, and the new
    `libs.androidx.compose.material3.adaptive.navigation.suite`. Debug tooling
    (`ui-tooling`/`ui-tooling-preview`) as the project already does elsewhere.
- `gradle/libs.versions.toml`: add
  `androidx-compose-material3-adaptive-navigation-suite = { module = "androidx.compose.material3:material3-adaptive-navigation-suite", version.ref = "material3-expressive" }`.
  Verify `activity-compose` and the compose-bom aliases already exist (they back
  `:feature:ui`); reuse them. Confirm the new module resolves at build time; if
  it does not exist at `1.5.0-alpha20`, **stop and ask** before changing the pin.

### 2. Layout split

- Create `app/src/main/res/layout/content_main.xml`: a copy of `activity_main.xml`
  **without** the `BottomNavigationView`. The `navHost` `FragmentContainerView`
  constraints change so its bottom is `app:layout_constraintBottom_toBottomOf="parent"`
  (it no longer sits above the bottom bar). All IDs stay identical: `toolbar`,
  `batteryOptimizationWarning`, `dismissBatteryWarning`, `exactRemindersWarning`,
  `dismissExactReminderWarning`, `navHost`. viewBinding generates `ContentMainBinding`.
- `activity_main.xml` is removed (its role is taken by `content_main.xml` +
  the Compose scaffold). The `bottom_nav.xml` menu stays — it is the source of
  truth for the three items (id/icon/title/contentDescription), read in code.

### 3. `MainActivity` rewrite

`start()` replaces `setContentView(R.layout.activity_main)` with:

```kotlin
setContent {
    MedTimerTheme {
        AppNavigationScaffold(
            onNavHostReady = { navHostFragment -> bindNavHost(navHostFragment) },
        )
    }
}
```

`AppNavigationScaffold` (a private `@Composable` in the app module):

- Holds `var navController by remember { mutableStateOf<NavController?>(null) }`
  and `var currentDestinationId by remember { mutableIntStateOf(0) }`.
- Computes `val type = navSuiteType(currentWindowAdaptiveInfo())`.
- Renders:
  ```kotlin
  NavigationSuiteScaffold(
      navigationSuiteType = type,
      navigationSuiteItems = {
          NAV_ITEMS.forEach { item ->
              item(
                  selected = currentDestinationId == item.destinationId,
                  onClick = { navController?.let { onNavItemClick(it, item.destinationId) } },
                  icon = { Icon(painterResource(item.iconRes), stringResource(item.labelRes)) },
                  label = { Text(stringResource(item.labelRes)) },
              )
          }
      },
  ) {
      AndroidViewBinding(ContentMainBinding::inflate) {
          val navHostFragment = navHost.getFragment<NavHostFragment>()
          val controller = navHostFragment.navController
          navController = controller
          controller.addOnDestinationChangedListener { _, destination, _ ->
              currentDestinationId = topLevelIdFor(destination)
          }
          onNavHostReady(navHostFragment)   // activity wires action bar + warnings
      }
  }
  ```
- `NAV_ITEMS`: a small list of `{ destinationId, iconRes, labelRes }` mirroring
  `@menu/bottom_nav` (overview/medicines/analysis), referencing the existing
  `feature.ui.R.id.*Fragment` ids and the existing drawables/strings.

`navSuiteType(info: WindowAdaptiveInfo): NavigationSuiteType` — pure, top-level,
**unit-testable**:

```kotlin
fun navSuiteType(info: WindowAdaptiveInfo): NavigationSuiteType =
    if (info.windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)) {
        NavigationSuiteType.NavigationRail
    } else {
        NavigationSuiteType.NavigationBar
    }
```

`onNavItemClick(controller, destId)` reproduces today's bottom-nav select
behavior: `controller.popBackStack(preferencesFragment, true)` then a
NavigationUI-equivalent navigate (launchSingleTop, restoreState, popUpTo the
graph start saving state). Reselect (clicking the already-selected item) keeps
today's behavior: `popBackStack(destId, false)` and forward to the current
fragment if it is an `OnFragmentReselectedListener`. (If item-level reselect
detection in the scaffold is awkward, reselect == onClick while already selected.)

`bindNavHost(navHostFragment)` (activity method, called from the bind block)
does what `setupNavigation()` + `start()` do today with the Views: capture the
`NavController`, `setSupportActionBar(binding.toolbar)` (via a stored binding
reference), `setupActionBarWithNavController`, and wire the warning dismiss
buttons + store `batteryOptimizationWarning`/`exactReminderWarning` refs for the
`onResume` visibility checks. `onSupportNavigateUp()` keeps using
`findNavController(R.id.navHost)`, which still resolves because the id is present
in the embedded binding.

### 4. Insets / edge-to-edge (risk)

The activity already calls `enableEdgeToEdge()`. `NavigationSuiteScaffold`
applies window-inset padding to the nav container; the embedded content
(`content_main.xml`) must still apply the **top** system-bar inset to the
`Toolbar` (today the `ConstraintLayout` root uses `android:fitsSystemWindows="true"`).
Verify on device that: status-bar/toolbar spacing is correct, the rail does not
overlap the gesture/nav bars in landscape, and the bottom bar sits above the
navigation bar in portrait. Adjust `fitsSystemWindows` / inset handling on the
embedded root if needed. This is the highest-risk item.

## Testing

- **Unit test** (app module, JVM/Robolectric): `navSuiteType` returns
  `NavigationRail` for a Medium/Expanded width class and `NavigationBar` for
  Compact, using `BREAKPOINTS_V1.computeWindowSizeClass(...)` to build the input —
  mirrors the pattern in `:feature:ui`'s `ChartsContentLayoutTest`.
- **Build:** `./gradlew assembleFullDebug assembleFossDebug` (both flavors build
  with Compose newly enabled in `app`), `./gradlew lint` clean.
- **Manual (device/emulator):** rotate each top-level destination; confirm
  bottom bar ↔ side rail switch at the Medium width boundary; selection follows
  navigation; up-navigation and the action-bar title still work; the two
  warnings still appear/dismiss; the Analysis screen gets the reclaimed height
  in landscape. Run the existing instrumented nav tests if present.

## Non-goals

- Not migrating the destination Fragments or the nav graph to Navigation-Compose.
- Not replacing the `Toolbar`/action bar with a Compose `TopAppBar`.
- Not changing the in-screen Analysis view selector (Charts/Table/Calendar chips)
  or the per-view landscape layouts already shipped.
- No change to the three nav destinations, their icons, or their order.
- No new `android:configChanges` / manifest navigation changes.

## Open questions / confirmations needed

1. **Approve the build changes:** enabling Compose + viewBinding in `app` and
   adding `material3-adaptive-navigation-suite` (+ the Compose deps) — per
   AGENTS.md "ask first" for dependencies.
2. **Rail header (optional):** `NavigationSuiteScaffold` rails can show an
   optional header (e.g. a FAB/logo). Default = none. Confirm none for now.
