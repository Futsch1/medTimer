package com.futsch1.medtimer

import com.futsch1.medtimer.database.dao.MedicineDao
import com.futsch1.medtimer.database.dao.ReminderDao
import com.futsch1.medtimer.database.dao.ReminderEventDao
import com.futsch1.medtimer.database.dao.TagDao
import com.futsch1.medtimer.database.backup.JSONBackup
import com.futsch1.medtimer.database.backup.JSONMedicineBackup
import com.futsch1.medtimer.database.backup.JSONReminderEventBackup
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mockito

@RunWith(Parameterized::class)
class JSONBackupFuzzTest(private val json: String) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun inputs() = listOf(
            "",
            "{}",
            "[]",
            "null",
            """{"invalid":}""",
            "normal string",
            """{"medicines":[]}"""
        )
    }

    @Test
    fun fuzzTestMedicineBackup() {
        val medicineDao = Mockito.mock<MedicineDao>()
        val reminderDao = Mockito.mock<ReminderDao>()
        val reminderEventDao = Mockito.mock<ReminderEventDao>()
        val tagDao = Mockito.mock<TagDao>()

        checkBackup(JSONMedicineBackup(medicineDao, reminderDao, tagDao), json)
        checkBackup(JSONReminderEventBackup(reminderEventDao), json)
    }

    private fun <T> checkBackup(backup: JSONBackup<T>, json: String) {
        val parsedData = backup.parseBackup(json)
        if (parsedData != null) {
            runBlocking { backup.applyBackup(parsedData) }
        }
    }
}
