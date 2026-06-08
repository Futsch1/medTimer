# Analysis Calendar — Landscape Tablet Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** On a tablet in landscape, lay out the Analysis Calendar view as calendar-left (`weight 2f`) and event-panel-right (`weight 1f`); phones and portrait keep today's stacked column.

**Architecture:** A single inlined adaptive check in `CalendarContent` selects a `Row` (landscape) or the existing `Column` (otherwise). The calendar `Card` and the event-panel `AnimatedContent` are each hoisted into a local `@Composable (Modifier) -> Unit` lambda (the `barChart`-lambda pattern from `ChartsContent`) so neither branch duplicates them. Detection uses an injectable `windowAdaptiveInfo` parameter (Now-in-Android pattern) so layout tests inject a computed `WindowSizeClass`.

**Tech Stack:** Jetpack Compose, Material 3, `androidx.compose.material3.adaptive:adaptive:1.2.0` (already a dependency from the Charts work), `androidx.window` `WindowSizeClass` (transitive 1.5.x), Kizitonwose calendar-compose, Robolectric Compose tests.

**Source spec:** `docs/superpowers/specs/2026-06-03-analysis-calendar-landscape-tablet-design.md`

---

## File Structure

- **Modify** `feature/ui/src/main/java/com/futsch1/medtimer/feature/ui/statistics/calendar/CalendarContent.kt` — inlined detection, `Row`/`Column` branch, two hoisted lambdas, new landscape `@Preview`.
- **Create** `feature/ui/src/test/java/com/futsch1/medtimer/feature/ui/statistics/calendar/CalendarContentLayoutTest.kt` — three Robolectric position tests.

No dependency or gradle changes — `material3-adaptive` is already wired into `:feature:ui`.

---

## Task 1: Branch `CalendarContent` on tablet-landscape, hoist the two panes (TDD)

The failing test asserts the landscape arrangement (event panel right of the calendar), which today's unconditional `Column { calendar; events }` cannot satisfy.

**Files:**
- Create: `feature/ui/src/test/java/com/futsch1/medtimer/feature/ui/statistics/calendar/CalendarContentLayoutTest.kt`
- Modify: `feature/ui/src/main/java/com/futsch1/medtimer/feature/ui/statistics/calendar/CalendarContent.kt` (the `CalendarContent` function, currently lines 49–123)

- [ ] **Step 1: Write the failing test**

Create `feature/ui/src/test/java/com/futsch1/medtimer/feature/ui/statistics/calendar/CalendarContentLayoutTest.kt` with EXACTLY this content:

