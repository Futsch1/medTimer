package com.futsch1.medtimer.database.toBackup

import com.futsch1.medtimer.core.domain.backup.FullMedicineBackup
import com.futsch1.medtimer.core.domain.backup.MedicineBackup
import com.futsch1.medtimer.core.domain.backup.ReminderBackup
import com.futsch1.medtimer.core.domain.backup.ReminderEventBackup
import com.futsch1.medtimer.core.domain.backup.TagBackup
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.MedicineEntity
import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.database.ReminderEntityType
import com.futsch1.medtimer.database.ReminderEventEntity
import com.futsch1.medtimer.database.TagEntity

// --- Medicine ---

fun MedicineEntity.toBackup(): MedicineBackup = MedicineBackup(
    name = name,
    color = color,
    useColor = useColor,
    notificationImportance = notificationImportance,
    iconId = iconId,
    amount = amount,
    refillSizes = refillSizes?.toMutableList(),
    unit = unit,
    sortOrder = sortOrder,
    notes = notes,
    showNotificationAsAlarm = showNotificationAsAlarm,
    productionDate = productionDate,
    expirationDate = expirationDate,
    cannotBeSkipped = cannotBeSkipped
)

fun MedicineBackup.toEntity(): MedicineEntity = MedicineEntity(
    name = name,
    color = color,
    useColor = useColor,
    notificationImportance = notificationImportance,
    iconId = iconId,
    amount = amount,
    refillSizes = refillSizes?.toMutableList(),
    unit = unit,
    sortOrder = sortOrder,
    notes = notes,
    showNotificationAsAlarm = showNotificationAsAlarm,
    productionDate = productionDate,
    expirationDate = expirationDate,
    cannotBeSkipped = cannotBeSkipped
)

// --- Reminder ---

fun ReminderEntity.toBackup(): ReminderBackup = ReminderBackup(
    reminderId = reminderId,
    timeInMinutes = timeInMinutes,
    consecutiveDays = consecutiveDays,
    pauseDays = pauseDays,
    instructions = instructions,
    cycleStartDay = cycleStartDay,
    amount = amount,
    days = days.toMutableList(),
    active = active,
    periodStart = periodStart,
    periodEnd = periodEnd,
    activeDaysOfMonth = activeDaysOfMonth,
    linkedReminderId = linkedReminderId,
    intervalStart = intervalStart,
    intervalStartsFromProcessed = intervalStartsFromProcessed,
    variableAmount = variableAmount,
    automaticallyTaken = automaticallyTaken,
    intervalStartTimeOfDay = intervalStartTimeOfDay,
    intervalEndTimeOfDay = intervalEndTimeOfDay,
    windowedInterval = windowedInterval,
    outOfStockThreshold = outOfStockThreshold,
    outOfStockReminderType = Reminder.OutOfStockReminderType.valueOf(outOfStockReminderType.name),
    expirationReminderType = Reminder.ExpirationReminderType.valueOf(expirationReminderType.name),
)

fun ReminderBackup.toEntity(): ReminderEntity = ReminderEntity(
    reminderId = reminderId,
    timeInMinutes = timeInMinutes,
    consecutiveDays = consecutiveDays,
    pauseDays = pauseDays,
    instructions = instructions,
    cycleStartDay = cycleStartDay,
    amount = amount,
    days = days.toMutableList(),
    active = active,
    periodStart = periodStart,
    periodEnd = periodEnd,
    activeDaysOfMonth = activeDaysOfMonth,
    linkedReminderId = linkedReminderId,
    intervalStart = intervalStart,
    intervalStartsFromProcessed = intervalStartsFromProcessed,
    variableAmount = variableAmount,
    automaticallyTaken = automaticallyTaken,
    intervalStartTimeOfDay = intervalStartTimeOfDay,
    intervalEndTimeOfDay = intervalEndTimeOfDay,
    windowedInterval = windowedInterval,
    outOfStockThreshold = outOfStockThreshold,
    outOfStockReminderType = ReminderEntity.OutOfStockReminderType.valueOf(outOfStockReminderType.name),
    expirationReminderType = ReminderEntity.ExpirationReminderType.valueOf(expirationReminderType.name),
)

// --- Tag ---

fun TagEntity.toBackup(): TagBackup = TagBackup(name = name)

fun TagBackup.toEntity(): TagEntity = TagEntity(name = name)

// --- ReminderEvent ---

fun ReminderEventEntity.toBackup(): ReminderEventBackup = ReminderEventBackup(
    medicineName = medicineName,
    amount = amount,
    color = color,
    useColor = useColor,
    status = ReminderEvent.ReminderStatus.valueOf(status.name),
    remindedTimestamp = remindedTimestamp,
    processedTimestamp = processedTimestamp,
    reminderId = reminderId,
    iconId = iconId,
    tags = tags,
    lastIntervalReminderTimeInMinutes = lastIntervalReminderTimeInMinutes,
    notes = notes,
    reminderType = ReminderType.valueOf(reminderType.name),
)

fun ReminderEventBackup.toEntity(): ReminderEventEntity = ReminderEventEntity(
    medicineName = medicineName,
    amount = amount,
    color = color,
    useColor = useColor,
    status = ReminderEventEntity.ReminderEntityStatus.valueOf(status.name),
    remindedTimestamp = remindedTimestamp,
    processedTimestamp = processedTimestamp,
    reminderId = reminderId,
    iconId = iconId,
    tags = tags,
    lastIntervalReminderTimeInMinutes = lastIntervalReminderTimeInMinutes,
    notes = notes,
    reminderType = ReminderEntityType.valueOf(reminderType.name),
)

// --- FullMedicine ---

fun FullMedicineEntity.toBackup(): FullMedicineBackup = FullMedicineBackup(
    medicine = medicine.toBackup(),
    tags = tags.map { it.toBackup() },
    reminders = reminders.map { it.toBackup() }.toMutableList(),
)
