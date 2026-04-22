package com.futsch1.medtimer.database.toModel

import com.futsch1.medtimer.database.ReminderEntityType
import com.futsch1.medtimer.database.ReminderEventEntity
import com.futsch1.medtimer.model.ReminderEvent
import com.futsch1.medtimer.model.ReminderType
import java.time.Instant

fun ReminderEventEntity.toModel(): ReminderEvent =
    ReminderEvent(
        reminderEventId = reminderEventId,
        reminderId = reminderId,
        medicineName = medicineName,
        amount = amount,
        color = color,
        useColor = useColor,
        iconId = iconId,
        tags = tags,
        status = status.toModel(),
        remindedTimestamp = Instant.ofEpochSecond(remindedTimestamp),
        processedTimestamp = Instant.ofEpochSecond(processedTimestamp),
        notificationId = notificationId,
        remainingRepeats = remainingRepeats,
        notes = notes,
        reminderType = reminderType.toModelReminderEventType(),
        stockHandled = stockHandled,
        askForAmount = askForAmount,
        lastIntervalReminderTimeInMinutes = lastIntervalReminderTimeInMinutes
    )

fun ReminderEvent.toEntity(): ReminderEventEntity = ReminderEventEntity(
    reminderEventId = reminderEventId,
    reminderId = reminderId,
    medicineName = medicineName,
    amount = amount,
    color = color,
    useColor = useColor,
    iconId = iconId,
    tags = tags,
    status = status.toEntity(),
    remindedTimestamp = remindedTimestamp.epochSecond,
    processedTimestamp = processedTimestamp.epochSecond,
    notificationId = notificationId,
    remainingRepeats = remainingRepeats,
    notes = notes,
    reminderType = reminderType.toEntityReminderType(),
    stockHandled = stockHandled,
    askForAmount = askForAmount,
    lastIntervalReminderTimeInMinutes = lastIntervalReminderTimeInMinutes
)

fun ReminderEventEntity.ReminderEntityStatus.toModel(): ReminderEvent.ReminderStatus =
    when (this) {
        ReminderEventEntity.ReminderEntityStatus.RAISED -> ReminderEvent.ReminderStatus.RAISED
        ReminderEventEntity.ReminderEntityStatus.TAKEN -> ReminderEvent.ReminderStatus.TAKEN
        ReminderEventEntity.ReminderEntityStatus.SKIPPED -> ReminderEvent.ReminderStatus.SKIPPED
        ReminderEventEntity.ReminderEntityStatus.DELETED -> ReminderEvent.ReminderStatus.DELETED
        ReminderEventEntity.ReminderEntityStatus.ACKNOWLEDGED -> ReminderEvent.ReminderStatus.ACKNOWLEDGED
    }

fun ReminderEvent.ReminderStatus.toEntity(): ReminderEventEntity.ReminderEntityStatus =
    when (this) {
        ReminderEvent.ReminderStatus.RAISED -> ReminderEventEntity.ReminderEntityStatus.RAISED
        ReminderEvent.ReminderStatus.TAKEN -> ReminderEventEntity.ReminderEntityStatus.TAKEN
        ReminderEvent.ReminderStatus.SKIPPED -> ReminderEventEntity.ReminderEntityStatus.SKIPPED
        ReminderEvent.ReminderStatus.DELETED -> ReminderEventEntity.ReminderEntityStatus.DELETED
        ReminderEvent.ReminderStatus.ACKNOWLEDGED -> ReminderEventEntity.ReminderEntityStatus.ACKNOWLEDGED
    }

fun ReminderEntityType.toModelReminderEventType(): ReminderType =
    when (this) {
        ReminderEntityType.TIME_BASED -> ReminderType.TIME_BASED
        ReminderEntityType.LINKED -> ReminderType.LINKED
        ReminderEntityType.CONTINUOUS_INTERVAL -> ReminderType.CONTINUOUS_INTERVAL
        ReminderEntityType.WINDOWED_INTERVAL -> ReminderType.WINDOWED_INTERVAL
        ReminderEntityType.OUT_OF_STOCK -> ReminderType.OUT_OF_STOCK
        ReminderEntityType.EXPIRATION_DATE -> ReminderType.EXPIRATION_DATE
        ReminderEntityType.REFILL -> ReminderType.REFILL
    }

fun ReminderType.toEntityReminderType(): ReminderEntityType =
    when (this) {
        ReminderType.TIME_BASED -> ReminderEntityType.TIME_BASED
        ReminderType.LINKED -> ReminderEntityType.LINKED
        ReminderType.CONTINUOUS_INTERVAL -> ReminderEntityType.CONTINUOUS_INTERVAL
        ReminderType.WINDOWED_INTERVAL -> ReminderEntityType.WINDOWED_INTERVAL
        ReminderType.OUT_OF_STOCK -> ReminderEntityType.OUT_OF_STOCK
        ReminderType.EXPIRATION_DATE -> ReminderEntityType.EXPIRATION_DATE
        ReminderType.REFILL -> ReminderEntityType.REFILL
    }
