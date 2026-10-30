---
status: proposed
---

# Treat stock as a dose-equivalent quantity

Stock is a dose-equivalent balance, not a count of physical tablets, capsules, or packages: the primary numeric amount recorded for a dose is subtracted directly from stock in that same unit, with no conversions. The unit is inferred from active, dose-taking reminder configurations; conflicting units produce an advisory warning but do not stop stock updates or forecasts, and the displayed balance omits its unit while the conflict remains. We choose this simpler model over tracking physical forms and converting by medicine strength, which would require a formulation/strength model and additional rules for partial doses; a retained legacy unit is only a fallback for unitless doses, not a check against explicit reminder units.
