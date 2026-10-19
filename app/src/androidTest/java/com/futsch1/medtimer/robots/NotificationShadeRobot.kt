package com.futsch1.medtimer.robots

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import androidx.annotation.StringRes
import androidx.test.ext.truth.app.NotificationActionSubject.notificationActions
import androidx.test.ext.truth.app.NotificationSubject.notifications
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.futsch1.medtimer.utilities.closeNotification
import com.google.common.truth.Truth.assertWithMessage
import java.util.regex.Pattern
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The system notification shade. Everything here goes through UiAutomator: the shade is another
 * app's window, so neither Espresso nor Compose can see or synchronize with it.
 */
class NotificationShadeRobot {

    private val device: UiDevice get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private val appPackage: String get() = InstrumentationRegistry.getInstrumentation().targetContext.packageName

    /** Opens the shade, runs [block] against it and closes it again. */
    fun <T> inShade(block: NotificationShadeRobot.() -> T): T = open().use { block() }

    private fun open(): AutoCloseable {
        device.openNotification()
        return AutoCloseable {
            device.closeNotification()
            device.waitForIdle(500)
        }
    }

    fun actionLabel(@StringRes textRes: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(textRes, *args)

    fun await(text: String, timeoutMillis: Long = DEFAULT_TIMEOUT): UiObject2? =
        device.wait(Until.findObject(By.textContains(text)), timeoutMillis)

    fun assertShows(text: String, timeoutMillis: Long = DEFAULT_TIMEOUT): UiObject2 =
        assertNotNull(await(text, timeoutMillis), "No notification containing '$text'. Shade shows: ${shadeTexts()}")

    fun assertHidden(text: String, timeoutMillis: Long = DEFAULT_TIMEOUT) {
        assertNull(await(text, timeoutMillis), "A notification contains '$text' but should not")
    }

    /**
     * Waits for a posted notification containing [text] (and [title], if given). Asks the posted notifications,
     * so it works without the shade being open.
     */
    fun assertPosted(text: String, title: String? = null, timeoutMillis: Long = DEFAULT_TIMEOUT) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        var match = findPosted(text, title)
        while (match == null && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(POLL_INTERVAL)
            match = findPosted(text, title)
        }
        val notification = assertNotNull(match, "No posted notification contains '$text'${titleClause(title)}. Posted: ${postedDescriptions()}").notification
        if (title != null) {
            assertWithMessage("Posted: ${postedDescriptions()}").about(notifications()).that(notification)
                .extras().string(Notification.EXTRA_TITLE).contains(title)
        }
    }

