# Stock Usability — Implementation Plan

**Date:** 2026-10-20  
**Status:** Draft — blocked on the Edit Medicine screen's Compose migration  
**Issue:** [#1776](https://github.com/Futsch1/medTimer/issues/1776)  
**Decision:** [ADR 0006 — Treat stock as a dose-equivalent quantity](../../adr/0006-dose-equivalent-stock.md)

## Goal

Make stock setup and stock changes understandable without adding medicine-strength conversion or physical package tracking. A medicine's stock is a dose-equivalent balance: the primary numeric amount recorded for a dose is subtracted directly from stock in the same unit.

## Scope

This plan covers stock-specific usability only. Overview filters, tags, general medicine icons, and other feedback in issue #1776 are deferred. The stock shortcut's icon/menu treatment is also deferred until after the Compose migration.

## Preconditions

1. Complete the Compose migration of the **Edit Medicine screen** before starting this plan. Revisit the stock shortcut's icon/menu treatment after that migration.
2. Merge [PR #1796](https://github.com/Futsch1/medTimer/pull/1796) before adding the stock-handling help page. This dependency applies to the help page, not the other stock work.

## Agreed behavior

- Use one shared, locale-aware amount parser for stock arithmetic and the Level Estimate's dose reading. The first parsed number is the primary dose amount; additional explanatory amounts do not change the stock decrement. For example, `1 capsule (25 mg)` subtracts `1` capsule.
- Keep the parser's numeric result visible while editing any dose that can affect stock. Show the effect, for example, **“Stock will decrease by 1 capsule.”** If the unit is unknown or conflicting, show the numeric effect without a unit. Keep invalid input visibly identified.
- Infer the stock unit from **active, dose-taking reminder configurations only**. Do not infer it from recorded dose history. Exclude inactive reminders and stock/expiration alerts.
- Trim unit text and compare case-insensitively; otherwise require exact text. Do not guess unit equivalences or convert. Thus `mg` and `MG` match, while `capsule` and `capsules` differ.
- A missing unit is unspecified. It conflicts with a named unit unless the retained legacy unit supplies the fallback for that unitless dose. All-unitless reminders remain unitless.
- Show a warning panel above reminders in the Compose Edit Medicine screen when active dose reminders disagree. The warning is advisory: reminder scheduling, dose recording, stock arithmetic, and forecasts continue. While units conflict, show stock balances without a unit and explain that the figures may be unreliable.
- Remove the separate stock-unit editor, but retain the saved `Medicine.unit` value as a fallback for unitless doses and historical data. Do not compare it with explicit reminder units and do not remove its database column in this plan. Configured reminders determine the unit when they provide one; the help page explains this transition.
- Preserve the Level Estimate's historical-data role: it can continue checking recorded doses for its own estimate, but recorded doses must not determine the stock unit.

## Implementation tasks

### 1. Consolidate amount parsing and make stock effects visible

- Extend or consolidate the standard amount parser so callers share the same primary-number interpretation, while retaining unit information needed by the Level Estimate.
- Ensure `1 capsule (25 mg)` resolves to amount `1` and unit `capsule` for stock effect and unit comparison; the parenthetical quantity is explanatory.
- Use the shared parsing behavior for all stock-affecting dose paths: reminder creation/editing, variable doses, manual doses, and reminder-event edits, as well as future-stock simulation.
- Add inline feedback for each corresponding editor/input showing the amount that will be removed from stock. Keep stock-disabled behavior unchanged.
- Add JVM tests for first-number parsing, locale-specific numbers, unit extraction, explanatory parenthetical amounts, invalid values, and Level Estimate compatibility.

### 2. Infer configured units and warn about conflicts

- Derive a medicine's effective unit from its active dose-taking reminder amounts. Apply the legacy-unit fallback only to unitless reminder amounts; ignore recorded-dose history for stock-unit inference.
- Treat disagreement between explicit units, or between an unspecified unit and a named unit, as a conflict. Apply the agreed trim/case-insensitive comparison without conversion or singular/plural guessing.
- Add a warning panel directly above the reminder list in the Compose Edit Medicine screen. Identify the conflicting units and explain that stock uses the first parsed number with no unit conversion.
- Keep the warning non-blocking. Continue all stock updates and forecasts; omit the unit from stock balances while a conflict exists.
- Test active versus inactive reminders, stock/expiration alerts, missing and legacy-fallback units, explicit unit conflicts, and case-only differences.

### 3. Clarify stock settings and stock displays

- Rename **Amount** to **Remaining stock** and **Refill size** to **Refill amount**. Add concise in-place explanations of what each quantity means.
- Remove the **Unit** editor from the UI. Retain the persisted legacy field; no Room schema change or migration is planned.
- Show the inferred unit alongside stock, refill, threshold, and related summaries when there is one unambiguous effective unit. Show no unit when units conflict or remain unspecified.
- Use the effective configured unit for new stock-change labels/snapshots; preserve units already stored on historical events.
- Rename **Create calendar entry** to **Add estimated run-out date to calendar**.
- Verify stock settings, refill behavior, out-of-stock thresholds/notifications, medicine-list stock text, reminder-event stock changes, and simulated run-out estimates all use the same unit decision and continue functioning during conflicts.

### 4. Add stock-handling help after PR #1796 merges

Document:

- Stock is a dose-equivalent balance, not a count of packages, tablets, or capsules unless the user chooses that as the dose unit.
- The first parsed number controls stock consumption; no conversion is done. Include `1 capsule (25 mg)` → stock decreases by one capsule.
- Active dose reminders should use a consistent unit. Explain the advisory warning and that conflicting-unit stock figures/forecasts may be unreliable while calculations continue.
- How remaining stock, refill amount, stock run-out estimates, and the calendar action relate.
- The retained legacy unit's limited fallback role for unitless doses.

## Acceptance criteria

- A user can see the numeric amount MedTimer will subtract before saving or confirming any stock-affecting dose.
- All stock-affecting flows use the same parsed primary number as the Level Estimate dose reader.
- Unit conflicts are detected from active configured dose reminders only, displayed in the medicine editor, and never block reminder or dose handling.
- Conflicting stock balances have no misleading unit label; stock operations and forecasts still run.
- The stock settings no longer ask users to set a separate unit, and the field/action labels explain their outcomes.
- No Room migration is introduced. The Compose migration and PR #1796 merge dependencies are respected.
- Update English strings and follow the project's translation workflow for all changed user-facing text.

## Explicitly deferred

- Compose migration itself (a precondition, not part of this plan).
- Choosing/replacing the stock shortcut icon or redesigning its option menu; revisit after the Compose migration.
- Removing the legacy `Medicine.unit` database field, comparing it to explicit reminder units, or migrating old stock values.
- Physical tablet/capsule/package inventory, strength modeling, dose conversion, and conversion of recorded-dose history.
- Non-stock usability changes from issue #1776.
