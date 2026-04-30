package com.futsch1.medtimer

object ActivityCodes {
    const val EXTRA_SNOOZE_TIME_SECONDS: String = "com.futsch1.medtimer.SNOOZE_TIME"
    const val EXTRA_NOTIFICATION_ID: String = "com.futsch1.medtimer.NOTIFICATION_ID"
    const val EXTRA_AMOUNT: String = "com.futsch1.medtimer.AMOUNT"
    const val EXTRA_MEDICINE_ID: String = "com.futsch1.medtimer.MEDICINE_ID"
    const val EXTRA_REMINDER_EVENT_ID_LIST: String = "com.futsch1.medtimer.REMINDER_EVENT_ID_LIST"
    const val EXTRA_REMINDER_ID_LIST: String = "com.futsch1.medtimer.REMINDER_ID_LIST"
    const val EXTRA_REMIND_INSTANT: String = "com.futsch1.medtimer.REMIND_INSTANT"

    const val VARIABLE_AMOUNT_ACTIVITY: String = "com.futsch1.medtimer.VARIABLE_AMOUNT_ACTIVITY"
    const val CUSTOM_SNOOZE_ACTIVITY: String = "com.futsch1.medtimer.CUSTOM_SNOOZE_ACTIVITY"
}

enum class ProcessorCode(val action: String) {
    Reminder("com.futsch1.medtimer.REMINDER_ACTION"),
    Dismissed("com.futsch1.medtimer.DISMISSED_ACTION"),
    Taken("com.futsch1.medtimer.TAKEN_ACTION"),
    Snooze("com.futsch1.medtimer.SNOOZE_ACTION"),
    Acknowledged("com.futsch1.medtimer.ACKNOWLEDGED_ACTION"),
    Refill("com.futsch1.medtimer.REFILL_ACTION"),
    ShowReminderNotification("com.futsch1.medtimer.SHOW_REMINDER_NOTIFICATION"),
    StockHandling("com.futsch1.medtimer.STOCK_HANDLING"),
    Schedule("com.futsch1.medtimer.SCHEDULE"),
    LocationSnooze("com.futsch1.medtimer.LOCATION_SNOOZE");

    companion object {
        private val actionMap = entries.associateBy { it.action }

        fun fromAction(action: String?): ProcessorCode? = actionMap[action]
    }
}
