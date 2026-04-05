package com.futsch1.medtimer.database

import com.futsch1.medtimer.model.ReminderEvent
import com.futsch1.medtimer.model.ReminderEventType
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

fun ReminderEvent.toEntity(): ReminderEventEntity {
    val entity = ReminderEventEntity()
    entity.reminderEventId = reminderEventId
    entity.reminderId = reminderId
    entity.medicineName = medicineName
    entity.amount = amount
    entity.color = color
    entity.useColor = useColor
    entity.iconId = iconId
    entity.tags = tags
    entity.status = status.toEntity()
    entity.remindedTimestamp = remindedTimestamp.epochSecond
    entity.processedTimestamp = processedTimestamp.epochSecond
    entity.notificationId = notificationId
    entity.remainingRepeats = remainingRepeats
    entity.notes = notes
    entity.reminderType = reminderType.toEntityReminderType()
    entity.stockHandled = stockHandled
    entity.askForAmount = askForAmount
    entity.lastIntervalReminderTimeInMinutes = lastIntervalReminderTimeInMinutes
    return entity
}

fun ReminderEventEntity.ReminderStatus.toModel(): ReminderEvent.ReminderStatus =
    when (this) {
        ReminderEventEntity.ReminderStatus.RAISED -> ReminderEvent.ReminderStatus.RAISED
        ReminderEventEntity.ReminderStatus.TAKEN -> ReminderEvent.ReminderStatus.TAKEN
        ReminderEventEntity.ReminderStatus.SKIPPED -> ReminderEvent.ReminderStatus.SKIPPED
        ReminderEventEntity.ReminderStatus.DELETED -> ReminderEvent.ReminderStatus.DELETED
        ReminderEventEntity.ReminderStatus.ACKNOWLEDGED -> ReminderEvent.ReminderStatus.ACKNOWLEDGED
    }

fun ReminderEvent.ReminderStatus.toEntity(): ReminderEventEntity.ReminderStatus =
    when (this) {
        ReminderEvent.ReminderStatus.RAISED -> ReminderEventEntity.ReminderStatus.RAISED
        ReminderEvent.ReminderStatus.TAKEN -> ReminderEventEntity.ReminderStatus.TAKEN
        ReminderEvent.ReminderStatus.SKIPPED -> ReminderEventEntity.ReminderStatus.SKIPPED
        ReminderEvent.ReminderStatus.DELETED -> ReminderEventEntity.ReminderStatus.DELETED
        ReminderEvent.ReminderStatus.ACKNOWLEDGED -> ReminderEventEntity.ReminderStatus.ACKNOWLEDGED
    }

fun ReminderEntity.ReminderType.toModelReminderEventType(): ReminderEventType =
    when (this) {
        ReminderEntity.ReminderType.TIME_BASED -> ReminderEventType.TIME_BASED
        ReminderEntity.ReminderType.LINKED -> ReminderEventType.LINKED
        ReminderEntity.ReminderType.CONTINUOUS_INTERVAL -> ReminderEventType.CONTINUOUS_INTERVAL
        ReminderEntity.ReminderType.WINDOWED_INTERVAL -> ReminderEventType.WINDOWED_INTERVAL
        ReminderEntity.ReminderType.OUT_OF_STOCK -> ReminderEventType.OUT_OF_STOCK
        ReminderEntity.ReminderType.EXPIRATION_DATE -> ReminderEventType.EXPIRATION_DATE
        ReminderEntity.ReminderType.REFILL -> ReminderEventType.REFILL
    }

fun ReminderEventType.toEntityReminderType(): ReminderEntity.ReminderType =
    when (this) {
        ReminderEventType.TIME_BASED -> ReminderEntity.ReminderType.TIME_BASED
        ReminderEventType.LINKED -> ReminderEntity.ReminderType.LINKED
        ReminderEventType.CONTINUOUS_INTERVAL -> ReminderEntity.ReminderType.CONTINUOUS_INTERVAL
        ReminderEventType.WINDOWED_INTERVAL -> ReminderEntity.ReminderType.WINDOWED_INTERVAL
        ReminderEventType.OUT_OF_STOCK -> ReminderEntity.ReminderType.OUT_OF_STOCK
        ReminderEventType.EXPIRATION_DATE -> ReminderEntity.ReminderType.EXPIRATION_DATE
        ReminderEventType.REFILL -> ReminderEntity.ReminderType.REFILL
    }
