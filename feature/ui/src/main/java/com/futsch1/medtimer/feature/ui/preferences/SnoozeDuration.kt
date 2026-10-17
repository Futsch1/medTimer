package com.futsch1.medtimer.feature.ui.preferences

/** Snooze duration list entry that asks for a duration in minutes and stores that instead of itself. */
internal const val SET_SNOOZE_DURATION_VALUE = "-2"

/** Minutes entered for a fixed snooze duration, or null unless the input is a positive whole number. */
internal fun parseSnoozeMinutes(input: String?): Int? = input?.trim()?.toIntOrNull()?.takeIf { it > 0 }

/**
 * Summary of the snooze duration setting: the matching list entry, or the minutes for a duration
 * the user entered, which has no entry of its own.
 */
internal fun snoozeDurationSummary(
    value: String?,
    entries: Array<CharSequence>,
    entryValues: Array<CharSequence>,
    minutesLabel: (Int) -> CharSequence,
): CharSequence? {
    val index = entryValues.indexOfFirst { it.toString() == value }
    if (index >= 0) {
        return entries[index]
    }
    return value?.toIntOrNull()?.let(minutesLabel)
}
