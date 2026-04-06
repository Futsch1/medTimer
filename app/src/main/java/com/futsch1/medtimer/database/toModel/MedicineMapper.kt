package com.futsch1.medtimer.database.toModel

import com.futsch1.medtimer.ReminderNotificationChannelManager
import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.MedicineEntity
import com.futsch1.medtimer.model.Medicine
import java.time.LocalDate

fun Medicine.toEntity(): MedicineEntity {
    val entity = MedicineEntity()
    entity.name = name
    entity.medicineId = id
    entity.color = color
    entity.useColor = useColor
    entity.notificationImportance = notificationImportance.value
    entity.iconId = iconId
    entity.amount = amount
    entity.refillSizes = if (refillSize != 0.0) mutableListOf(refillSize) else mutableListOf()
    entity.unit = unit
    entity.notes = notes
    entity.showNotificationAsAlarm = showNotificationAsAlarm
    entity.productionDate = productionDate.toEpochDay()
    entity.expirationDate = expirationDate.toEpochDay()
    entity.sortOrder = sortOrder
    return entity
}

fun FullMedicineEntity.toModel(): Medicine {
    return Medicine(
        name = medicine.name,
        id = medicine.medicineId,
        color = medicine.color,
        useColor = medicine.useColor,
        notificationImportance = if (medicine.notificationImportance == ReminderNotificationChannelManager.Importance.DEFAULT.value) ReminderNotificationChannelManager.Importance.DEFAULT else ReminderNotificationChannelManager.Importance.HIGH,
        iconId = medicine.iconId,
        amount = medicine.amount,
        refillSize = if (medicine.refillSizes.isEmpty()) 0.0 else medicine.refillSizes[0],
        unit = medicine.unit,
        notes = medicine.notes ?: "",
        showNotificationAsAlarm = medicine.showNotificationAsAlarm,
        productionDate = LocalDate.ofEpochDay(medicine.productionDate),
        expirationDate = LocalDate.ofEpochDay(medicine.expirationDate),
        sortOrder = medicine.sortOrder,
        tags = tags.map { it.toModel() },
        reminders = reminders.map { it.toModel() }
    )
}
