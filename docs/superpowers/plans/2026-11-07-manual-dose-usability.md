# Manual Dose Usability — Implementation Plan

**Date:** 2026-11-07  
**Status:** Agreed design; implementation not started

## Goal

Make the two different actions on the first page understandable while retaining one-tap logging of recent manual doses. Keep the three-stage guided flow and minimize taps. The [domain glossary](../../../CONTEXT.md) defines a Recent dose.

## Agreed behavior

### First page and Quick log

- Place **Quick log** above the three-step indicator. The indicator belongs to the guided path, followed by **Choose a medicine**. On later steps, keep the indicator at the top.
- Show Quick log only when the selected Overview day is today and there are usable Recent doses. It contains manual additional doses only, not doses from scheduled reminders.
- Each Recent row shows the medicine name, the amount if present, and an explicit **Log now** action. The whole row is tappable. A tap immediately logs the combination at the current time without another confirmation.
- Do not add an edit action for Recent entries. To change the amount or time, use the guided flow. Do not add special labels for linked versus name-only entries, even if two rows look alike.
- Keep at most three distinct combinations, ordered by most recently **logged**. Reusing a combination moves it to the top rather than duplicating it. Its identity includes the medicine name, linked medicine ID if any, and amount. Two doses with the same name and amount but different IDs remain distinct; a changed name can also identify a different combination.
- Ignore leading and trailing whitespace in names and amounts, including when comparing existing Recent entries and amount suggestions. Otherwise preserve exact text: case, internal whitespace and amount formatting remain significant. Do not rewrite historical dose records. Existing invalid blank-name shortcuts must not allow a new unnamed dose to be logged.
- No special handling is required for renamed medicines: retain the current linkage behavior even though a shortcut with the old name can still affect the linked medicine's stock.

### Guided flow

- Label the first section **Choose a medicine** and give medicine rows a forward affordance. Tapping a medicine immediately advances to Amount; do not add a Next button.
- Trim input. A medicine name is required after trimming. A blank custom name must not advance: keep the entry dialog open and display a clear validation error. Apply the rule to all selection paths, not just the custom field.
- Amount is optional. Offer **No amount** as an explicit one-tap choice alongside suggested and custom amounts. Trim custom amounts and deduplicate suggested amounts using their trimmed text. Do not show “No amount” in a dose summary or Recent row; omit the blank amount as the logged dose does.
- On Time, make the selected date prominent, including when it is not today. Retain the current-time action for other dates: it combines the selected date with the current clock time. Make both actions explicitly describe logging, for example **Log at current time** and **Log at another time**. Confirming the time picker logs without a separate confirmation.
- Completed steps remain tappable for going back; current and future steps are non-interactive indicators. System Back moves back one step on Amount and Time and closes the dialog on Medicine. Explicit Cancel still closes it from any step.

### Save feedback and Overview

- Both Quick log and the guided flow give the same feedback: show a success toast only after the dose is created, and scroll to the created event when it is visible on the currently selected day.
- Do not change the selected day or clear status/tag filters. If the event is hidden by filters, the user changed days, or scrolling is otherwise not possible, do not scroll. The normal success toast is sufficient; do not add a special hidden-by-filter message.
- If creating the event fails, show a failure toast. Keep failure handling simple; do not add a complex retry flow. Keeping the dialog open on failure is optional only if straightforward, not a requirement.

## Implementation tasks

