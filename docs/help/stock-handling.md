# Stock handling

## How dose amounts are read

Stock calculations and **Level estimate** use the first number in a dose amount, read with your phone's number format.
For example, `1 capsule (25 mg)` counts as `1`, with `capsule` as the unit; the `25 mg` does not change the quantity.
No unit conversion is performed. Use your phone's decimal separator: an unrecognized separator can cause only part of
a number to be read.

**Level estimate** still leaves out medicines whose recorded doses use different units. Doses with no readable number
are omitted from the estimate.
