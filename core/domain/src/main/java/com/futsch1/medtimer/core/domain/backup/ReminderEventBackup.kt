package com.futsch1.medtimer.core.domain.backup

import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ReminderType

class ReminderEventBackup(
    var medicineName: String = "",
    var amount: String = "",
    var color: Int = 0,
    var useColor: Boolean = false,
    var status: ReminderEvent.ReminderStatus = ReminderEvent.ReminderStatus.RAISED,
    var remindedTimestamp: Long = 0,
    var processedTimestamp: Long = 0,
    var reminderId: Int = 0,
    var iconId: Int = 0,
    var tags: List<String> = listOf(),
    var lastIntervalReminderTimeInMinutes: Int = 0,
    var notes: String = "",
    var reminderType: ReminderType = ReminderType.TIME_BASED,
    var stockBefore: Double = -1.0,
    var stockAfter: Double = -1.0,
    var stockUnit: String = "",
)
