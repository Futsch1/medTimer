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
`CalendarFragment` shows (built in `CalendarEventsViewModel`). The provider also exposes a flow-shaped
read (`structuredEventsFlow`) that adapts a change trigger — the screen's shared reminder-events flow —
into the reactive stream the Compose screen collects, so the provider owns its reactivity rather than
the ViewModel faking it around a one-shot suspend read.

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
Table shows all taken/skipped events and the Calendar pages by month (neither is range-driven). Held
as a single source of truth in the ViewModel (one `MutableStateFlow`); the screen-state value the
dropdown reads is a one-way projection of it, so the displayed range and the aggregated range cannot
drift apart.
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
