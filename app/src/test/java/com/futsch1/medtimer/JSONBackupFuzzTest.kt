package com.futsch1.medtimer

import com.code_intelligence.jazzer.junit.FuzzTest
import com.futsch1.medtimer.database.JSONBackup
import com.futsch1.medtimer.database.JSONMedicineBackup
import com.futsch1.medtimer.database.JSONReminderEventBackup
import com.futsch1.medtimer.database.MedicineRepository
import org.mockito.Mockito

class JSONBackupFuzzTest {
    @FuzzTest
    fun fuzzTestMedicineBackup(json: String) {
        val jsonMedicineBackup = JSONMedicineBackup()
        val jsonReminderEventBackup = JSONReminderEventBackup()

        checkBackup(jsonMedicineBackup, json)
        checkBackup(jsonReminderEventBackup, json)
    }

    private fun <T> checkBackup(backup: JSONBackup<T>, json: String) {
        val parsedData = backup.parseBackup(json)
        val medicineRepository = Mockito.mock(MedicineRepository::class.java)
        if (parsedData != null) {
            backup.applyBackup(parsedData, medicineRepository)
        }
    }
}
