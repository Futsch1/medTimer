package com.futsch1.medtimer.wear

/**
 * The wire protocol shared with the phone app's `feature/reminders` module. The two sides live in
 * separate Gradle modules (separate installable APKs), so these constants and [WatchReminderItem]
 * are duplicated on the phone side (`WearProtocol.kt` there) rather than shared via a dependency -
 * keep the two definitions in sync by hand.
 */
object WearProtocol {
    const val TODAY_DATA_PATH = "/medtimer/today"
    const val ACTION_MESSAGE_PATH = "/medtimer/action"
    const val TODAY_DATA_KEY = "items"

    const val ACTION_TAKEN = "TAKEN"
    const val ACTION_SKIPPED = "SKIPPED"
    const val ACTION_SNOOZE = "SNOOZE"
    const val ACTION_SNOOZE_HOME = "SNOOZE_HOME"
}

data class WatchReminderItem(
    val reminderEventId: Int,
    val reminderId: Int,
    val medicineName: String,
    val amount: String,
    val remindedEpochSecond: Long,
    val status: String,
    val variableAmount: Boolean
)

data class WatchAction(
    val action: String,
    val reminderEventId: Int
)
