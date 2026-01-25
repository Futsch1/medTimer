package com.futsch1.medtimer.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class StockEvent {
    @PrimaryKey(autoGenerate = true)
    var stockEventId: Int = 0
    var medicineId: Int = 0
    var timestamp: Long = 0
    var type: StockEventType = StockEventType.REFILL
    var amount: Double = 0.0
    var refillSize: Double = 0.0
    var expirationDate: Long = 0
}

enum class StockEventType {
    REFILL,
    STOCK_REMINDER,
    EXPIRATION_REMINDER
}