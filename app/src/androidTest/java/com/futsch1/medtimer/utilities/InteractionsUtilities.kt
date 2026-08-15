package com.futsch1.medtimer.utilities

import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import com.futsch1.medtimer.feature.reminders.ReminderProcessorBroadcastReceiver

/** Asks the app to schedule its reminders now, so a test does not have to wait for the real clock. */
fun scheduleRemindersNow(delayMillis: Long = 0, repeats: Int = 0) {
    ReminderProcessorBroadcastReceiver.requestScheduleNowForTests(
        InstrumentationRegistry.getInstrumentation().targetContext, delayMillis, repeats
    )
}

/**
 * Pulls the next [repeats] + 1 alarms the app schedules by itself forward to [delayMillis] from now.
 * The shortest repeat interval a user can pick is a minute, so a test that waits one out spends a
 * minute doing nothing; call this right before the action that schedules the repeat instead.
 */
fun fireNextAlarmsAfter(delayMillis: Long, repeats: Int = 0) {
    ReminderProcessorBroadcastReceiver.armNextAlarmsForTests(delayMillis, repeats)
}

/** Reminder events are keyed by their remind second, so one raised in the same second reuses the previous event. */
fun awaitNextSecond() {
    SystemClock.sleep(1000 - System.currentTimeMillis() % 1000)
}
