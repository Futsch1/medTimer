package com.futsch1.medtimer

import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.utilities.scheduleRemindersNow
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

/**
 * Higher-level verification for https://github.com/Futsch1/medTimer/issues/1418
 * ("Battery dead = missing medications").
 *
 * The #1418 scenario: the device is off (battery dead) when an automatically-taken
 * reminder falls due. On the next app reschedule/start, the missed dose must be
 * caught up and recorded as TAKEN - not left pending and not raised for a manual action.
 *
 * End-to-end on the device (frozen-clock harness): an automatically-taken reminder whose
 * time is ALREADY BEHIND now ([MedTimerTestBase.earlierToday]) is processed via
 * [scheduleRemindersNow]; the overview must show it as taken and no "taken" action may
 * remain in the notification shade. This mirrors the maintainer's `automaticallyTakenTest`
 * but with a PAST reminder time - the missed-while-off case - instead of a future one.
 *
 * Note: unlike the future-time case, a past-time reminder has NO pre-existing overview event
 * (nothing was processed yet), so the "please wait" intermediate state is intentionally not
 * asserted - only the post-catch-up "taken" state matters for the #1418 scenario.
 */
@HiltAndroidTest
class MissedAutomaticallyTakenInstrumentedTest : MedTimerTestBase() {

    @Test
    @AllowFlaky(attempts = 3)
    fun missedAutomaticallyTakenReminderIsRecordedAsTaken() {
        medicines.create(TEST_MED)

        medicineEditor.addReminder("1", earlierToday())
        reminders.inSettingsOf(0) { toggleAutomaticallyTaken() }

        navigation.toOverview()

        scheduleRemindersNow()
        overview.assertEventState(0, R.string.taken)

        notifications.inShade { assertHidden(actionLabel(R.string.taken)) }
    }
}
