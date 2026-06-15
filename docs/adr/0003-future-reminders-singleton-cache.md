# Future reminders computed in a singleton cache, not per-ViewModel

The overview and analysis calendar both need scheduled reminders for the next 14 days. Rather than running `SchedulingSimulator` independently in each ViewModel (as `CalendarEventsViewModel` does today), a `@Singleton FutureRemindersRepository` owns the simulation and exposes `StateFlow<List<ScheduledReminder>>`. This keeps the expensive computation in one place and ensures the overview always reflects the same data that was most recently calculated.

## Considered Options

**WorkManager** — rejected. WorkManager guarantees execution even after the app is killed, which is unnecessary here: the cache only serves active UI. The overhead (persistent DB, constraints, `WorkInfo`) is not justified.

**Per-ViewModel on-demand simulation** — rejected for the overview specifically. The overview reacts to live DB changes (medicines, reminder events), so a new simulation would run on every change with no debouncing. `CalendarEventsViewModel` keeps this pattern because its use case is pull-based (user scrolls to a month) and it supports ranges beyond 14 days.

**Pull-based Flow watch in the singleton** — rejected. Watching `MedicineRepository.getAllFlow()` directly would fire on every DB write, duplicating the logic already in `ReminderSchedulerService`. Push injection at the reschedule callsites is more precise.

## Consequences

- `SchedulingSimulator.simulate()` becomes `suspend` and adds `yield()` per simulated day so an in-progress simulation is cancelled promptly when a new trigger arrives.
- `NextReminders` (the class that previously pushed next-per-medicine results into `MedicineViewModel`) is deleted.
- `MedicineViewModel` subscribes to the singleton's `StateFlow` instead of receiving pushed results, preserving the existing tag-filter chain.
- Widgets (`NextRemindersLineProvider`) and `CalendarEventsViewModel` remain independent — they have different scopes and lifecycles that don't benefit from the shared cache.
