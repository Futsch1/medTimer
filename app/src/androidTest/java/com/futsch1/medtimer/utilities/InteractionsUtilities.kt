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

/** Reminder events are keyed by their remind second, so one raised in the same second reuses the previous event. */
fun awaitNextSecond() {
    SystemClock.sleep(1000 - System.currentTimeMillis() % 1000)
}
