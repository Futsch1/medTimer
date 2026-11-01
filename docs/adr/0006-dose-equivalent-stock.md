---
status: accepted
---

# Treat stock as a dose-equivalent quantity

Stock is a dose-equivalent balance, not a count of physical tablets, capsules, or packages: the primary numeric amount recorded for a dose is subtracted directly from stock in that same unit, with no conversions. The effective unit is inferred only from active, dose-taking reminder configurations. Trimmed, case-insensitive matches and explicitly supported singular/plural forms are treated as the same unit; other unit spellings must match exactly. This bounded recognition list is not general language morphology and does not imply conversion or broader semantic equivalence.

A missing unit is unspecified. If active dose reminders mix unitless and named amounts, or use different units, show an advisory warning, omit the unit from stock balances, and continue stock updates and forecasts. Those figures may be unreliable while the conflict remains. Recorded dose history does not determine the stock unit.

Do not use the legacy `Medicine.unit` value as a fallback for unitless reminders or historical dose labels. Keeping it active would require an additional editable unit setting, which is intentionally avoided to keep stock setup simpler. The persisted database column remains untouched by this decision; no schema migration is included. Historical stock-change events retain the units already stored with them.

We choose this simpler dose-equivalent model over tracking physical forms and converting by medicine strength, which would require a formulation/strength model and additional rules for partial doses.
