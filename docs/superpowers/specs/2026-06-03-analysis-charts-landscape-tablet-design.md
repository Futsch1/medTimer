# Analysis Charts — landscape tablet layout

**Date:** 2026-06-03
**Branch:** `feature/1234-analysis-screen-jetpack-compose`
**Status:** Approved design — ready for implementation plan

## Goal

On a tablet in landscape, rearrange the **Charts** view of the Analysis screen so
the per-day bar chart sits on the left and the two taken/skipped pie charts are
**stacked vertically in a column on the right**. Phones and portrait keep the
current top-to-bottom layout. This is the first adaptive-layout step for the
Analysis screen; the detection logic is inlined (not extracted to a shared
helper) so future per-view adaptive tweaks stay easy.

## Scope

- **Changes:** `feature/ui/.../statistics/charts/ChartsContent.kt`,
  `gradle/libs.versions.toml`, `feature/ui/build.gradle.kts`, plus a test and an
  extra preview.
- **Unchanged:** `StatisticsScreen` (header chips + range dropdown +
  `AnimatedContent` tab container), the Table view, the Calendar view, all
  ViewModel/state/data code, the Vico chart configuration, and the existing
  `material3` Expressive version pin.

## Decisions (from brainstorming)

1. **Dependency:** add only `androidx.compose.material3.adaptive:adaptive` at the
   latest **stable** version, **1.2.0** (released 2025-10-22). Do **not** pin
   `androidx.window` — it resolves transitively (1.4.x, which carries the
   `WindowSizeClass` breakpoint API). Confirm the transitive resolution at build
   time; if `isWidthAtLeastBreakpoint` / `WIDTH_DP_MEDIUM_LOWER_BOUND` fail to
   resolve, **stop and ask** before adding `window-core` explicitly.
2. **Split ratio:** bar chart `weight(2f)` : pie column `weight(1f)` in landscape,
   mirroring the current portrait 2:1 vertical weighting.
3. **Detection inlined** in `ChartsContent`, not a reusable `isTabletLandscape()`.

## Design

### 1. Dependency wiring

`gradle/libs.versions.toml`:

```toml
# [versions]
material3-adaptive = "1.2.0"

# [libraries]
androidx-compose-material3-adaptive = { module = "androidx.compose.material3.adaptive:adaptive", version.ref = "material3-adaptive" }
```

Add `implementation(libs.androidx.compose.material3.adaptive)` to
`feature/ui/build.gradle.kts`. Gate verification: `./gradlew assembleFullDebug`
and `assembleFossDebug` both succeed and the breakpoint symbols resolve.

### 2. Inlined adaptive detection

At the top of `ChartsContent`:

```kotlin
val configuration = LocalConfiguration.current
val isTabletLandscape =
    currentWindowAdaptiveInfo().windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND) &&
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
```

Imports: `android.content.res.Configuration`,
`androidx.compose.ui.platform.LocalConfiguration`,
`androidx.compose.material3.adaptive.currentWindowAdaptiveInfo`,
`androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND`.

### 3. Layout branch

`ChartsContent` keeps the `ProvideVicoTheme(rememberM3VicoTheme())` wrapper and
branches inside it:

- **Portrait / compact (unchanged):**
  `Column(spacedBy 8.dp) { MedicinePerDayBarChart(Modifier.fillMaxWidth().weight(2f)); TwoPies(state, stacked = false, Modifier.weight(1f)) }`
- **Tablet landscape (new):**
  `Row(spacedBy 8.dp) { MedicinePerDayBarChart(Modifier.fillMaxHeight().weight(2f)); TwoPies(state, stacked = true, Modifier.weight(1f)) }`

```
Portrait (today)              Tablet landscape (new)
+----------------+            +--------------+------+
|   Bar chart    | 2          |              | Pie  | 1
+----------------+            |  Bar chart   +------+
| Pie  |  Pie    | 1          |    (2)       | Pie  | 1
+----------------+            +--------------+------+
```

### 4. DRY the pie pair

Factor the two `TakenSkippedPieChart` call-sites into one private composable so
neither branch duplicates them:

```kotlin
@Composable
private fun TwoPies(state: ChartsState, stacked: Boolean, modifier: Modifier = Modifier) {
    val periodTitle = pluralStringResource(R.plurals.last_n_days, state.days, state.days)
    val totalTitle = stringResource(R.string.total)
    if (stacked) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TakenSkippedPieChart(periodTitle, state.takenPeriod, state.skippedPeriod, Modifier.weight(1f))
            TakenSkippedPieChart(totalTitle, state.takenTotal, state.skippedTotal, Modifier.weight(1f))
        }
    } else {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TakenSkippedPieChart(periodTitle, state.takenPeriod, state.skippedPeriod, Modifier.weight(1f))
            TakenSkippedPieChart(totalTitle, state.takenTotal, state.skippedTotal, Modifier.weight(1f))
        }
    }
}
```

`TakenSkippedPieChart`, `EmptyPieCircle`, `LegendDot`, and
`MedicinePerDayBarChart` are unchanged.

### 5. Tests & previews

- **Test:** a Robolectric Compose test under a landscape large-width qualifier
  (`@Config(qualifiers = "w800dp-land")`) that renders `ChartsContent` with
  synthetic data ("Vitamin X 500 mg", "Medicine A") and asserts the bar chart and
  both pie charts (period + total) are present. Semantics-first, following the
  existing Compose test conventions. Keep a compact-width counterpart if it adds
  signal without duplicating the portrait coverage already in place.
- **Preview:** add a second `@Preview` to `ChartsContent` with a wide `widthDp`
  (and landscape orientation) so the new arrangement is reviewable in the IDE.

## Verification

- `./gradlew assembleFullDebug assembleFossDebug` — both flavors build; adaptive
  + transitive window symbols resolve.
- `./gradlew lint` — clean (no new suppressions).
- `./gradlew testFullDebugUnitTest` (or the targeted module test task) — the new
  landscape test passes.

## Non-goals

- No data/ViewModel/state changes.
- No landscape handling for the Table or Calendar views.
- No manifest or `android:configChanges` changes.
- No change to the `material3` Expressive `1.5.0-alpha20` pin.
- No extraction of a shared `isTabletLandscape()` helper (intentionally inlined).
