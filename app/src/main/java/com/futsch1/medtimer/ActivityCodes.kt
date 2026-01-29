package com.futsch1.medtimer

object ActivityCodes {
    const val EXTRA_SNOOZE_TIME: String = "com.futsch1.medTimer.SNOOZE_TIME"
    const val EXTRA_NOTIFICATION_ID: String = "com.futsch1.medTimer.NOTIFICATION_ID"
    const val EXTRA_REPEAT_TIME_SECONDS: String = "com.futsch1.medTimer.EXTRA_REPEAT_TIME_SECONDS"
    const val EXTRA_REMAINING_REPEATS: String = "com.futsch1.medTimer.EXTRA_REMAINING_REPEATS"
    const val EXTRA_AMOUNT: String = "com.futsch1.medTimer.AMOUNT"
    const val EXTRA_MEDICINE_ID: String = "com.futsch1.medTimer.MEDICINE_ID"
    const val EXTRA_SCHEDULE_FOR_TESTS: String = "com.futsch1.medTimer.EXTRA_SCHEDULE_FOR_TESTS"
    const val EXTRA_REMINDER_EVENT_ID_LIST: String = "com.futsch1.medTimer.REMINDER_EVENT_ID_LIST"
    const val EXTRA_REMINDER_ID_LIST: String = "com.futsch1.medTimer.REMINDER_ID_LIST"
    const val EXTRA_REMIND_INSTANT: String = "com.futsch1.medTimer.REMIND_INSTANT"

    const val REMOTE_INPUT_SNOOZE_ACTION: String = "com.futsch1.medtimer.REMOTE_INPUT_SNOOZE_ACTION"
    const val REMOTE_INPUT_VARIABLE_AMOUNT_ACTION: String = "com.futsch1.medtimer.REMOTE_INPUT_VARIABLE_AMOUNT_ACTION"

    const val VARIABLE_AMOUNT_ACTIVITY: String = "com.futsch1.medTimer.VARIABLE_AMOUNT_ACTIVITY"
    const val CUSTOM_SNOOZE_ACTIVITY: String = "com.futsch1.medTimer.CUSTOM_SNOOZE_ACTIVITY"
}

enum class WorkerActionCodes(val action: String) {
    Reminder("com.futsch1.medTimer.REMINDER_ACTION"),
    Dismissed("com.futsch1.medTimer.DISMISSED_ACTION"),
    Taken("com.futsch1.medTimer.TAKEN_ACTION"),
    Snooze("com.futsch1.medTimer.SNOOZE_ACTION"),
    Acknowledged("com.futsch1.medTimer.ACKNOWLEDGED_ACTION"),
    Refill("com.futsch1.medTimer.REFILL_ACTION")
}