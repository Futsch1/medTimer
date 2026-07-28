package com.futsch1.medtimer.utilities

import androidx.test.platform.app.InstrumentationRegistry
import com.futsch1.medtimer.feature.reminders.ReminderProcessorBroadcastReceiver

/** Asks the app to schedule its reminders now, so a test does not have to wait for the real clock. */
fun scheduleRemindersNow(delayMillis: Long = 0, repeats: Int = 0) {
    ReminderProcessorBroadcastReceiver.requestScheduleNowForTests(
        InstrumentationRegistry.getInstrumentation().targetContext, delayMillis, repeats
    )
}
