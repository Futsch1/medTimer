package com.futsch1.medtimer.database

import android.graphics.Color
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.futsch1.medtimer.ReminderNotificationChannelManager
import com.google.gson.annotations.Expose
import java.time.LocalDate
import java.util.Objects

@Entity
class Medicine() {
    @ColumnInfo(name = "medicineName")
    @Expose
    var name: String = ""

    @PrimaryKey(autoGenerate = true)
    var medicineId: Int = 0

    @ColumnInfo(defaultValue = "0xFFFF0000")
    @Expose
    var color: Int = Color.DKGRAY

    @ColumnInfo(defaultValue = "false")
    @Expose
    var useColor: Boolean = false

    @ColumnInfo(defaultValue = "3")
    @Expose
    var notificationImportance: Int = ReminderNotificationChannelManager.Importance.DEFAULT.value

    @ColumnInfo(defaultValue = "0")
    @Expose
    var iconId: Int = 0

    @ColumnInfo(defaultValue = "0")
    @Expose
    var amount: Double = 0.0

    @ColumnInfo(defaultValue = "[]")
    @Expose
    var refillSizes: MutableList<Double> = mutableListOf()

    @ColumnInfo(defaultValue = "")
    @Expose
    var unit: String = ""

    @ColumnInfo(defaultValue = "1.0")
    @Expose
    var sortOrder: Double = 1.0

    @ColumnInfo(defaultValue = "")
    @Expose
    var notes: String? = ""

    @ColumnInfo(defaultValue = "false")
    @Expose
    var showNotificationAsAlarm: Boolean = false

    @ColumnInfo(defaultValue = "0")
    @Expose
    var productionDate: Long = 0

    @ColumnInfo(defaultValue = "0")
    @Expose
    var expirationDate: Long = 0

    @Ignore
    constructor(name: String, id: Int = 0) : this() {
        this.name = name
        this.medicineId = id
    }

    @Ignore
    constructor(name: String) : this() {
        this.name = name
    }

    fun hasExpired(): Boolean {
        return expirationDate != 0L && expirationDate < LocalDate.now().toEpochDay()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) return false
        return membersEqual(other as Medicine)
    }

    override fun hashCode(): Int {
        return Objects.hash(medicineId, name, useColor, color, notificationImportance, iconId, amount, refillSizes, unit, notes, showNotificationAsAlarm)
    }

    private fun membersEqual(that: Medicine): Boolean {
        return medicineId == that.medicineId &&
                name == that.name && useColor == that.useColor && color == that.color && notificationImportance == that.notificationImportance && iconId == that.iconId && amount == that.amount &&
                refillSizes == that.refillSizes &&
                unit == that.unit &&
                notes == that.notes && showNotificationAsAlarm == that.showNotificationAsAlarm && expirationDate == that.expirationDate && productionDate == that.productionDate
    }

    val refillSize: Double
        get() = (if (refillSizes.isEmpty()) 0.0 else refillSizes[0])
}
