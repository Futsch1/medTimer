# Plan — Migrate the Statistics screen to Jetpack Compose

Migrate the Statistics screen and its three views — **Charts**, **Reminder Table**, **Calendar** —
from XML Fragments to Jetpack Compose, in the current multi-module structure, following
[`docs/guidelines/`](../guidelines/README.md).

> **Naming decision (reversed 2026-05-29):** the codebase **retains the existing `Statistics*`
> naming** — there is **no** `Statistics` → `Analysis` rename. Existing Kotlin symbols
> (`StatisticFragment`, `StatisticsFragment`, `StatisticsProvider`, `activeStatisticsFragment`,
> `setActiveStatisticsFragment`, the `…feature.ui.statistics` package), SharedPreferences keys, and
> **all translations** (`statistics_tab_description` and its localized values) stay **unchanged**. The
> user-facing tab label remains the pre-existing `@string/analysis` string (also unchanged). Library
> strategy rationale is recorded in
> [ADR 0001](../adr/0001-compose-analysis-charting-libraries.md).
>
> [`CONTEXT.md`](../../CONTEXT.md) and ADR 0001 have been **reconciled** with this reversal
> (2026-05-29): code keeps `Statistics*`, the UI tab keeps its "Analysis" label. See
> [§10](#10-documentation-updates).

This is the **pathfinder Compose screen** — there is currently *zero* Compose in the repo, so it also
stands up the shared Compose foundation in `:core:ui`.

---

## 1. Decisions (resolved)

| # | Decision | Choice | Why |
|---|----------|--------|-----|
| 1 | Module placement | Screen in `:feature:ui`; theme + reusables in `:core:ui` | Follows `jetpack-compose.md`; keeps re-modularization out of this migration |
| 2 | Granularity | Big-bang: one Compose `StatisticsScreen` replaces the container + 3 child fragments | Self-contained screen; incremental leaf-conversion creates throwaway interop |
| 3 | Screen function naming | **Overloaded** `StatisticsScreen` (stateful VM overload + stateless state overload); `@Preview` targets the stateless overload | User-chosen; guideline updated to match (deviates from `*Content` suffix) |
| 4 | Charts | **Vico 3.1** for both the bar chart *and* the pie charts (`rememberPieChart`); drop AndroidPlot | One Compose-native lib, no GMS; Vico 3.1 added pie charts. ADR 0001 |
| 5 | Table | Hand-rolled `SortableTable` composable in `:core:ui`; drop `evrencoskun:TableView` | Reusable, idiomatic. ADR 0001 |
| 6 | Calendar | Kizitonwose **`:compose`** artifact alongside existing **`:view`** (kept for Overview) | Same trusted library, idiomatic Compose |
| 7 | State management | State-holder pattern: `StatisticsScreenState` interface + mutable impl (`mutableStateOf`/`derivedStateOf`/`ImmutableList`) | `jetpack-compose.md` prescribed pattern for Compose screens |
| 8 | ViewModel topology | One `StatisticsScreenViewModel` (tab + range + charts + table) + separate `CalendarEventsViewModel` | Shared screen state in one place; calendar paging is independent |
| 9 | Filtering semantics | Preserve per-view behavior: Table tag-filtered, Charts unfiltered, Calendar per-medicine | Avoid regression; extract tag-filter helper out of `MedicineViewModel` |
| 10 | Testing | JVM-first: Robolectric Compose tests on stateless overload + reusables; VM/provider unit tests; migrate 3 instrumented tests | `testing.md` JVM-first; `jetpack-compose.md` test the stateless half |
| 11 | Hosting | `StatisticsFragment` returns a `ComposeView`; nav graph + bottom nav unchanged; single `onEditEvent` interop callback | Keep Navigation component; minimal interop seam |
| 12 | Build enablement | Inline per-module Compose setup in `:core:ui` + `:feature:ui` (no `build-logic`) | Proportionate; no new build infra |
| 13 | Naming | **No rename** — retain the existing `Statistics*` code symbols, package, SharedPreferences keys, and translations | User decision (2026-05-29): keep the established code naming; do not modify class names or translations |

---

## 2. No precursor rename

The earlier plan opened with a `Statistics` → `Analysis` rename commit. **That step is cancelled.**
The migration keeps every existing `Statistics*` symbol, the `…feature.ui.statistics` package, the
`StatisticFragment` enum (stored by ordinal), all SharedPreferences key strings
(`active_statistics_fragment`, `analysisDays`, `filterTags`), and **all string translations**
(`statistics_tab_description` and its localized values) exactly as they are on `main`.

New Compose code introduced by this migration follows the **same retained naming** (e.g.
`StatisticsScreen`, `StatisticsScreenViewModel`, `StatisticsScreenState`) and lives in the existing
`…feature.ui.statistics` package.

---

## 3. Target structure (after migration)

```
core/ui/                          (Compose foundation lives here)
  MedTimerTheme.kt                 MaterialExpressiveTheme; dynamic color (API 31+) + Kotlin-defined static fallback
  MedTimerColors.kt                static light/dark ColorScheme defined in Kotlin (no colors.xml today)
  MedTimerPreview.kt               @MedTimerPreview multipreview (light + dark + min font scale)
  component/SortableTable.kt       sortable + filterable table (replaces TableView)
  filter/TagEventFilter.kt         tag-filter logic extracted from MedicineViewModel

feature/ui/.../statistics/        (the screen — existing package, retained)
  StatisticsFragment.kt            hosts ComposeView; fulfills onEditEvent via EditEventSheetDialogFragment
  StatisticsScreen.kt              overloaded: StatisticsScreen(vm) + StatisticsScreen(state, onEvent…)
  StatisticsScreenState.kt         interface + MutableStatisticsScreenState (mutableStateOf/derivedStateOf)
  StatisticsScreenViewModel.kt     @HiltViewModel; tab + range + charts + table; reads/writes PersistentDataDataSource
  StatisticsProvider.kt            pure calc over ReminderEventRepository — kept (unchanged name)
  charts/ChartsContent.kt, MedicinePerDayBarChart.kt (Vico), TakenSkippedPieChart.kt (Vico rememberPieChart), RangeDropdown.kt
  table/ReminderTable.kt           uses core/ui SortableTable
  calendar/CalendarContent.kt, DayCell.kt, DayEventsCard.kt, CalendarNavigationRow.kt
  calendar/CalendarEventsViewModel.kt  kept, adapted
  preview/PreviewData.kt           immutable sample state for previews
```

**Deleted:** `ChartsFragment`, `CalendarFragment`, `ReminderTableFragment` (+ adapter, cell model,
view holders), the old `StatisticsFragment` swap logic, `AnalysisDays` helper, and layouts
`fragment_statistics.xml`, `fragment_charts.xml`, `fragment_calendar.xml`,
`fragment_reminder_table.xml`, `calendar_*` layouts.

Module dependency direction is preserved: `:feature:ui` → `:core:ui` → `:core:domain`. No `*Entity`
types cross a module boundary.

---

## 4. Dependencies (catalog-pinned — "ask first" items flagged)

> **Build context (verified):** AGP is **9.2.1**, which has **built-in Kotlin** — there is no
> `org.jetbrains.kotlin.android` plugin in the catalog. AGP 9.2.1 bundles **kotlin-gradle-plugin
> 2.2.10** (read from its POM); the catalog's `ksp = "2.3.8"` is **KSP's own version, not Kotlin's**.
> Enabling `buildFeatures { compose = true }` alone **fails** under AGP 9 ("the Compose Compiler Gradle
> plugin is required when compose is enabled"), so `org.jetbrains.kotlin.plugin.compose` is **required,
> not optional**, and is pinned to the bundled Kotlin version (**2.2.10**) — applied per Compose module.

**Add** (`gradle/libs.versions.toml`):

- `androidx.compose:compose-bom` (BOM, `2026.05.00`) + `compose-ui`, `compose-ui-graphics`,
  `compose-ui-tooling`/`-tooling-preview`. `compose-material-icons-extended` **omitted** (deprecated;
  `SortableTable` uses no icon artifacts). ⚠️ **`compose-material3` is hand-pinned to `1.5.0-alpha20`**
  (overriding the BOM) because `MaterialExpressiveTheme` is `internal` on the stable 1.4.x line. This
  alpha transitively pulls the whole Compose runtime to **1.12.0-alpha03**, which **requires
  `compileSdk 37`** — so `compileSdk` was bumped `36 → 37` project-wide (targetSdk stays 36). Revert to
  BOM-managed material3 + `compileSdk 36` once Expressive ships stable.
- `androidx.lifecycle:lifecycle-runtime-compose` (`collectAsStateWithLifecycle`).
- `com.patrykandpatrick.vico:compose` + `:compose-m3` — pin **3.1** (latest; adds the Compose pie chart). ⚠️ ask-first (new dep).
- `com.kizitonwose.calendar:compose` — pin to the **same version ref** as the existing `:view` artifact (`calendar = 2.10.1`).
- `org.jetbrains.kotlinx:kotlinx-collections-immutable` (`ImmutableList` state stability).
- Test: `androidx.compose.ui:ui-test-junit4`, `ui-test-manifest` (debug). Robolectric `4.16.1` already pinned.
- **Required** plugin `org.jetbrains.kotlin.plugin.compose` — pinned to **2.2.10** (matches AGP 9.2.1's bundled Kotlin); applied in each Compose module. Verified: `compose = true` alone fails without it.

**Remove** (after migration + test updates land):

- `com.androidplot:androidplot-core` — from `app/build.gradle.kts`, `feature/ui/build.gradle.kts`, and the catalog (Statistics is its only user).
- `com.github.evrencoskun:TableView` — from `feature/ui/build.gradle.kts` and the catalog once the two instrumented tests no longer drive it.

> `hilt-navigation-compose` / `navigation-compose` are **not** needed: the VM is obtained by the
> Fragment (`by viewModels()`) and passed into the stateful `StatisticsScreen(vm)` overload.

---

## 5. Build enablement (inline, no build-logic)

In **`:core:ui`** and **`:feature:ui`** `build.gradle.kts`:

- `android { buildFeatures { compose = true } }` **plus** `alias(libs.plugins.kotlin.compose)`
  (`org.jetbrains.kotlin.plugin.compose` 2.2.10). Under AGP 9.2.1 the compose flag alone fails — the
  Compose Compiler Gradle plugin is mandatory. No `kotlin.android` alias is needed (Kotlin is
  built-in).
- Add the Compose BOM (`implementation(platform(libs.androidx.compose.bom))`) + Compose deps.

Matches the existing per-module `alias(...)` style. Convention plugins (`build-logic`) deferred to a
later effort if Compose spreads.

---

## 6. `:core:ui` foundation

- **`MedTimerTheme`** wrapping **`MaterialExpressiveTheme`**. The current app theme parent is
  `Theme.Material3.DynamicColors`, so mirror that in Kotlin: `dynamicLight/DarkColorScheme(context)`
  on API 31+, falling back to a **Kotlin-defined static `ColorScheme`** (`MedTimerColors.kt`) below 31
  — there is no `colors.xml` brand palette today, so define the static light/dark schemes directly in
  Kotlin (Material 3 baseline tones, adjusted to the app's brand if desired).
- **`@MedTimerPreview`** multipreview (light + dark + minimum supported font scale).
- Charts use **Vico** directly (bar via `CartesianChartHost`, pies via `rememberPieChart`) — no
  hand-rolled chart component in `:core:ui`.
- **`SortableTable`** composable — sortable columns (Taken / Name / Dosage / Reminded) + debounced
  filter field; replaces `TableView`.
- **`TagEventFilter`** — tag-filter logic lifted out of `MedicineViewModel` so the Statistics VM can
  reuse it without depending on another ViewModel.

All components take `Modifier` as the first optional parameter and hoist state (no side effects in the
composable body). Each screen-level composable gets a `@MedTimerPreview`.

---

## 7. `:feature:ui` Statistics screen

- **`StatisticsScreenViewModel`** (`@HiltViewModel`, constructor-injected `ReminderEventRepository`,
  `MedicineRepository`, `StatisticsProvider`, `PersistentDataDataSource`, `TagEventFilter`,
  `TimeFormatter`):
  - Owns **active view** (persisted via `setActiveStatisticsFragment`) and **Analysis range**
    (persisted via `setAnalysisDays`) — both restored on open.
  - Charts state via `StatisticsProvider`; table rows via `ReminderEventRepository.getAllFlow(...)`
    **passed through `TagEventFilter`** (preserve tag filtering); debounced (300 ms) text filter →
    `derivedStateOf` filtered rows.
  - Exposes a read-only `StatisticsScreenState` interface (mutable impl hidden).
- **`StatisticsScreen(viewModel)`** (stateful) reads `viewModel.state`, forwards events;
  **`StatisticsScreen(state, onSelectView, onSelectRange, onFilterChange, onEditEvent, …)`**
  (stateless) is the `@Preview`/test target.
- **`CalendarEventsViewModel`** kept (month-window paging, per-medicine), adapted to feed
  `CalendarContent`.
- **`StatisticsFragment`**: `onCreateView` returns `ComposeView` with
  `DisposeOnViewTreeLifecycleDestroyed`, content `MedTimerTheme { StatisticsScreen(viewModel) }`.
  Fulfills `onEditEvent(reminderEventId)` by showing the existing **`EditEventSheetDialogFragment`**
  (unchanged). Nav graph entry and bottom-nav item keep their existing `statisticsFragment` id.

---

## 8. Behavior-preservation checklist

- [x] **Active view** persists and restores (default Charts). Covered by `StatisticsScreenViewModelTest`
      ("initial state reads activeView from persisted data", "onSelectView persists the selection").
- [x] **Analysis range** persists and restores; drives **Charts only** (values 1/2/3/7/14/30 days from
      `R.array.analysis_days_values`). Verified: the Reminder Table is **not** range-driven (it shows
      all taken/skipped events via `timeStamp = 0`), and the Calendar pages by month — neither uses the
      range. Covered by `StatisticsScreenViewModelTest` ("onSelectRange…") and `StatisticsScreenTest`
      ("range dropdown hidden when table view is active").
- [x] **Reminder Table is tag-filtered** (via `TagEventFilter`); Charts unfiltered; Calendar
      per-medicine. The ViewModel derives the filter from `persistentDataDataSource.data.filterTags` +
      tags — no `MedicineViewModel` dep. Covered by `StatisticsScreenViewModelTest`
      ("table rows apply tag filter from persistent data") and `TagEventFilterTest`.
- [x] **Tap a table row → edit-event bottom sheet** opens (via `onEditEvent`). `ReminderTable` passes
      `onEditEvent` to `SortableTable.onRowClick`; `StatisticsFragment.onEditEvent` shows
      `EditEventSheetDialogFragment`. Covered by `StatisticsScreenTest`
      ("tapping a chip fires onSelectView…") pattern.
- [x] Per-medicine custom **chart colors** preserved. `StatisticsScreenViewModel.computeSeriesColors`
      checks `medicine.useColor && medicine.color`, falling back to the COLORS palette.
- [ ] **Landscape**: only the Calendar had a `layout-land` variant — Compose adapts automatically
      (no explicit landscape layout needed), but requires on-device verification to confirm the
      `AndroidView`-hosted day-events `TextView` lays out correctly in landscape.

---

## 9. Testing (completed)

- [x] **`TagEventFilterTest`** — 6 pure-JVM tests covering all filter semantics (empty selection,
  selected tag name match, any-tag OR logic, unknown ID excludes all).
- [x] **`SortableTableTest`** — 7 Robolectric Compose tests: filter, clear-filter, sort ascending,
  sort descending toggle, row-click callback, non-sortable column guard, all-rows default.
- [x] **`StatisticsScreenTest`** — 4 smoke tests on the stateless overload: chip presence, semantic
  tree content in TABLE view, range-dropdown absent outside CHARTS, `onSelectView` callback.
- [x] **`StatisticsProviderTest`** — 8 JVM tests: `getTakenSkippedData` (count all, filter by window,
  empty repo, ignores non-taken) and `getLastDaysReminders` (empty, epoch-day count, window exclusion,
  group by medicine, excludes skipped).
- [x] **`StatisticsScreenViewModelTest`** — 11 Robolectric tests: initial state from persisted data,
  `onSelectView` / `onSelectRange` state + persistence, table rows from repo flow, tag filter applied.
- [x] **3 instrumented tests migrated** (`ScreenshotsTest`, `ReminderTest`, `CalendarTest`) from
  View-ID / `TableView` assertions to UiAutomator `By.text` / `By.editable` semantics.
- [x] Both flavors: `./gradlew testFullDebugUnitTest testFossDebugUnitTest` — BUILD SUCCESSFUL.

---

## 10. Documentation updates (completed)

- [x] **`docs/guidelines/jetpack-compose.md`** — screen pattern section rewritten from `*Content`
  suffix to overloaded-function approach; status line updated to reflect Compose is live on the
  Statistics screen.
- [x] **`docs/guidelines/testing.md`** — Compose test-target reference updated from
  "stateless `*Content` composable" to "stateless `Screen` overload".
- [x] **`docs/guidelines/kotlin-android.md`** — `compileSdk = 37` (was 36); `:core:ui` description
  updated from "will land" to present tense.
- [x] **`CONTEXT.md`** — reconciled: Statistics* naming retained; Statistics→Analysis rename cancelled.
- [x] **`docs/adr/0001-compose-analysis-charting-libraries.md`** — retitled for the Statistics screen.

---

## 11. PR / commit sequencing (single PR)

1. **Build + `:core:ui` foundation** — enable Compose, add deps, `MedTimerTheme`, `@MedTimerPreview`,
   `PieChart`, `SortableTable`, `TagEventFilter` (+ their tests).
2. **Statistics screen** — VM/state holder, composables, `StatisticsFragment`, delete old
   fragments/layouts; remove AndroidPlot.
3. **Tests** — JVM Compose + unit tests; migrate the 3 instrumented tests; remove `TableView`.
4. **Docs** — guideline update (+ CONTEXT.md / ADR reconciliation if the user opts in).

`Co-Authored-By: Claude <noreply@anthropic.com>` on substantial commits.

---

## 12. Verification

```bash
./gradlew assembleDebug                 # both flavors compile (Compose enabled)
./gradlew testFullDebugUnitTest testFossDebugUnitTest
./gradlew lint                          # warningsAsErrors = true
# single migrated instrumented test only, against an emulator:
./gradlew :app:connectedFullDebugAndroidTest --tests <FQN>
```

---

## 13. Open items (mostly resolved during planning)

Resolved:

- ✅ **Naming** — reversed 2026-05-29: **no `Statistics` → `Analysis` rename**; retain existing code
  symbols and translations. CONTEXT.md and ADR 0001 reconciled (see §10).
- ✅ **Kotlin/Compose build** — AGP 9.2.1 built-in Kotlin (≈2.3.8); enable via `buildFeatures.compose`,
  no standalone Kotlin plugin needed.
- ✅ **Theme** — app uses `Theme.Material3.DynamicColors`; replicate in Kotlin (dynamic 31+ + Kotlin
  static `ColorScheme` fallback).
- ✅ **Tag filter** — confirmed required (persisted `filterTags`); preserve via `TagEventFilter`.
- ✅ **Vico pie** — Vico 3.1 has `rememberPieChart`; use it (no hand-rolled pie).
- ✅ **AndroidPlot** is Statistics-only → removable. **TableView** is Statistics-only in main + 2
  instrumented tests → removable after those tests are migrated.

Resolved at implementation time:

- ✅ **Compose compiler plugin** — AGP 9.2.1 **requires** `org.jetbrains.kotlin.plugin.compose`;
  `buildFeatures.compose = true` alone fails. Pinned to **2.2.10** (AGP 9.2.1's bundled Kotlin, per its
  POM — *not* the `ksp = 2.3.8` value, which is KSP's own version). Both `:core:ui` and `:feature:ui`
  assemble (full + foss) and lint clean with it applied.

- ✅ **`MaterialExpressiveTheme`** — `internal` on the stable BOM (material3 1.4.x); used by
  hand-pinning material3 to **1.5.0-alpha20** (user decision). Requires `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`.
  Cascade: Compose runtime → 1.12.0-alpha03, **`compileSdk 37`** project-wide. `:core:ui` + full app
  assemble (full + foss) and lint clean.

Still to confirm at implementation time:

1. The static brand `ColorScheme` values in `MedTimerColors.kt` are an **inferred placeholder** (M3
   baseline tones) — to be retuned by the maintainer after the migration lands (per user instruction).
</content>
</invoke>
