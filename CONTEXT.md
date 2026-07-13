# medTimer

medTimer is an Android medication-reminder app. This glossary fixes the language used across the
codebase and UI so code symbols, strings, and conversations stay aligned.

## Language

### Statistics feature

**Statistics**:
The screen where the user reviews their medication-adherence history. The feature's **code** keeps the
`Statistics*` naming — `StatisticsFragment`, `StatisticsProvider`, the `StatisticFragment` enum, the
`…feature.ui.statistics` package, and the `active_statistics_fragment` preference key. The **tab is
labeled "Analysis" in the UI** (the pre-existing `@string/analysis` string); this code-vs-UI split is
intentional and is **not** being unified.
_Note_: an earlier plan to rename `Statistic*` code symbols to `Analysis*` was **cancelled**
(2026-05-29) — do not rename these symbols or their translations.

**Statistics screen state** (`StatisticsUiState`):
The single immutable render-ready value the Statistics screen draws from. The ViewModel exposes one
read-only `uiState: StateFlow<StatisticsUiState>` — the two session selections combined with three
independently-derived view slices (Charts, Reminder Table rows, Calendar day events), each keeping its
own recompute trigger. The screen collects it once at the Compose edge (`collectAsStateWithLifecycle`),
so the flow-to-snapshot conversion isn't a ViewModel concern.

**Charts**:
One of the three Statistics views — pie charts (taken vs. skipped) plus a per-day stacked bar chart of
medicines. `StatisticsProvider` folds a single reminder-event read into the **Charts data** —
the per-day series plus the windowed and all-time taken/skipped tallies. The **Charts presenter**
(`ChartsPresenter`) then turns that data into the render-ready `ChartsState`: it formats per-day
labels and delegates per-series coloring to **Chart series colors** (`ChartSeriesColors`) — a pure
rule where a medicine's opted-in custom color wins, otherwise a fixed fallback palette cycles in
order. The ViewModel supplies the medicines' custom colors; both modules are pure and JVM-testable.

**Calendar events provider** (`CalendarEventsProvider`):
The single month-events traversal behind the Calendar. It loads events once and buckets past reminders
and scheduler-simulated future ones into a **Calendar entry** per day (`CalendarEntry`: a past
`ReminderEvent` or a future `ScheduledReminder`). Two renderers sit at that seam: the typed
`CalendarDayEvent` for the Compose calendar, and the icon-bearing `Spanned` text the legacy XML
`CalendarFragment` shows (built in `CalendarEventsViewModel`). The provider owns the calendar's
reactivity for both renderers via one flow-shaped seam (`entriesFlow`): it adapts a change trigger into
a stream of bucketed `CalendarEntry`s, re-reading the window on every emission. Each renderer maps that
stream to its own leaf — `structuredEventsFlow` to `CalendarDayEvent` for the Compose screen (collecting
the shared reminder-events flow as trigger), and `CalendarEventsViewModel` to `Spanned` for the legacy
XML calendar (a one-shot trigger). Neither caller fakes reactivity around a suspend read.

**Reminder Table**:
One of the three Statistics views — a sortable, filterable table of reminder events. The **Reminder
Table presenter** (`ReminderTablePresenter`) owns the row contract — the cell order (taken timestamp,
medicine name, amount, reminded timestamp) and the timestamp formatting — turning filtered reminder
events into render-ready rows. Pure and JVM-testable, mirroring the Charts presenter; the ViewModel
applies the tag filter before handing the events to it.
_Avoid_: Table (when ambiguous), grid.

**Calendar**:
One of the three Statistics views — a month calendar marking reminder events per day, with a
read-only detail panel for the selected day.

**Analysis range**:
The look-back window the Charts aggregate over. The actual options are **1 / 2 / 3 / 7 / 14 / 30 days**
(`R.array.analysis_days_values`). Persisted across sessions. Drives the **Charts only** — the Reminder
Table shows all taken/skipped events and the Calendar pages by month (neither is range-driven). The
range and the active view are the two **session selections** — each a `PersistedSelection` (seeded from
persistence, written through to it on change, skipping unchanged values) — so the persist rule lives in
one module rather than per handler. The dropdown reads the range through the screen's single derived
state, so the displayed range and the aggregated range cannot drift apart.
_Avoid_: days, time span (in user-facing text "Analysis" owns this; the underlying setting key is
`analysisDays`).

### Reminder events

**Reminder Event**:
A record that a scheduled reminder reached a terminal state for a given medicine at a given time.

**Taken / Skipped**:
The two Reminder Event states the Analysis feature aggregates and visualizes.
_Avoid_: done/dismissed, completed/missed.

**Tag filter**:
A global filter on medicines by tag. The Reminder Table respects it; Charts do not; the Calendar
filters by a single selected medicine instead.

### Stock and simulation

**Stock run-out date**:
The predicted date a medicine's remaining stock reaches zero, derived from the scheduling simulation.
Exposed as `SimulatedRemindersRepository.stockRunOutDates: StateFlow<Map<Int, LocalDate?>>`. A `null`
value means the stock does not run out within the active simulation window; the UI renders this as
"After [simulatedThrough date]". Medicine-list display is deferred to the Compose migration; stock
settings consumes this signal today.
_Avoid_: expiry date (that is the physical expiration, a separate field), depletion date.

**Simulation window**:
The time horizon, in days from today, that `SimulatedRemindersRepository` simulates into the future.
Dynamic: each UI consumer registers its required number of days via `requestWindow(consumerId, days)`;
the repository uses the maximum of all active registrations, falling back to `DEFAULT_SIMULATION_DAYS`
(28) when no consumer is registered. A consumer releases its claim via `releaseWindow(consumerId)`.
The window grows immediately on a new maximum; it shrinks on the next natural trigger (alarm or DB
change), not immediately on release.
_Avoid_: forecast horizon, lookahead period.

## Flagged ambiguities

- **Statistics vs. Analysis** — resolved (2026-05-29): code keeps the `Statistics*` naming; the UI
  tab keeps its existing "Analysis" label (`@string/analysis`). The earlier Statistics→Analysis code
  rename was **cancelled**, so no Kotlin symbols, SharedPreferences keys, or translations change. The
  two terms coexist by design: "Statistics" in code, "Analysis" on the tab.

## Example dialogue

> **Dev:** When the user opens Analysis, which view shows first?
> **Expert:** Charts by default, unless they last left it on the Reminder Table or Calendar — the
> active view is remembered.
> **Dev:** And the 30-day setting — does that affect the Calendar too?
> **Expert:** No. The Analysis range drives the **Charts only**. The Reminder Table shows all
> taken/skipped events, and the Calendar pages by month and filters by a chosen medicine — neither is
> driven by the range or the tag filter.
> **Dev:** So a tag filter set elsewhere only changes the Reminder Table?
> **Expert:** Right — Charts ignore tags, the Calendar is per-medicine, only the Reminder Table is
> tag-filtered.
