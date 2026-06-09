# Analysis Calendar — landscape tablet layout

**Date:** 2026-06-03
**Branch:** `feature/1234-analysis-screen-jetpack-compose`
**Status:** Approved design — ready for implementation plan

## Goal

On a tablet in landscape, lay out the Analysis **Calendar** view as calendar-left,
event-panel-right (2:1 width split) instead of the current stacked column. Phones
and portrait keep today's top-to-bottom layout. This applies the same adaptive
pattern already shipped for the Charts view to the Calendar view.

## Scope

- **Changes:** `feature/ui/.../statistics/calendar/CalendarContent.kt`, plus a test
  and an extra preview.
- **Unchanged:** `StatisticsScreen` (header chips + range dropdown + tab
  container), the Charts view, the Table view, all ViewModel/state/data code, the
  `DayEventsCard`, `CalendarNavigationRow`, `DayCell`, and the 400dp calendar cap.
- **No new dependency** — `androidx.compose.material3.adaptive:adaptive:1.2.0` is
  already wired into `:feature:ui` from the Charts work.

## Decisions (from brainstorming)

1. **Split:** calendar `weight(2f)` (left) : event panel `weight(1f)` (right).
2. **Calendar width:** keep the existing `widthIn(max = 400.dp)` cap — the
   calendar stays centered in its 2/3 pane (accepting side whitespace on very wide
   tablets) rather than stretching to fill.
3. **Detection inlined** in `CalendarContent` via an injectable
   `windowAdaptiveInfo` parameter — same pattern as `ChartsContent` (Now in
   Android precedent), so layout tests inject a computed window size.

## Design

### 1. Inlined, injectable adaptive detection

`CalendarContent` gains a parameter (after the existing `dayEvents`, `modifier`,
`pastMonths`, `futureMonths`):

```kotlin
windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
```

`CalendarContent` already imports `androidx.compose.ui.platform.LocalConfiguration`.
Add:
```kotlin
import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
```

Detection (computed once near the top, after the existing state is set up):
```kotlin
val configuration = LocalConfiguration.current
val isTabletLandscape = remember(windowAdaptiveInfo, configuration) {
    windowAdaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND) &&
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}
```

### 2. Factor the two parts into modifier-taking lambdas

To avoid duplicating the two halves across the branches, capture the existing
locals in two local composable lambdas (the `barChart`-lambda pattern from
`ChartsContent`):

- `calendarCard: @Composable (Modifier) -> Unit` — the existing
  `Card { Column { CalendarNavigationRow(...); HorizontalCalendar(...) } }`, with
  the `HorizontalCalendar`'s `widthIn(max = 400.dp).align(CenterHorizontally)`
  unchanged. The lambda applies the passed `Modifier` to the `Card`.
- `eventPanel: @Composable (Modifier) -> Unit` — the existing
  `AnimatedContent(targetState = selectedDate, ...) { date -> ... DayEventsCard }`,
  applying the passed `Modifier` to the `AnimatedContent`.

### 3. Layout branch

- **Portrait / compact (behavior-identical to today):**
  ```kotlin
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
      calendarCard(Modifier)
      eventPanel(Modifier)
  }
  ```
  Passing the default `Modifier` preserves the current wrap-content sizing exactly
  (today the `Card` and `AnimatedContent` take no modifier).
- **Tablet landscape (new):**
  ```kotlin
  Row(modifier = modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      calendarCard(Modifier.weight(2f))
      eventPanel(Modifier.weight(1f))
  }
  ```
  Top-aligned (Row default `Alignment.Top`).

```
Portrait (today)            Tablet landscape (new)
+----------------+          +--------------+------+
|   calendar     |          |              |events|
+----------------+          |  calendar    | for  |
|  events (day)  |          |  (2)         | day  |
+----------------+          +--------------+------+
```

### 4. Tests

`feature/ui/src/test/.../calendar/CalendarContentLayoutTest.kt`, mirroring
`ChartsContentLayoutTest`: Robolectric + `createComposeRule`, inject
`WindowAdaptiveInfo(BREAKPOINTS_V1.computeWindowSizeClass(w, h), Posture())` and
wrap in `CompositionLocalProvider(LocalInspectionMode provides true)`. Synthetic
data only ("Vitamin X 500 mg"). Three position-based tests:

- **tablet landscape** (`@Config(qualifiers = "land")`, wide window) — event panel
  is to the **right** of the calendar (`event.left > calendar.left`).
- **phone portrait** (default/narrow window) — event panel is **below** the
  calendar (`event.top > calendar.top`).
- **wide tablet in portrait** (`@Config(qualifiers = "port")`, wide window) —
  **below** as well, proving the orientation guard (not just the width guard)
  governs the switch.

Node anchors are semantics-first and finalized in the plan after inspecting
`DayEventsCard` and `CalendarNavigationRow` for stable text. Working assumption:
anchor the event panel by the synthetic medicine name ("Vitamin X 500 mg",
rendered only in `DayEventsCard`) and the calendar by a stable element of the
calendar Card (a weekday header or the month-navigation label). The selected date
defaults to `LocalDate.now()`, so the chosen day must carry the synthetic event.

### 5. Preview

Add a wide landscape `@Preview` (e.g. `widthDp = 900, heightDp = 480`) that injects
a medium-width `WindowAdaptiveInfo` and a landscape `LocalConfiguration` override,
so it renders the side-by-side arrangement in the IDE (same technique as
`ChartsContentLandscapePreview`). Keep the existing `CalendarContentPreview`.

## Verification

- `./gradlew :feature:ui:testFullDebugUnitTest :feature:ui:testFossDebugUnitTest`
  — the new `CalendarContentLayoutTest` passes on both flavors.
- `./gradlew assembleFullDebug assembleFossDebug` — both flavors build.
- `./gradlew lint` — clean, no new suppressions.

## Non-goals

- No data/ViewModel/state changes.
- No landscape handling for the Charts (already done) or Table views.
- The `widthIn(max = 400.dp)` calendar cap is unchanged.
- No new scrolling behavior for either pane.
- No manifest or `android:configChanges` changes.
- No extraction of a shared `isTabletLandscape()` helper (intentionally inlined,
  consistent with `ChartsContent`).
