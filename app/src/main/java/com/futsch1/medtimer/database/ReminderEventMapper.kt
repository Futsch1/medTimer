package com.futsch1.medtimer.database

import com.futsch1.medtimer.model.Reminder
import com.futsch1.medtimer.model.reminderevent.DoseReminderEvent
import com.futsch1.medtimer.model.reminderevent.IntervalReminderEvent
import com.futsch1.medtimer.model.reminderevent.ReminderEvent
import com.futsch1.medtimer.model.reminderevent.StockReminderEvent
import com.futsch1.medtimer.model.reminderevent.StockReminderType
import java.time.Instant

fun ReminderEventEntity.toModel(reminder: Reminder? = null): ReminderEvent {
    val commonArgs = CommonArgs(
        reminderEventId = reminderEventId,
        reminderId = reminderId,
        reminder = reminder,
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
        notes = notes
    )
    return when (reminderType) {
        ReminderEntity.ReminderType.TIME_BASED,
        ReminderEntity.ReminderType.LINKED -> DoseReminderEvent(
            reminderEventId = commonArgs.reminderEventId,
            reminderId = commonArgs.reminderId,
            reminder = commonArgs.reminder,
            medicineName = commonArgs.medicineName,
            amount = commonArgs.amount,
            color = commonArgs.color,
            useColor = commonArgs.useColor,
            iconId = commonArgs.iconId,
            tags = commonArgs.tags,
            status = commonArgs.status,
            remindedTimestamp = commonArgs.remindedTimestamp,
            processedTimestamp = commonArgs.processedTimestamp,
            notificationId = commonArgs.notificationId,
            remainingRepeats = commonArgs.remainingRepeats,
            notes = commonArgs.notes,
            stockHandled = stockHandled,
            askForAmount = askForAmount
        )

        ReminderEntity.ReminderType.CONTINUOUS_INTERVAL,
        ReminderEntity.ReminderType.WINDOWED_INTERVAL -> IntervalReminderEvent(
            reminderEventId = commonArgs.reminderEventId,
            reminderId = commonArgs.reminderId,
            reminder = commonArgs.reminder,
            medicineName = commonArgs.medicineName,
            amount = commonArgs.amount,
            color = commonArgs.color,
            useColor = commonArgs.useColor,
            iconId = commonArgs.iconId,
            tags = commonArgs.tags,
            status = commonArgs.status,
            remindedTimestamp = commonArgs.remindedTimestamp,
            processedTimestamp = commonArgs.processedTimestamp,
            notificationId = commonArgs.notificationId,
            remainingRepeats = commonArgs.remainingRepeats,
            notes = commonArgs.notes,
            stockHandled = stockHandled,
            askForAmount = askForAmount,
            lastIntervalReminderTimeInMinutes = lastIntervalReminderTimeInMinutes
        )

        ReminderEntity.ReminderType.OUT_OF_STOCK,
        ReminderEntity.ReminderType.EXPIRATION_DATE,
        ReminderEntity.ReminderType.REFILL -> StockReminderEvent(
            reminderEventId = commonArgs.reminderEventId,
            reminderId = commonArgs.reminderId,
            reminder = commonArgs.reminder,
            medicineName = commonArgs.medicineName,
            amount = commonArgs.amount,
            color = commonArgs.color,
            useColor = commonArgs.useColor,
            iconId = commonArgs.iconId,
            tags = commonArgs.tags,
            status = commonArgs.status,
            remindedTimestamp = commonArgs.remindedTimestamp,
            processedTimestamp = commonArgs.processedTimestamp,
            notificationId = commonArgs.notificationId,
            remainingRepeats = commonArgs.remainingRepeats,
            notes = commonArgs.notes,
            reminderType = reminderType.toStockReminderType()
        )
    }
}

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
    when (this) {
        is DoseReminderEvent -> {
            entity.reminderType = ReminderEntity.ReminderType.TIME_BASED
            entity.stockHandled = stockHandled
            entity.askForAmount = askForAmount
        }

        is IntervalReminderEvent -> {
            entity.reminderType = ReminderEntity.ReminderType.CONTINUOUS_INTERVAL
            entity.stockHandled = stockHandled
            entity.askForAmount = askForAmount
            entity.lastIntervalReminderTimeInMinutes = lastIntervalReminderTimeInMinutes
        }

        is StockReminderEvent -> {
            entity.reminderType = reminderType.toEntityReminderType()
        }
    }
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

fun ReminderEntity.ReminderType.toStockReminderType(): StockReminderType =
    when (this) {
        ReminderEntity.ReminderType.OUT_OF_STOCK -> StockReminderType.OUT_OF_STOCK
        ReminderEntity.ReminderType.EXPIRATION_DATE -> StockReminderType.EXPIRATION_DATE
        ReminderEntity.ReminderType.REFILL -> StockReminderType.REFILL
        else -> throw IllegalArgumentException("Cannot convert $this to StockReminderType")
    }

fun StockReminderType.toEntityReminderType(): ReminderEntity.ReminderType =
    when (this) {
        StockReminderType.OUT_OF_STOCK -> ReminderEntity.ReminderType.OUT_OF_STOCK
        StockReminderType.EXPIRATION_DATE -> ReminderEntity.ReminderType.EXPIRATION_DATE
        StockReminderType.REFILL -> ReminderEntity.ReminderType.REFILL
    }

private data class CommonArgs(
    val reminderEventId: Int,
    val reminderId: Int,
    val reminder: Reminder?,
    val medicineName: String,
    val amount: String,
    val color: Int,
    val useColor: Boolean,
    val iconId: Int,
    val tags: List<String>,
    val status: ReminderEvent.ReminderStatus,
    val remindedTimestamp: Instant,
    val processedTimestamp: Instant,
    val notificationId: Int,
    val remainingRepeats: Int,
    val notes: String
)
