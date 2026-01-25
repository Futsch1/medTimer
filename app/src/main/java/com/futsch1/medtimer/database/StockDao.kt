package com.futsch1.medtimer.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface StockDao {
    @Insert
    fun insertStockEvent(stockEvent: StockEvent): Long

    @Query("SELECT * FROM StockEvent WHERE medicineId = :medicineId ORDER BY timestamp DESC")
    fun getStockEventsForMedicine(medicineId: Int): List<StockEvent>
}
