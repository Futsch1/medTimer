package com.futsch1.medtimer

import com.futsch1.medtimer.core.domain.repository.BackupRepository
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
        val backupRepository = Mockito.mock<BackupRepository>()

        checkBackup(JSONMedicineBackup(backupRepository), json)
        checkBackup(JSONReminderEventBackup(backupRepository), json)
    }

    private fun <T> checkBackup(backup: JSONBackup<T>, json: String) {
        val parsedData = backup.parseBackup(json)
        if (parsedData != null) {
            runBlocking { backup.applyBackup(parsedData) }
        }
    }
}
