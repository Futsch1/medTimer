package com.futsch1.medtimer

import com.futsch1.medtimer.database.JSONBackup
import com.futsch1.medtimer.database.JSONMedicineBackup
import com.futsch1.medtimer.database.JSONReminderEventBackup
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEventRepository
import com.futsch1.medtimer.database.ReminderRepository
import com.futsch1.medtimer.database.TagRepository
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
        val medicineRepository = Mockito.mock<MedicineRepository>()
        val reminderRepository = Mockito.mock<ReminderRepository>()
        val reminderEventRepository = Mockito.mock<ReminderEventRepository>()
        val tagRepository = Mockito.mock<TagRepository>()

        checkBackup(JSONMedicineBackup(medicineRepository, reminderRepository, tagRepository), json)
        checkBackup(JSONReminderEventBackup(reminderEventRepository), json)
    }

    private fun <T> checkBackup(backup: JSONBackup<T>, json: String) {
        val parsedData = backup.parseBackup(json)
        if (parsedData != null) {
            runBlocking { backup.applyBackup(parsedData) }
        }
    }
}