1. **Recent data and validation.** Review `core/domain/src/main/java/com/futsch1/medtimer/core/domain/model/PersistentData.kt` and `core/datastore/src/main/java/com/futsch1/medtimer/core/datastore/PersistentDataDataSource.kt`. Implement trim-aware distinct combinations, newest-first reordering and the three-entry limit, including persisted legacy whitespace variants. Record a Recent entry only after successful event creation and avoid losing rapid updates through stale preference snapshots. Preserve the existing stored data format and historical events; do not add a Room migration. Review `feature/ui/src/main/java/com/futsch1/medtimer/feature/ui/overview/manualDose/ManualDoseViewModel.kt` for trimmed names/amounts, blank-name enforcement and trim-aware amount suggestions. Do not change the shared text-input helper globally just to fix this flow.
2. **Dialog.** Update `feature/ui/src/main/java/com/futsch1/medtimer/feature/ui/overview/manualDose/ManualDoseDialog.kt` and `ManualDoseState.kt`: separate the paths, make actions and navigation unambiguous, implement validation and the optional-amount choice, and maintain accessible labels and touch targets. Review the custom input dialog approach in `feature/ui/src/main/java/com/futsch1/medtimer/feature/ui/helpers/TextInputDialogBuilder.kt`; scope validation to manual dosing. Add/update strings using the project's localization conventions.
3. **Save and scroll.** Trace `ManualDoseViewModel`, `feature/ui/src/main/java/com/futsch1/medtimer/feature/ui/overview/OverviewFragment.kt`, `OverviewViewModel.kt`, `OverviewScreen.kt` and `OverviewEventList.kt`. Capture the selected dose and time at submission, then carry the newly created event's ID and selected day to Overview after insertion; handle list updates and save completion arriving in either order. Avoid duplicate inserts on repeated taps. Preserve stock handling and avoid reporting creation failure or retrying creation merely because a later stock update fails; creation and stock adjustment are not atomic, so account for partial failure without risking a duplicate dose or stock decrement. `OverviewViewModel.bringIntoView` currently clears filters and `OverviewDayRequests` changes days, so do not reuse those behaviors unchanged. Do not leave a permanently pending scroll target when the event cannot appear; cancel or discard stale requests as appropriate. The existing query window can omit early events on its six-day boundary; avoid an indefinitely pending target, but do not expand scope into unrelated query changes without need.
4. **Help.** Update `docs/help/review-events.md` to describe the guided time step, optional amount, today's immediate Recent shortcut, and relevant feedback. Correct its existing statement that amount presets appear in the medicine chooser. Follow `docs/guidelines/documentation-guidelines.md` and review affected screenshots and translations.

## Verification

- JVM tests: Recent order and three-entry limit; repeated combinations move to the front without duplicates; distinct names, IDs and amounts remain distinct; surrounding whitespace and legacy variants deduplicate; case/internal whitespace stay significant; persistence round-trip; preset deduplication; blank-name rejection and optional trimmed amount.
- Focused UI/instrumented tests: today versus another selected day; one-tap Quick log; guided selection and No amount; blank-name error; completed step navigation and system Back; explicit log actions and selected date; distinct entries and absent amounts displayed correctly. Strengthen `app/src/androidTest/java/com/futsch1/medtimer/ManualDoseTest.kt`: its existing Recent assertion does not independently prove another event was created.
- Test success and failure feedback, generated-event identity, stock handling, duplicate-tap prevention, asynchronous event/list ordering, scrolling to a visible event, hidden status/tag filters, changed day and absent/stale targets. Ensure no filters or date change; cover rapid preference updates and failure after event insertion. Review existing `feature/ui/src/test/java/com/futsch1/medtimer/feature/ui/overview/OverviewEventListScrollTest.kt` and `feature/ui/src/test/java/com/futsch1/medtimer/feature/ui/overview/manualDose/ManualDoseViewModelTest.kt`.
- Run focused JVM tests for affected modules, `./gradlew assembleDebug`, and `./gradlew lint` as feasible. For each added/changed instrumented test run only that test on an emulator; do not run the full connected suite locally. After Help changes, run `./gradlew :app:validateHelpGuide` following the build. Report any checks not run.

## Scope and handover

No implementation has been made. `CONTEXT.md` was updated during the design discussion. Do not add new dependencies or request a database migration for this redesign. Keep the existing three-stage flow, one-tap shortcut, and chosen simplicity; do not add shortcut-edit actions, per-row identity labels, hidden-filter notices, or special renamed-medicine handling.
