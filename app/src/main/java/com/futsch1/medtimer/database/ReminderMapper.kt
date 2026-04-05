package com.futsch1.medtimer.database

import com.futsch1.medtimer.model.Reminder
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

fun ReminderEntity.toModel(): Reminder =
    Reminder(
        id = reminderId,
        medicineRelId = medicineRelId,
        time = LocalTime.ofSecondOfDay(timeInMinutes * 60L),
        createdTime = Instant.ofEpochSecond(createdTimestamp),
        consecutiveDays = consecutiveDays,
        pauseDays = pauseDays,
        instructions = instructions,
        cycleStartDay = LocalDate.ofEpochDay(cycleStartDay),
        amount = amount,
        days = DayOfWeek.entries.filterIndexed { i, _ -> days.getOrElse(i) { false } },
        active = active,
        periodStart = LocalDate.ofEpochDay(periodStart),
        periodEnd = LocalDate.ofEpochDay(periodEnd),
        activeDaysOfMonth = (1..31).filter { day -> (activeDaysOfMonth ushr (day - 1)) and 1 == 1 },
        linkedReminderId = linkedReminderId,
        intervalStart = Instant.ofEpochSecond(intervalStart),
        intervalStartsFromProcessed = intervalStartsFromProcessed,
        variableAmount = variableAmount,
        automaticallyTaken = automaticallyTaken,
        intervalStartTimeOfDay = LocalTime.ofSecondOfDay(intervalStartTimeOfDay * 60L),
        intervalEndTimeOfDay = LocalTime.ofSecondOfDay(intervalEndTimeOfDay * 60L),
        windowedInterval = windowedInterval,
        outOfStockThreshold = outOfStockThreshold,
        outOfStockReminderType = outOfStockReminderType.toModel(),
        expirationReminderType = expirationReminderType.toModel()
    )

fun Reminder.toEntity(): ReminderEntity {
    val entity = ReminderEntity(medicineRelId)
    entity.reminderId = id
    entity.timeInMinutes = time.hour * 60 + time.minute
    entity.createdTimestamp = createdTime.epochSecond
    entity.consecutiveDays = consecutiveDays
    entity.pauseDays = pauseDays
    entity.instructions = instructions
    entity.cycleStartDay = cycleStartDay.toEpochDay()
    entity.amount = amount
    entity.days = DayOfWeek.entries.map { it in days }.toMutableList()
    entity.active = active
    entity.periodStart = periodStart.toEpochDay()
    entity.periodEnd = periodEnd.toEpochDay()
    entity.activeDaysOfMonth = activeDaysOfMonth.fold(0) { acc, day -> acc or (1 shl (day - 1)) }
    entity.linkedReminderId = linkedReminderId
    entity.intervalStart = intervalStart.epochSecond
    entity.intervalStartsFromProcessed = intervalStartsFromProcessed
    entity.variableAmount = variableAmount
    entity.automaticallyTaken = automaticallyTaken
    entity.intervalStartTimeOfDay = intervalStartTimeOfDay.hour * 60 + intervalStartTimeOfDay.minute
    entity.intervalEndTimeOfDay = intervalEndTimeOfDay.hour * 60 + intervalEndTimeOfDay.minute
    entity.windowedInterval = windowedInterval
    entity.outOfStockThreshold = outOfStockThreshold
    entity.outOfStockReminderType = outOfStockReminderType.toEntity()
    entity.expirationReminderType = expirationReminderType.toEntity()
    return entity
}

fun ReminderEntity.OutOfStockReminderType.toModel(): Reminder.OutOfStockReminderType =
    when (this) {
        ReminderEntity.OutOfStockReminderType.ONCE -> Reminder.OutOfStockReminderType.ONCE
        ReminderEntity.OutOfStockReminderType.ALWAYS -> Reminder.OutOfStockReminderType.ALWAYS
        ReminderEntity.OutOfStockReminderType.DAILY -> Reminder.OutOfStockReminderType.DAILY
        ReminderEntity.OutOfStockReminderType.OFF -> Reminder.OutOfStockReminderType.OFF
    }

fun Reminder.OutOfStockReminderType.toEntity(): ReminderEntity.OutOfStockReminderType =
    when (this) {
        Reminder.OutOfStockReminderType.ONCE -> ReminderEntity.OutOfStockReminderType.ONCE
        Reminder.OutOfStockReminderType.ALWAYS -> ReminderEntity.OutOfStockReminderType.ALWAYS
        Reminder.OutOfStockReminderType.DAILY -> ReminderEntity.OutOfStockReminderType.DAILY
        Reminder.OutOfStockReminderType.OFF -> ReminderEntity.OutOfStockReminderType.OFF
    }

fun ReminderEntity.ExpirationReminderType.toModel(): Reminder.ExpirationReminderType =
    when (this) {
        ReminderEntity.ExpirationReminderType.ONCE -> Reminder.ExpirationReminderType.ONCE
        ReminderEntity.ExpirationReminderType.DAILY -> Reminder.ExpirationReminderType.DAILY
        ReminderEntity.ExpirationReminderType.OFF -> Reminder.ExpirationReminderType.OFF
    }

fun Reminder.ExpirationReminderType.toEntity(): ReminderEntity.ExpirationReminderType =
    when (this) {
        Reminder.ExpirationReminderType.ONCE -> ReminderEntity.ExpirationReminderType.ONCE
        Reminder.ExpirationReminderType.DAILY -> ReminderEntity.ExpirationReminderType.DAILY
        Reminder.ExpirationReminderType.OFF -> ReminderEntity.ExpirationReminderType.OFF
    }
