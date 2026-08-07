# Overview day selector scrolls freely into past and future

The overview week calendar was fixed to a 7-day window (5 days past, 1 day future). We extended it to scroll freely: 3 years into the past (practical limit — the app is ~2 years old) and forward without a hard cap.

## Past scrolling

Past events are already in the DB; widening the query is cheap. `OverviewViewModel` owns a `MutableStateFlow<Instant>` for the query start date. Default is `now - 6 days` (matching the original window). When the user scrolls past that boundary, it expands to `Instant.EPOCH` so all historical events are fetched. `flatMapLatest` re-issues the Room query when the start changes. `DaySelector.rangeStartDay` is fixed at `now - 3 years` — no dynamic expansion needed since the library pre-computes ~160 week entries for that range, which is trivial for RecyclerView.

## Future scrolling

Future reminders require simulation (see ADR-0001). We maintain a prefetch buffer: the simulation always covers `today + 28 days` (4 weeks). When the selected day reaches within 7 days of the simulation boundary, `OverviewViewModel` extends the boundary by 3 more weeks and calls `FutureRemindersRepository.triggerCalculation(newEndDay, immediate = true)`. The `immediate` flag bypasses the 1-second debounce that exists for DB-change-triggered recalculation; scroll-triggered prefetch needs to start immediately. `DaySelector.rangeEndDay` grows alongside the simulation boundary via a `StateFlow<LocalDate>` exposed from the VM.

## Scroll auto-selection

`WeekCalendarView.weekScrollListener` fires when the scroll settles on a new week. The listener auto-selects: today if the week contains today, otherwise the first day of the week. This keeps the overview always showing real data when scrolling, without requiring an explicit tap.

## Week start

`firstDayOfWeek` is pinned to `today - 3 days`, so today always renders in column 4 of 7 and yesterday and tomorrow are always adjacent to it, independent of
locale. Glimpsing at the neighbouring days is the dominant use of the selector, and it must not require a scroll.

The value is `remember`ed rather than recomputed. Reading `LocalDate.now()` on every composition — as the original View implementation did — shifted the whole
grid by a day when the date rolled over while the screen was open. Anchoring it once per composition lifetime keeps the grid still and lets today advance
through it instead.

The cost is that the seven visible days are not a calendar week in any locale, and the Statistics month calendar (which does use
`WeekFields.of(locale).firstDayOfWeek` via `rememberFirstDayOfWeek()`) starts its weeks on a different day. That inconsistency is accepted deliberately: the
month calendar is for reading a real calendar, the overview strip is for reaching nearby days.

## Considered options

**Constrain forward scrolling to 14 days** — rejected. Simulation cost is low enough that extending on demand is acceptable, and a hard cap would feel arbitrary.

**Dynamic past query (`now - selectedDay - buffer`)** — rejected. Re-creating the Flow on each scroll is complex and unnecessary; expanding to `Instant.EPOCH` only when the user first scrolls past the default window is simpler and queries use the timestamp index.

**`LocalDate.MIN` as calendar start** — rejected. The library pre-computes week entries for the full range; an unbounded past would exhaust memory.

**Keep today centered, but derive the offset from locale (e.g. locale's first day + 3)** — rejected. This still produces week boundaries divorced from any real
calendar week, just with different padding per locale; it inherits the same drift and confusion as the original hack, only harder to reason about.
