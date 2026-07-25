package com.futsch1.medtimer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** See `docs/guidelines/jetpack-compose.md` §State holders. [MainViewModel] is the only writer. */
interface MainScreenState {
    val showBatteryOptimizationWarning: Boolean
    val showExactRemindersWarning: Boolean
}

class MutableMainScreenState : MainScreenState {
    override var showBatteryOptimizationWarning by mutableStateOf(false)
    override var showExactRemindersWarning by mutableStateOf(false)
}

internal fun showBatteryOptimizationWarning(
    warningsSuppressed: Boolean,
    isIgnoringBatteryOptimizations: Boolean,
    batteryWarningShown: Boolean,
): Boolean = !warningsSuppressed && !batteryWarningShown && !isIgnoringBatteryOptimizations

internal fun showExactRemindersWarning(
    warningsSuppressed: Boolean,
    exactReminders: Boolean,
    exactRemindersWarningShown: Boolean,
): Boolean = !warningsSuppressed && !exactRemindersWarningShown && !exactReminders