```kotlin
package com.futsch1.medtimer.feature.ui.statistics.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowSizeClass.Companion.BREAKPOINTS_V1
import androidx.window.core.layout.computeWindowSizeClass
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.test.assertTrue

/**
 * Verifies the adaptive arrangement of [CalendarContent]: the event panel sits to the right of the
 * calendar on a tablet in landscape, and below it otherwise. WindowAdaptiveInfo is injected (so the
 * width signal does not depend on host window metrics) and LocalInspectionMode is set so embedded
 * charts/calendar take their deterministic render paths. The calendar is anchored by its month-year
 * navigation title (a sibling of the HorizontalCalendar) and the event panel by the synthetic
 * medicine name rendered only inside DayEventsCard.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CalendarContentLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Matches CalendarNavigationRow's "<FullMonth> <year>" title for the initially-visible month (now).
    private val calendarTitle: String = run {
        val visibleMonth = YearMonth.now()
        "${visibleMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${visibleMonth.year}"
    }

    @Test
    @Config(qualifiers = "land")
    fun `tablet landscape places the event panel to the right of the calendar`() {
        setCalendar(width = 900.dp, height = 500.dp, windowSizeClass = BREAKPOINTS_V1.computeWindowSizeClass(widthDp = 900f, heightDp = 500f))

        val calendar = composeTestRule.onNodeWithText(calendarTitle).getBoundsInRoot()
        val event = composeTestRule.onNode(hasText("Vitamin X 500 mg", substring = true)).getBoundsInRoot()

        assertTrue(
            event.left > calendar.left,
            "Expected the event panel right of the calendar; calendar.left=${calendar.left}, event.left=${event.left}",
        )
    }

    @Test
    fun `phone portrait stacks the event panel below the calendar`() {
        setCalendar(width = 420.dp, height = 900.dp, windowSizeClass = BREAKPOINTS_V1.computeWindowSizeClass(widthDp = 420f, heightDp = 900f))

        val calendar = composeTestRule.onNodeWithText(calendarTitle).getBoundsInRoot()
        val event = composeTestRule.onNode(hasText("Vitamin X 500 mg", substring = true)).getBoundsInRoot()

        assertTrue(
            event.top > calendar.top,
            "Expected the event panel below the calendar; calendar.top=${calendar.top}, event.top=${event.top}",
        )
    }

    @Test
    @Config(qualifiers = "port")
    fun `wide tablet in portrait stacks the event panel below the calendar`() {
        // Width passes the medium breakpoint, so only the orientation guard keeps the layout stacked.
        setCalendar(width = 900.dp, height = 1200.dp, windowSizeClass = BREAKPOINTS_V1.computeWindowSizeClass(widthDp = 900f, heightDp = 1200f))

        val calendar = composeTestRule.onNodeWithText(calendarTitle).getBoundsInRoot()
        val event = composeTestRule.onNode(hasText("Vitamin X 500 mg", substring = true)).getBoundsInRoot()

        assertTrue(
            event.top > calendar.top,
            "Wide tablet in portrait should stack; calendar.top=${calendar.top}, event.top=${event.top}",
        )
    }

    private fun setCalendar(width: Dp, height: Dp, windowSizeClass: WindowSizeClass) {
        val today = LocalDate.now()
        composeTestRule.setContent {
            MedTimerTheme {
                CompositionLocalProvider(LocalInspectionMode provides true) {
                    Box(modifier = Modifier.size(width, height)) {
                        CalendarContent(
                            dayEvents = mapOf(
                                today to listOf(
                                    CalendarDayEvent(today.atTime(8, 0), "1 tablet", "Vitamin X 500 mg", CalendarDayEvent.Status.TAKEN),
                                ),
                            ),
                            windowAdaptiveInfo = WindowAdaptiveInfo(
                                windowSizeClass = windowSizeClass,
                                windowPosture = Posture(),
                            ),
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Run the test to verify it fails (red)**

Run: `./gradlew :feature:ui:testFullDebugUnitTest --tests "com.futsch1.medtimer.feature.ui.statistics.calendar.CalendarContentLayoutTest"`
Expected red state: a COMPILATION failure, because `CalendarContent` does not yet accept a `windowAdaptiveInfo` parameter (added in Step 3). That compile failure IS the red state — confirm you see it, then proceed.

> Known risk to watch: if, after Step 3, the tests error because the Kizitonwose `HorizontalCalendar` fails to render/compose under Robolectric (rather than failing the position assertion), STOP and report BLOCKED with the exact stack trace. Do NOT weaken assertions. The fallback (only if needed, decided with the controller) is to add a `Modifier.testTag("calendarPane")` to the calendar `Card` and `Modifier.testTag("eventPane")` to the event `AnimatedContent` and anchor on those tags via `onNodeWithTag`. Try the text anchors first.

- [ ] **Step 3: Implement the branch + hoisted lambdas**

In `CalendarContent.kt`, ADD these imports (keep the file's existing alphabetical ordering; do NOT duplicate — `LocalConfiguration`, `Arrangement`, `Row`, `Column`, `widthIn`, `padding`, `fillMaxWidth` are already present, but `fillMaxSize` is NOT):

```kotlin
import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
```

Replace the ENTIRE existing `CalendarContent` function (currently lines 49–123, from `@Composable` / `fun CalendarContent(` through its closing brace) with:

```kotlin
@Composable
fun CalendarContent(
    dayEvents: Map<LocalDate, List<CalendarDayEvent>>,
    modifier: Modifier = Modifier,
    pastMonths: Int = 3,
    futureMonths: Int = 0,
    // Injectable (defaults to the live value) so layout tests supply a computed window size. The
    // detection below stays inlined per the "no shared helper" decision. Matches ChartsContent.
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
) {
    // Pre-select today so its events show on open, matching the legacy calendar's default day.
    var selectedDate by rememberSaveable { mutableStateOf<LocalDate?>(LocalDate.now()) }
    val startMonth = remember { YearMonth.now().minusMonths(pastMonths.toLong()) }
    val endMonth = remember { YearMonth.now().plusMonths(futureMonths.toLong()) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }

    val calendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = YearMonth.now(),
        firstDayOfWeek = firstDayOfWeek,
        outDateStyle = OutDateStyle.EndOfGrid,
    )
    val coroutineScope = rememberCoroutineScope()
    val visibleMonth = calendarState.firstVisibleMonth.yearMonth

    val configuration = LocalConfiguration.current
    val isTabletLandscape = remember(windowAdaptiveInfo, configuration) {
        windowAdaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND) &&
            configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    // The calendar Card; the caller supplies the scoped weight (landscape) or default Modifier (portrait).
    val calendarCard: @Composable (Modifier) -> Unit = { cardModifier ->
        Card(
            modifier = cardModifier,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                CalendarNavigationRow(
                    yearMonth = visibleMonth,
                    startMonth = startMonth,
                    endMonth = endMonth,
                    onPrev = {
                        coroutineScope.launch { calendarState.animateScrollToMonth(visibleMonth.previousMonth) }
                    },
                    onNext = {
                        coroutineScope.launch { calendarState.animateScrollToMonth(visibleMonth.nextMonth) }
                    },
                    onYearSelected = { year ->
                        val target = YearMonth.of(year, visibleMonth.monthValue).coerceIn(startMonth, endMonth)
                        coroutineScope.launch { calendarState.animateScrollToMonth(target) }
                    },
                )

                HorizontalCalendar(
                    state = calendarState,
                    dayContent = { day ->
                        val hasEvents = dayEvents[day.date]?.isNotEmpty() == true
                        DayCell(
                            day = day,
                            isSelected = day.date == selectedDate,
                            hasEvents = hasEvents,
                            onClick = { selectedDate = day.date },
                        )
                    },
                    monthHeader = { WeekDaysRow(firstDayOfWeek = firstDayOfWeek) },
                    modifier = Modifier
                        .widthIn(max = 400.dp)
                        .align(Alignment.CenterHorizontally),
                )
            }
        }
    }

    // The day-events panel; the caller supplies the scoped weight (landscape) or default Modifier (portrait).
    val eventPanel: @Composable (Modifier) -> Unit = { panelModifier ->
        AnimatedContent(
            targetState = selectedDate,
            transitionSpec = {
                (fadeIn(tween(300)) + expandVertically(tween(300)))
                    .togetherWith(fadeOut(tween(300)) + shrinkVertically(tween(300)))
                    .using(SizeTransform(clip = false))
            },
            label = "dayEventsCard",
            modifier = panelModifier,
        ) { date ->
            if (date == null) return@AnimatedContent
            DayEventsCard(date = date, events = dayEvents[date].orEmpty())
        }
    }

    if (isTabletLandscape) {
        Row(modifier = modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            calendarCard(Modifier.weight(2f))
            eventPanel(Modifier.weight(1f))
        }
    } else {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            calendarCard(Modifier)
            eventPanel(Modifier)
        }
    }
}
```

Leave `WeekDaysRow`, `CalendarNavigationRow`, `DayCell`, `DayEventsCard`, and `CalendarDayEvent` unchanged. The `StatisticsScreen` call site (`CalendarContent(dayEvents = state.calendarDayEvents)`) needs no change — it uses the `windowAdaptiveInfo` default. The portrait branch is behavior-identical to the original: `calendarCard(Modifier)` / `eventPanel(Modifier)` pass the default `Modifier`, so the `Card` and `AnimatedContent` are exactly as before (they took no modifier originally).

- [ ] **Step 4: Run the test to verify it passes (green)**

Run: `./gradlew :feature:ui:testFullDebugUnitTest --tests "com.futsch1.medtimer.feature.ui.statistics.calendar.CalendarContentLayoutTest"`
Expected: all THREE tests PASS.

- [ ] **Step 5: Build + lint**

Run: `./gradlew :feature:ui:assembleDebug :feature:ui:lint`
Expected: `BUILD SUCCESSFUL`, no new lint findings. The project enforces a 160-char line length and `warningsAsErrors`/`abortOnError`. Do NOT add any `@Suppress`.

- [ ] **Step 6: Commit**

```bash
git add feature/ui/src/main/java/com/futsch1/medtimer/feature/ui/statistics/calendar/CalendarContent.kt \
        feature/ui/src/test/java/com/futsch1/medtimer/feature/ui/statistics/calendar/CalendarContentLayoutTest.kt
