# Consolidate all simulation behind FutureRemindersRepository with a dynamic window

**Supersedes parts of ADR 0003.** ADR 0003 said widgets and `CalendarEventsViewModel` remain independent;
this ADR reverses both decisions and extends the repository with dynamic window management and
stock run-out tracking.

## Context

Three callers ran `SchedulingSimulator` directly outside `FutureRemindersRepository`:
`StockSettingsFragment` (2-year window per-medicine), `CalendarEventsProvider` (variable months),
and `NextRemindersLineProvider` (via `ReminderScheduler`). This produced redundant simulation work,
inconsistent data across screens, and no shared stock run-out signal.

The new requirement adds a stock run-out date per medicine (initially consumed by stock settings;
medicine-list display is deferred to the Compose migration). The run-out date requires tracking
when a medicine's remaining amount hits 0 during simulation — information that only the simulator
has. Exposing it from the shared repository is the natural fit.

## Decisions

### 1 — `stockRunOutDates: StateFlow<Map<Int, LocalDate?>>` added to `FutureRemindersRepository`

Computed inside `runSimulation` alongside the existing reminder list. For each medicine, the first
date where the simulated remaining amount reaches 0 is recorded. `null` means the stock does not
run out within the simulation window; the UI displays "After [simulatedThrough]" in that case.

### 2 — Dynamic window via `requestWindow` / `releaseWindow`

```kotlin
fun requestWindow(consumerId: String, days: Long)
fun releaseWindow(consumerId: String)
```

The repository holds a `Map<String, Long>` of active consumers. The effective window is the max
of all registered values, falling back to `DEFAULT_SIMULATION_DAYS` (28) when the map is empty.
`requestWindow` retriggers a simulation immediately if the new effective window is larger.
`releaseWindow` reduces the effective window; the smaller window takes effect on the next natural
trigger (alarm, DB change) — no immediate re-simulation on release.

The "never shrink within a session" invariant from ADR 0003 is removed. Window now tracks active
consumers rather than a session high-water mark.

### 3 — Consumer responsibilities

| Consumer                        | consumerId        | Window                                                                                                              | Lifecycle                                                     |
| ------------------------------- | ----------------- | ------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------- |
| `StockSettingsFragment`         | `"stockSettings"` | 365 days                                                                                                            | Fragment `onStart` / `onStop`                                 |
| `CalendarEventsViewModel`       | `"calendar"`      | `futureMonths × 31 + 28` days                                                                                       | ViewModel `init` / `onCleared`                                |
| `OverviewViewModel`             | `"overview"`      | `dayOffset + 28` days, only when offset ≥ 21                                                                        | updated on day selection; released when offset drops below 21 |
| Widget (`WidgetUpdateReceiver`) | —                 | No registration; reads existing result. Triggers `triggerCalculation()` only if `simulatedThrough == LocalDate.MIN` | —                                                             |

### 4 — `CalendarEventsProvider` migrated to the repository

`CalendarEventsProvider` no longer runs `SchedulingSimulator`. `addFutureEntries` is deleted.
`entriesFlow` drops the external `trigger` parameter; the provider internally combines
`FutureRemindersRepository.simulatedReminders` (future entries) with `reminderEventRepository`
past-events (past entries). The `simulatedReminders` flow drives reactivity for future entries
directly.

### 5 — Widget migrated to the repository

`NextRemindersLineProvider` replaces its `ReminderScheduler` call with
`futureRemindersRepository.simulatedReminders.value.take(N)`. Both produce a time-ordered list of
upcoming reminders; `take(N)` on the repository result is equivalent.

### 6 — `StockSettingsFragment` direct simulation removed

`estimateStockRunOutDate` and `calculateRunOutDate` are deleted. Stock settings observes
`stockRunOutDates[medicineId]` from the repository. Because amount edits write through to the DB
immediately, a DB change triggers re-simulation automatically — live-preview while editing is
preserved without any in-fragment simulation.

## Considered Options

**Keep per-screen simulations, add a new stock-only flow** — rejected. Perpetuates three independent
simulation paths and adds a fourth. Each path redoes the same expensive work independently.

**One-off 365-day session expansion (no release)** — rejected. A single stock-settings visit would
permanently lock the session window at 365 days, running expensive simulations on every reminder
interaction for the rest of the session.

**Separate "stock simulation" flow in the repository** — rejected. Two simulation passes over the
same data doubles the work; a single pass that records both scheduled reminders and run-out dates
is sufficient.

## Consequences

- `SchedulingSimulator` is only instantiated inside `FutureRemindersRepository`. All other
  callers are gone.
- `FutureRemindersRepository` is now the single source of truth for both future scheduled
  reminders and stock run-out dates.
- `CalendarEventsProvider` signature changes (no `trigger` param); callers that passed a trigger
  flow must be updated.
- `CalendarEventsProviderTest` needs updating; future-entry tests previously skipped simulation
  (`futureMonths = 0`) — they can now test future entries by injecting a fake repository.
- The overview week calendar's scrollable range (`DaySelector.rangeEndDay`) updates reactively
  as `simulatedThrough` grows, allowing users to page further into the future when a longer
  window is active.
