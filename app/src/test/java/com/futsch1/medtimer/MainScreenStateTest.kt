package com.futsch1.medtimer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainScreenStateTest {

    @Test
    fun `battery warning shows when not exempt, not dismissed and not suppressed`() {
        assertTrue(
            showBatteryOptimizationWarning(
                warningsSuppressed = false,
                isIgnoringBatteryOptimizations = false,
                batteryWarningShown = false,
            )
        )
    }

    @Test
    fun `battery warning is hidden once the exemption is granted`() {
        assertFalse(
            showBatteryOptimizationWarning(
                warningsSuppressed = false,
                isIgnoringBatteryOptimizations = true,
                batteryWarningShown = false,
            )
        )
    }

    @Test
    fun `battery warning is hidden once dismissed`() {
        assertFalse(
            showBatteryOptimizationWarning(
                warningsSuppressed = false,
                isIgnoringBatteryOptimizations = false,
                batteryWarningShown = true,
            )
        )
    }

    @Test
    fun `battery warning is hidden when suppressed`() {
        assertFalse(
            showBatteryOptimizationWarning(
                warningsSuppressed = true,
                isIgnoringBatteryOptimizations = false,
                batteryWarningShown = false,
            )
        )
    }

    @Test
    fun `exact reminders warning shows when disabled, not dismissed and not suppressed`() {
        assertTrue(
            showExactRemindersWarning(
                warningsSuppressed = false,
                exactReminders = false,
                exactRemindersWarningShown = false,
            )
        )
    }

    @Test
    fun `exact reminders warning is hidden once exact reminders are enabled`() {
        assertFalse(
            showExactRemindersWarning(
                warningsSuppressed = false,
                exactReminders = true,
                exactRemindersWarningShown = false,
            )
        )
    }

    @Test
    fun `exact reminders warning is hidden once dismissed`() {
        assertFalse(
            showExactRemindersWarning(
                warningsSuppressed = false,
                exactReminders = false,
                exactRemindersWarningShown = true,
            )
        )
    }

    @Test
    fun `exact reminders warning is hidden when suppressed`() {
        assertFalse(
            showExactRemindersWarning(
                warningsSuppressed = true,
                exactReminders = false,
                exactRemindersWarningShown = false,
            )
        )
    }
}