    /** Watches the posted notifications for the whole [timeoutMillis]; a match at any point fails. */
    fun assertNotPosted(text: String, title: String? = null, timeoutMillis: Long = DEFAULT_TIMEOUT) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        do {
            assertNull(findPosted(text, title), "A posted notification contains '$text'${titleClause(title)} but should not. Posted: ${postedDescriptions()}")
            SystemClock.sleep(POLL_INTERVAL)
        } while (SystemClock.uptimeMillis() < deadline)
    }

    /** Watches the posted notifications for the whole [timeoutMillis]; any action labelled [textRes] fails. */
    fun assertNoPostedAction(@StringRes textRes: Int, timeoutMillis: Long = DEFAULT_TIMEOUT) {
        val label = actionLabel(textRes)
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        do {
            activeNotifications().flatMap { it.notification.actions.orEmpty().toList() }.forEach {
                assertWithMessage("Action '$label' is posted but should not be. Posted: ${postedDescriptions()}")
                    .about(notificationActions()).that(it).title().isNotEqualTo(label)
            }
            SystemClock.sleep(POLL_INTERVAL)
        } while (SystemClock.uptimeMillis() < deadline)
    }

    private fun findPosted(text: String, title: String?): StatusBarNotification? =
        activeNotifications().firstOrNull { posted ->
            posted.notification.textsOf().any { it.contains(text) } &&
                    (title == null || posted.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.contains(title) == true)
        }

    private fun titleClause(title: String?) = title?.let { " with title '$it'" }.orEmpty()

    /**
     * Waits for the notification containing [text] to be withdrawn. The app stores processed events before it
     * withdraws their notification, so it can still be posted for a moment. Asks the posted notifications, not
     * the shade, which can drop a notification for a moment while it is updated.
     */
    fun assertRemoved(text: String, timeoutMillis: Long = DEFAULT_TIMEOUT) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (postedIdOrNull(text) != null && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(POLL_INTERVAL)
        }
        assertNull(postedIdOrNull(text), "Notification containing '$text' was not withdrawn. Posted: ${postedDescriptions()}")
    }

    /** The shade is not inspectable after the fact - failures have to carry its contents themselves. */
    private fun shadeTexts(): List<String> =
        device.findObjects(By.pkg("com.android.systemui")).mapNotNull {
            // Expanding a notification re-renders its views, so some of them are already gone.
            runCatching { it.text }.getOrNull()
        }.filter { it.isNotBlank() }

    /** The id [text] is posted under, for [awaitRaisedAgain]. Take it before anything can raise it again. */
    fun postedId(text: String): Int =
        assertNotNull(postedIdOrNull(text), "No posted notification contains '$text'. Posted: ${postedDescriptions()}")

    /**
     * Waits for [text], posted under [previousId], to be raised again - a repeat, a snooze coming back.
     * Only the new id says so: the shade's timestamp resolves to minutes, and the post time also moves
     * on a plain update.
     */
    fun awaitRaisedAgain(text: String, previousId: Int, timeoutMillis: Long = DEFAULT_TIMEOUT) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        var current = previousId
        while (current == previousId && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(POLL_INTERVAL)
            // Raising it again cancels the old one first, so a momentary absence is not an answer.
            current = postedIdOrNull(text) ?: current
        }
        assertTrue(
            current != previousId,
            "Notification containing '$text' was never raised again, still posted as id $previousId. " +
                    "Posted: ${postedDescriptions()}"
        )
    }

    private fun postedIdOrNull(text: String): Int? =
        activeNotifications().filter { it.notification.textsOf().any { line -> line.contains(text) } }
            .maxByOrNull { it.postTime }?.id

    private fun activeNotifications(): List<StatusBarNotification> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.activeNotifications.toList()
    }

    /** The posted notifications as the assertions see them, so a failure can explain itself. */
    private fun postedDescriptions(): List<String> =
        activeNotifications().map { "id=${it.id} postTime=${it.postTime} ${it.notification.textsOf()}" }

    private fun Notification.textsOf(): List<String> = listOfNotNull(
        extras.getCharSequence(Notification.EXTRA_TITLE),
        extras.getCharSequence(Notification.EXTRA_TEXT),
        extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
        extras.getCharSequence(Notification.EXTRA_SUB_TEXT),
    ).map { it.toString() } + (extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.map { it.toString() }
        ?: emptyList())

    fun assertShowsAction(content: String, @StringRes textRes: Int, vararg args: Any) {
        val label = actionLabel(textRes, *args)
        expandFor(content, label)
        assertNotNull(device.findObject(labelSelector(label)), "Notification action '$label' is missing. Shade shows: ${shadeTexts()}")
    }

    fun assertNoAction(@StringRes textRes: Int, vararg args: Any) {
        val label = actionLabel(textRes, *args)
        assertNull(device.findObject(labelSelector(label)), "Notification action '$label' is present but should not be")
    }

    /** Asserts on a big-notification button, which is a custom view identified by its id. */
    fun assertShowsButton(id: String) {
        assertTrue(device.wait(Until.hasObject(By.res(appPackage, id)), DEFAULT_TIMEOUT), "Notification button '$id' is missing")
    }

    fun assertNoButton(id: String) {
        assertNull(device.findObject(By.res(appPackage, id)), "Notification button '$id' is present but should not be")
    }

    fun clickAction(content: String, @StringRes textRes: Int, vararg args: Any) {
        val label = actionLabel(textRes, *args)
        assertTrue(clickActionIfPresent(content, label), "Notification action '$label' is missing. Shade shows: ${shadeTexts()}")
    }

    fun clickActionIfPresent(content: String, label: String): Boolean {
        val button = expandFor(content, label)
        button?.click()
        return button != null
    }

    /** Platform versions differ in the case they render action labels in. */
    private fun labelSelector(label: String): BySelector =
        By.text(Pattern.compile(Pattern.quote(label), Pattern.CASE_INSENSITIVE))

    /**
     * Collapsed notifications hide their actions, and which one is collapsed varies, so this expands
     * them one at a time until the wanted label shows up. When the expand button does not do it, the
     * notification containing [content] is pulled down instead.
     */
    fun expandFor(content: String, label: String): UiObject2? {
        var buttonIndex = 0
        var tries = 10
        var button = device.findObject(labelSelector(label))
        while (tries-- > 0 && button == null) {
            val expandButtons = device.findObjects(By.res("android:id/expand_button"))
            if (expandButtons.size > buttonIndex) {
                try {
                    expandButtons[buttonIndex].click()
                    buttonIndex++
                    device.waitForIdle(1_000)
                } catch (_: StaleObjectException) {
                    // Ignore
                }
            } else {
                try {
                    device.findObject(By.textContains(content))?.swipe(Direction.DOWN, 0.5f)
                    device.waitForIdle(1_000)
                } catch (_: StaleObjectException) {
                    // Ignore
                }
                Thread.sleep(200)
            }
            button = device.findObject(labelSelector(label))
        }
        return button
    }

    /** Taps the body of the notification containing [text], which opens the app. */
    fun open(text: String) {
        repeat(OPEN_ATTEMPTS) {
            try {
                assertShows(text).click()
                return
            } catch (_: StaleObjectException) {
                // The shade re-renders a notification that was just posted or updated; find it again
            }
        }
        assertShows(text).click()
    }

    fun dismiss(text: String) {
        val notification = await(text) ?: return
        try {
            notification.fling(Direction.RIGHT)
        } catch (_: StaleObjectException) {
            // Notification view was re-rendered (e.g. after expansion); re-find and fling
        }
        await(text, 500)?.fling(Direction.RIGHT)
    }

    fun awaitShade() {
        device.wait(Until.hasObject(By.pkg("com.android.systemui")), DEFAULT_TIMEOUT)
    }

    companion object {
        const val DEFAULT_TIMEOUT = 2_000L
        private const val POLL_INTERVAL = 100L
        private const val OPEN_ATTEMPTS = 5
    }
}