git commit -m "#1234 Place Analysis calendar beside event panel in tablet landscape" -m "Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 2: Add a landscape preview

**Files:**
- Modify: `feature/ui/src/main/java/com/futsch1/medtimer/feature/ui/statistics/calendar/CalendarContent.kt` (add a preview beside the existing `CalendarContentPreview`)

- [ ] **Step 1: Add the preview**

ADD these imports (only those missing — `Configuration`, `LocalConfiguration`, `WindowAdaptiveInfo`, `currentWindowAdaptiveInfo` were added in Task 1; `Posture`, `CompositionLocalProvider`, `Preview`, `BREAKPOINTS_V1`, `computeWindowSizeClass` are new):

```kotlin
import androidx.compose.material3.adaptive.Posture
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import androidx.window.core.layout.WindowSizeClass.Companion.BREAKPOINTS_V1
import androidx.window.core.layout.computeWindowSizeClass
```

Directly below the existing `CalendarContentPreview()` function, add:

```kotlin
@Preview(name = "Calendar — tablet landscape", widthDp = 900, heightDp = 480)
@Composable
private fun CalendarContentLandscapePreview() {
    // A wide @Preview canvas alone drives neither currentWindowAdaptiveInfo() nor orientation, so the
    // tablet-landscape branch is forced explicitly: a medium-width window size class + a landscape config.
    val landscapeConfiguration = Configuration(LocalConfiguration.current).apply {
        orientation = Configuration.ORIENTATION_LANDSCAPE
    }
    val today = LocalDate.now()
    MedTimerTheme {
        Surface {
            CompositionLocalProvider(LocalConfiguration provides landscapeConfiguration) {
                CalendarContent(
                    dayEvents = mapOf(
                        today to listOf(
                            CalendarDayEvent(today.atTime(8, 0), "1 tablet", "Vitamin X 500 mg", CalendarDayEvent.Status.TAKEN),
                        ),
                        today.minusDays(2) to listOf(
                            CalendarDayEvent(today.minusDays(2).atTime(20, 0), "2 ml", "Medicine A", CalendarDayEvent.Status.SKIPPED),
                        ),
                    ),
                    windowAdaptiveInfo = WindowAdaptiveInfo(
                        windowSizeClass = BREAKPOINTS_V1.computeWindowSizeClass(widthDp = 900f, heightDp = 480f),
                        windowPosture = Posture(),
                    ),
                )
            }
        }
    }
}
```

