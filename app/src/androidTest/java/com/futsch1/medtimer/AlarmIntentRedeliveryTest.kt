package com.futsch1.medtimer

import android.app.Activity
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.feature.reminders.alarm.ReminderAlarmActivity
import com.futsch1.medtimer.feature.reminders.api.notificationData.ReminderNotificationData
import com.futsch1.medtimer.harness.RepositoryEntryPoint
import com.futsch1.medtimer.utilities.pollUntil
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import kotlin.test.assertTrue

/**
 * Regression test for https://github.com/Futsch1/medTimer/issues/1494
 * ("Alarm can display previous events instead of current ones").
 *
 * The alarm activity is launched with [Intent.FLAG_ACTIVITY_SINGLE_TOP]
 * (ReminderAlarmActivity.getIntent). When a second alarm intent arrives while the
 * activity is already on top, Android delivers it via onNewIntent. Without an
 * onNewIntent override the new data is dropped and the screen keeps showing the
 * PREVIOUS alarm's events.
 *
 * This test drives the exact redelivery path deterministically: launch with the
 * first alarm's data, deliver the second alarm's intent via onNewIntent, and assert
 * the screen now shows the second alarm's medicine.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AlarmIntentRedeliveryTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun inject() {
        hiltRule.inject()
    }

    private val targetContext get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val entryPoint get() = EntryPointAccessors.fromApplication(
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext,
        RepositoryEntryPoint::class.java
    )

    @Test
    fun secondAlarmIntentReplacesDisplayedEvents() {
        // Two medicines with one reminder and one raised event each, seeded directly.
        val medAId = runBlocking { entryPoint.medicineRepository().create(Medicine.default().copy(name = "Meds A")) }
        val reminderAId = runBlocking { entryPoint.reminderRepository().create(Reminder.default().copy(medicineRelId = medAId)) }
        val eventA = runBlocking {
            entryPoint.reminderEventRepository().create(
                ReminderEvent.default().copy(reminderId = reminderAId, medicineName = "Meds A")
            )
        }
        val medBId = runBlocking { entryPoint.medicineRepository().create(Medicine.default().copy(name = "Meds B")) }
        val reminderBId = runBlocking { entryPoint.reminderRepository().create(Reminder.default().copy(medicineRelId = medBId)) }
        val eventB = runBlocking {
            entryPoint.reminderEventRepository().create(
                ReminderEvent.default().copy(reminderId = reminderBId, medicineName = "Meds B")
            )
        }

        val dataA = ReminderNotificationData.fromArrays(listOf(reminderAId), listOf(eventA.reminderEventId), Instant.now(), -1)
        val dataB = ReminderNotificationData.fromArrays(listOf(reminderBId), listOf(eventB.reminderEventId), Instant.now(), -1)

        val scenario = androidx.test.core.app.ActivityScenario.launch<ReminderAlarmActivity>(
            ReminderAlarmActivity.getIntent(targetContext, dataA)
        )

        try {
            // First alarm shows its own data.
            assertTrue(
                pollUntil(10_000) { notificationTitleText()?.contains("Meds A") == true },
                "Alarm screen should show the first alarm's medicine (Meds A), got: ${notificationTitleText()}"
            )

            // A second alarm intent arrives while the activity is on top (singleTop redelivery).
            // Instrumentation.callActivityOnNewIntent is the exact entry point the framework
            // uses (ActivityThread -> performNewIntent -> onNewIntent), so this exercises the
            // real delivery path instead of a reflection shortcut.
            scenario.onActivity { activity ->
                InstrumentationRegistry.getInstrumentation()
                    .callActivityOnNewIntent(activity, ReminderAlarmActivity.getIntent(targetContext, dataB))
            }

            // The screen must now show the SECOND alarm's medicine, not the previous one.
            assertTrue(
                pollUntil(10_000) { notificationTitleText()?.contains("Meds B") == true },
                "Alarm screen should show the second alarm's medicine (Meds B), got: ${notificationTitleText()}"
            )
        } finally {
            scenario.close()
        }
    }

    private fun notificationTitleText(): String? {
        var text: String? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            text = (ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .firstOrNull { it is ReminderAlarmActivity } as Activity?)
                ?.findViewById<TextView>(com.futsch1.medtimer.feature.reminders.R.id.notificationTitle)
                ?.text
                ?.toString()
        }
        return text
    }
}
