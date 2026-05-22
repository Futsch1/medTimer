package com.futsch1.medtimer.core.domain.backup

class FullMedicineBackup(
    var medicine: MedicineBackup = MedicineBackup(),
    var tags: List<TagBackup> = listOf(),
    var reminders: MutableList<ReminderBackup> = mutableListOf(),
)
