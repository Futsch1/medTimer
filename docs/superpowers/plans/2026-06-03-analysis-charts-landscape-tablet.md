# Analysis Charts — Landscape Tablet Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** On a tablet in landscape, lay out the Analysis **Charts** view as bar-chart-left with the two taken/skipped pie charts stacked vertically on the right; phones and portrait keep today's top-to-bottom layout.

**Architecture:** A single inlined adaptive check in `ChartsContent` selects a `Row` (landscape, bar `weight 2f` : pies `weight 1f`) or the existing `Column` (otherwise). The two pie call-sites are factored into one private `TwoPies(state, stacked, modifier)` composable so neither branch duplicates them, and the bar-chart call is hoisted into a local composable lambda for the same reason. Detection is inlined (no shared helper) to keep future per-view adaptive tweaks local.

**Tech Stack:** Jetpack Compose, Material 3, `androidx.compose.material3.adaptive:adaptive:1.2.0` (`currentWindowAdaptiveInfo`), `androidx.window` `WindowSizeClass` breakpoint API (transitive), Vico charts, Robolectric Compose tests.

**Source spec:** `docs/superpowers/specs/2026-06-03-analysis-charts-landscape-tablet-design.md`

---

## File Structure

- **Modify** `gradle/libs.versions.toml` — add the `material3-adaptive` version + `androidx-compose-material3-adaptive` library alias.
- **Modify** `feature/ui/build.gradle.kts` — add the `implementation(libs.androidx.compose.material3.adaptive)` line.
- **Modify** `feature/ui/src/main/java/com/futsch1/medtimer/feature/ui/statistics/charts/ChartsContent.kt` — inlined detection, `Row`/`Column` branch, new private `TwoPies`, hoisted bar-chart lambda, new landscape `@Preview`.
- **Create** `feature/ui/src/test/java/com/futsch1/medtimer/feature/ui/statistics/charts/ChartsContentLayoutTest.kt` — Robolectric Compose tests proving both arrangements.

---

## Task 1: Add the `material3-adaptive` dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `feature/ui/build.gradle.kts:90` (after the `androidx-compose-material3` line)

- [ ] **Step 1: Add the version to `[versions]`**

In `gradle/libs.versions.toml`, directly below the existing `material3-expressive = "1.5.0-alpha20"` line (currently line 58), add:

```toml
# Adaptive layout APIs (currentWindowAdaptiveInfo). Latest STABLE line; pulls androidx.window 1.4.x
# (the WindowSizeClass breakpoint API) transitively — no separate window pin.
material3-adaptive = "1.2.0"
```

- [ ] **Step 2: Add the library alias to `[libraries]`**

Directly below the existing `androidx-compose-material3 = { ... }` line (currently line 128), add:

```toml
androidx-compose-material3-adaptive = { module = "androidx.compose.material3.adaptive:adaptive", version.ref = "material3-adaptive" }
```

- [ ] **Step 3: Wire it into the module**

In `feature/ui/build.gradle.kts`, immediately after the line `implementation(libs.androidx.compose.material3)` (line 90), add:

```kotlin
    implementation(libs.androidx.compose.material3.adaptive)
```

- [ ] **Step 4: Verify both flavors build and the transitive window symbols resolve**

Run: `./gradlew :feature:ui:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

> **Contingency (do not skip-read):** If a later task's import of `androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND` fails to resolve, the transitive `androidx.window` version is too old. **Stop and ask** before adding an explicit `androidx-window` catalog entry — per `AGENTS.md`, dependency additions are an ask-first gate.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml feature/ui/build.gradle.kts
git commit -m "#1234 Add material3-adaptive dependency for adaptive Charts layout"
```

---

## Task 2: Branch `ChartsContent` on tablet-landscape, factor out `TwoPies`

This task is TDD: the failing test asserts the landscape arrangement (pies stacked), which today's unconditional `Column { bar; Row { pies } }` cannot satisfy.

**Files:**
- Create: `feature/ui/src/test/java/com/futsch1/medtimer/feature/ui/statistics/charts/ChartsContentLayoutTest.kt`
- Modify: `feature/ui/src/main/java/com/futsch1/medtimer/feature/ui/statistics/charts/ChartsContent.kt:73-105` (the `ChartsContent` body)

- [ ] **Step 1: Write the failing test**

Create `feature/ui/src/test/java/com/futsch1/medtimer/feature/ui/statistics/charts/ChartsContentLayoutTest.kt`:

