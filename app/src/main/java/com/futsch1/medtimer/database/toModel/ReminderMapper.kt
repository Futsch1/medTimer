package com.futsch1.medtimer.database.toModel

import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.model.Reminder
import com.futsch1.medtimer.model.ReminderTime
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

fun ReminderEntity.toModel(): Reminder =
    Reminder(
        id = reminderId,
        medicineRelId = medicineRelId,
        time = ReminderTime(timeInMinutes, intervalStart != 0L || windowedInterval),
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

fun Reminder.toEntity(): ReminderEntity = ReminderEntity(
    medicineRelId = medicineRelId,
    reminderId = id,
    timeInMinutes = time.minutes,
    createdTimestamp = createdTime.epochSecond,
    consecutiveDays = consecutiveDays,
    pauseDays = pauseDays,
    instructions = instructions,
    cycleStartDay = cycleStartDay.toEpochDay(),
    amount = amount,
    days = DayOfWeek.entries.map { it in days }.toMutableList(),
    active = active,
    periodStart = periodStart.toEpochDay(),
    periodEnd = periodEnd.toEpochDay(),
    activeDaysOfMonth = activeDaysOfMonth.fold(0) { acc, day -> acc or (1 shl (day - 1)) },
    linkedReminderId = linkedReminderId,
    intervalStart = intervalStart.epochSecond,
    intervalStartsFromProcessed = intervalStartsFromProcessed,
    variableAmount = variableAmount,
    automaticallyTaken = automaticallyTaken,
    intervalStartTimeOfDay = intervalStartTimeOfDay.hour * 60 + intervalStartTimeOfDay.minute,
    intervalEndTimeOfDay = intervalEndTimeOfDay.hour * 60 + intervalEndTimeOfDay.minute,
    windowedInterval = windowedInterval,
    outOfStockThreshold = outOfStockThreshold,
    outOfStockReminderType = outOfStockReminderType.toEntity(),
    expirationReminderType = expirationReminderType.toEntity()
)

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
