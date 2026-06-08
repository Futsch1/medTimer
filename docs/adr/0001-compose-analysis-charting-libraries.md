# Charting and table libraries for the Compose Statistics screen

> Naming note (2026-05-29): the screen's code retains the `Statistics*` naming (the
> `Statistics`→`Analysis` rename was cancelled); its UI tab remains labeled "Analysis"
> (`@string/analysis`). See [`CONTEXT.md`](../../CONTEXT.md). This ADR's library decisions are
> unaffected by that naming choice.

When migrating the Statistics screen to Jetpack Compose we replaced the View-based charting and table
libraries with Compose-native equivalents instead of wrapping the existing libraries in `AndroidView`.

## Decision

- **Charts → Vico** (`com.patrykandpatrick.vico:compose` + `:compose-m3`, 3.1). Vico 3.1 provides both
  the stacked bar chart (`CartesianChartHost` + `ColumnCartesianLayer`) and a Compose pie chart
  (`rememberPieChart`), so the per-day bar chart and the two taken/skipped pies all come from one
  Compose-native library. Pure Kotlin, no GMS dependency (works for both `full` and `foss` flavors).
- **Reminder Table → hand-rolled `SortableTable` composable** in `:core:ui`, replacing
  `com.github.evrencoskun:TableView` (no maintained Compose-native equivalent that fits).
- **AndroidPlot dropped** entirely (it was used only by the Statistics screen).
- **Calendar** is *not* part of this decision — it stays on the Kizitonwose library, just switching
  from the `:view` artifact to the `:compose` artifact (the `:view` artifact remains for Overview).

## Considered and rejected

Wrapping the existing AndroidPlot and TableView views in `AndroidView`. Rejected because it strands
imperative View code (manual `update {}` blocks, adapters, binders) inside Compose, straddling two UI
paradigms on the pathfinder Compose screen and creating work that would be redone later. The trade-off
is up-front rewrite cost (notably the hand-rolled `SortableTable`) in exchange for idiomatic, reusable
Compose components.
