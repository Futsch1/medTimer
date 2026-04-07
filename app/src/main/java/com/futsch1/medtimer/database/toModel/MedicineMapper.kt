package com.futsch1.medtimer.database.toModel

import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.MedicineEntity
import com.futsch1.medtimer.model.Medicine
import java.time.LocalDate

fun Medicine.toEntity(): MedicineEntity = MedicineEntity(
    name = name,
    medicineId = id,
    color = color,
    useColor = useColor,
    notificationImportance = notificationImportance.value,
    iconId = iconId,
    amount = amount,
    refillSizes = if (refillSize != 0.0) mutableListOf(refillSize) else mutableListOf(),
    unit = unit,
    sortOrder = sortOrder,
    notes = notes,
    showNotificationAsAlarm = showNotificationAsAlarm,
    productionDate = productionDate.toEpochDay(),
    expirationDate = expirationDate.toEpochDay()
)

fun FullMedicineEntity.toModel(): Medicine {
    return Medicine(
        name = medicine.name,
        id = medicine.medicineId,
        color = medicine.color,
        useColor = medicine.useColor,
        notificationImportance = if (medicine.notificationImportance == Medicine.NotificationImportance.DEFAULT.value) Medicine.NotificationImportance.DEFAULT else Medicine.NotificationImportance.HIGH,
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