```kotlin
package com.futsch1.medtimer.feature.ui.statistics.charts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    @Config(qualifiers = "w800dp-land")
    fun `tablet landscape stacks the two pie charts vertically`() {
        setCharts(width = 800.dp, height = 400.dp)

        val period = composeTestRule.onNodeWithText("Last 7 days").getBoundsInRoot()
        val total = composeTestRule.onNodeWithText("Total").getBoundsInRoot()

        assertTrue(
            total.top > period.top,
            "Expected the Total pie stacked below the period pie; period.top=${period.top}, total.top=${total.top}",
        )
    }

    @Test
    fun `phone portrait places the two pie charts side by side`() {
        setCharts(width = 400.dp, height = 800.dp)

        val period = composeTestRule.onNodeWithText("Last 7 days").getBoundsInRoot()
        val total = composeTestRule.onNodeWithText("Total").getBoundsInRoot()

        assertTrue(
            total.left > period.left,
            "Expected the Total pie to the right of the period pie; period.left=${period.left}, total.left=${total.left}",
        )
    }

    private fun setCharts(width: Dp, height: Dp) {
        composeTestRule.setContent {
            MedTimerTheme {
                // Inspection mode forces the bar chart's synchronous preview model (no async Vico
                // producer), keeping the Robolectric render deterministic. It does not affect the
                // Row/Column branch under test.
                CompositionLocalProvider(LocalInspectionMode provides true) {
                    Box(modifier = Modifier.size(width, height)) {
                        ChartsContent(state = chartsState())
                    }
                }
            }
        }
    }

    private fun chartsState() = ChartsState(
        perDay = MedicinePerDayData(
            epochDays = listOf(20200L, 20201L, 20202L),
            series = listOf(
                MedicineDaySeries("Vitamin X 500 mg", listOf(1, 2, 1)),
                MedicineDaySeries("Medicine A", listOf(0, 1, 2)),
            ),
        ),
        dayLabels = persistentListOf("May 26", "May 27", "May 28"),
        seriesColors = persistentListOf(0xFF003F5C.toInt(), 0xFFFF7C43.toInt()),
        takenPeriod = 7,
        skippedPeriod = 3,
        takenTotal = 42,
        skippedTotal = 8,
        days = 7,
    )
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :feature:ui:testDebugUnitTest --tests "com.futsch1.medtimer.feature.ui.statistics.charts.ChartsContentLayoutTest"`
Expected: the portrait test PASSES (today's layout is already side-by-side); the landscape test FAILS its assertion (`total.top > period.top` is false because the pies render side-by-side regardless of orientation).

> If the landscape test instead fails because `currentWindowAdaptiveInfo()` does not see the `w800dp` width under Robolectric (both pies still side-by-side after Step 3's implementation), **stop** — the detection works on-device but not under this harness. Do not weaken the assertion; raise it for a decision on using `androidx.window:window-testing`'s `TestWindowMetricsCalculator` or an instrumented test instead.

- [ ] **Step 3: Implement the branch + `TwoPies`**

In `ChartsContent.kt`, add these imports alongside the existing ones (keep them in the file's alphabetical import order):

```kotlin
import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.ui.platform.LocalConfiguration
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
```

Replace the entire `ChartsContent` function (current lines 73–105) with:

```kotlin
// Tag-independent: tag filtering applies to the Table only, not to Charts.
@Composable
fun ChartsContent(state: ChartsState, modifier: Modifier = Modifier) {
    val configuration = LocalConfiguration.current
    // Inlined (not a shared helper) so future per-view adaptive tweaks stay local to each view.
    val isTabletLandscape =
        currentWindowAdaptiveInfo().windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND) &&
            configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Single bar-chart definition shared by both arrangements; the caller supplies the scoped weight.
    val barChart: @Composable (Modifier) -> Unit = { barModifier ->
        MedicinePerDayBarChart(
            epochDays = state.perDay.epochDays,
            series = state.perDay.series.map { it.counts },
            seriesNames = state.perDay.series.map { it.medicineName },
            dayLabels = state.dayLabels,
            seriesColors = state.seriesColors,
            modifier = barModifier,
        )
    }

    ProvideVicoTheme(rememberM3VicoTheme()) {
        if (isTabletLandscape) {
            Row(
                modifier = modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                barChart(Modifier.fillMaxHeight().weight(2f))
                TwoPies(state = state, stacked = true, modifier = Modifier.fillMaxHeight().weight(1f))
            }
        } else {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                barChart(Modifier.fillMaxWidth().weight(2f))
                TwoPies(state = state, stacked = false, modifier = Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}

@Composable
private fun TwoPies(state: ChartsState, stacked: Boolean, modifier: Modifier = Modifier) {
    val periodTitle = pluralStringResource(R.plurals.last_n_days, state.days, state.days)
    val totalTitle = stringResource(R.string.total)
    if (stacked) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TakenSkippedPieChart(periodTitle, state.takenPeriod, state.skippedPeriod, Modifier.weight(1f))
            TakenSkippedPieChart(totalTitle, state.takenTotal, state.skippedTotal, Modifier.weight(1f))
        }
    } else {
        Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TakenSkippedPieChart(periodTitle, state.takenPeriod, state.skippedPeriod, Modifier.weight(1f))
            TakenSkippedPieChart(totalTitle, state.takenTotal, state.skippedTotal, Modifier.weight(1f))
        }
    }
}
```

Leave `TakenSkippedPieChart`, `EmptyPieCircle`, `LegendDot`, and `MedicinePerDayBarChart` unchanged.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :feature:ui:testDebugUnitTest --tests "com.futsch1.medtimer.feature.ui.statistics.charts.ChartsContentLayoutTest"`
Expected: both tests PASS.

- [ ] **Step 5: Build + lint**

Run: `./gradlew :feature:ui:assembleDebug :feature:ui:lint`
Expected: `BUILD SUCCESSFUL`, no new lint findings (160-char line length holds; no `@Suppress` added).

- [ ] **Step 6: Commit**

```bash
git add feature/ui/src/main/java/com/futsch1/medtimer/feature/ui/statistics/charts/ChartsContent.kt \
        feature/ui/src/test/java/com/futsch1/medtimer/feature/ui/statistics/charts/ChartsContentLayoutTest.kt
git commit -m "#1234 Stack Analysis pie charts beside bar chart in tablet landscape"
```

---

## Task 3: Add a landscape preview

**Files:**
- Modify: `feature/ui/src/main/java/com/futsch1/medtimer/feature/ui/statistics/charts/ChartsContent.kt` (add a preview beside the existing `ChartsContentPreview`)

- [ ] **Step 1: Add the preview**

Add the `import androidx.compose.ui.tooling.preview.Preview` (only if not already present) and, directly below the existing `ChartsContentPreview` function, add a wide landscape preview. It reuses the same synthetic state shape:

```kotlin
@Preview(name = "Charts — tablet landscape", widthDp = 900, heightDp = 480)
@Composable
private fun ChartsContentLandscapePreview() {
    MedTimerTheme {
        Surface {
            ChartsContent(
                state = ChartsState(
                    perDay = MedicinePerDayData(
                        epochDays = listOf(20200L, 20201L, 20202L),
                        series = listOf(
                            MedicineDaySeries("Vitamin X 500 mg", listOf(1, 2, 1)),
                            MedicineDaySeries("Medicine A", listOf(0, 1, 2)),
                        ),
                    ),
                    dayLabels = persistentListOf("May 26", "May 27", "May 28"),
                    seriesColors = persistentListOf(0xFF003F5C.toInt(), 0xFFFF7C43.toInt()),
                    takenPeriod = 7,
                    skippedPeriod = 3,
                    takenTotal = 42,
                    skippedTotal = 8,
                    days = 7,
                ),
            )
        }
    }
}
```

> Note: `@Preview(widthDp/heightDp)` sizes the canvas but does not by itself drive `currentWindowAdaptiveInfo()`, so this preview renders the portrait arrangement in the IDE. It exists to review the pies/bar at a wide canvas; the landscape arrangement itself is covered by the `w800dp-land` test in Task 2. If the existing `ChartsContentPreview` already imports `Preview` via `@MedTimerPreview`'s machinery, prefer matching that pattern — check the top of the file before adding a raw `@Preview` import.

- [ ] **Step 2: Build to verify the preview compiles**

Run: `./gradlew :feature:ui:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add feature/ui/src/main/java/com/futsch1/medtimer/feature/ui/statistics/charts/ChartsContent.kt
git commit -m "#1234 Add wide-canvas preview for Analysis Charts"
```

---

## Final Verification

- [ ] `./gradlew :feature:ui:testDebugUnitTest --tests "com.futsch1.medtimer.feature.ui.statistics.charts.ChartsContentLayoutTest"` — both arrangement tests pass.
- [ ] `./gradlew assembleFullDebug assembleFossDebug` — both flavors build (adaptive dep resolves on both).
- [ ] `./gradlew lint` — clean, no new suppressions.
- [ ] Manual sanity (optional, on a tablet emulator/device): rotate the Charts view to landscape — bar chart left, two pies stacked right; portrait unchanged; Table and Calendar views unchanged in both orientations.

---

## Self-Review Notes

- **Spec coverage:** dependency (Task 1) ✓; inlined detection + 2:1 `Row` branch (Task 2) ✓; `TwoPies` DRY factoring (Task 2) ✓; `w800dp-land` Robolectric test + wide preview (Tasks 2–3) ✓; non-goals respected (no ViewModel/Table/Calendar/manifest/`material3`-pin changes) ✓.
- **Naming consistency:** `TwoPies(state, stacked, modifier)`, `isTabletLandscape`, `barChart` lambda, and the `WIDTH_DP_MEDIUM_LOWER_BOUND` import are used identically across the implementation and the prose.
- **Known risk, surfaced not hidden:** Robolectric honoring the `w800dp` width for `currentWindowAdaptiveInfo()` is the one uncertainty; Task 2 Step 2 calls it out with a stop-and-ask contingency rather than papering over it.