Keep the existing `CalendarContentPreview` unchanged.

- [ ] **Step 2: Build to verify the preview compiles**

Run: `./gradlew :feature:ui:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add feature/ui/src/main/java/com/futsch1/medtimer/feature/ui/statistics/calendar/CalendarContent.kt
git commit -m "#1234 Add tablet-landscape preview for Analysis Calendar" -m "Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Final Verification

- [ ] `./gradlew :feature:ui:testFullDebugUnitTest :feature:ui:testFossDebugUnitTest --tests "com.futsch1.medtimer.feature.ui.statistics.calendar.CalendarContentLayoutTest"` — all three tests pass on both flavors.
- [ ] `./gradlew assembleFullDebug assembleFossDebug` — both flavors build.
- [ ] `./gradlew lint` — clean, no new suppressions.
- [ ] Manual sanity (optional, tablet emulator): rotate the Calendar view to landscape — calendar left, event panel right; tapping a day updates the right panel; portrait unchanged; Charts and Table unchanged.

---

## Self-Review Notes

- **Spec coverage:** injectable `windowAdaptiveInfo` + inlined detection (Task 1) ✓; `Row` 2:1 branch with calendar left / events right (Task 1) ✓; `calendarCard`/`eventPanel` lambda hoisting to avoid duplication (Task 1) ✓; 400dp cap unchanged (Task 1, `HorizontalCalendar` modifier copied verbatim) ✓; three position tests incl. wide-portrait orientation-guard test (Task 1) ✓; landscape preview (Task 2) ✓; non-goals respected (no ViewModel/data, no Charts/Table changes, no manifest, no new scroll) ✓.
- **Naming consistency:** `CalendarContent(dayEvents, modifier, pastMonths, futureMonths, windowAdaptiveInfo)`, `calendarCard`, `eventPanel`, `isTabletLandscape`, `calendarTitle`, and `WIDTH_DP_MEDIUM_LOWER_BOUND` are used identically across the implementation and the test.
- **Known risk, surfaced not hidden:** rendering the Kizitonwose `HorizontalCalendar` under Robolectric is the one uncertainty; Task 1 Step 2 calls it out with a stop-and-ask contingency (text anchors first; `testTag` fallback only if the calendar won't render) rather than papering over it.
- **Consistency with Charts:** mirrors `ChartsContentLayoutTest` (injected `WindowAdaptiveInfo`, `LocalInspectionMode`, `BREAKPOINTS_V1.computeWindowSizeClass`, the wide-portrait orientation-guard test) so the two adaptive views stay uniform.
