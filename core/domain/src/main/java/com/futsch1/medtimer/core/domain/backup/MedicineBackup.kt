package com.futsch1.medtimer.core.domain.backup

class MedicineBackup(
    var name: String? = null,
    var color: Int = 0,
    var useColor: Boolean = false,
    var notificationImportance: Int = 3,
    var iconId: Int = 0,
    var amount: Double = 0.0,
    var refillSizes: MutableList<Double>? = null,
    var unit: String? = null,
    var sortOrder: Double = 1.0,
    var notes: String? = "",
    var showNotificationAsAlarm: Boolean = false,
    var productionDate: Long = 0,
    var expirationDate: Long = 0,
    var cannotBeSkipped: Boolean = false
)
