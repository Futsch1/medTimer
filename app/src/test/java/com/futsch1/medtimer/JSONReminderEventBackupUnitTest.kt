package com.futsch1.medtimer

import android.graphics.Color
import com.futsch1.medtimer.database.JSONReminderEventBackup
import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.database.ReminderEventEntity
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class JSONReminderEventBackupUnitTest {
    // creates a backup object with a version number and a medicines array
    @Test
    fun testBackup() {
        val reminderEvents = listOf(ReminderEventEntity().apply {
            medicineName = "Medicine A"
            color = Color.RED
            useColor = true
            amount = "1"
            status = ReminderEventEntity.ReminderStatus.TAKEN
            remindedTimestamp = 1
            processedTimestamp = 2
            reminderId = 3
            iconId = 4

            tags = listOf("Tag A")
            lastIntervalReminderTimeInMinutes = 12
            notes = "Notes"
            reminderType = ReminderEntity.ReminderType.LINKED
        })

        val jsonReminderEventBackup = JSONReminderEventBackup()
        val result = assertNotNull(jsonReminderEventBackup.createBackupAsString(1, reminderEvents))

        // @formatter:off
        assertEquals("""
{
  "version": 1,
  "list": [
    {
      "medicineName": "Medicine A",
      "amount": "1",
      "color": -65536,
      "useColor": true,
      "status": "TAKEN",
      "remindedTimestamp": 1,
      "processedTimestamp": 2,
      "reminderId": 3,
      "iconId": 4,
      "tags": [
        "Tag A"
      ],
      "lastIntervalReminderTimeInMinutes": 12,
      "notes": "Notes",
      "reminderType": "LINKED"
    }
  ]
}
""".trimIndent(), result)
        
        val parsedReminders =
            assertNotNull(jsonReminderEventBackup.parseBackup(result))
        compareListReminderEvents(parsedReminders, reminderEvents)
    }

    private fun compareListReminderEvents(
        actual: List<ReminderEventEntity>,
        expected: List<ReminderEventEntity>
    ) {
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            compareReminderEvent(actual.get(i), expected.get(i))
        }
    }

    private fun compareReminderEvent(reminderEvent1: ReminderEventEntity, reminderEvent2: ReminderEventEntity) {
        assertEquals(reminderEvent1.medicineName, reminderEvent2.medicineName)
        assertEquals(reminderEvent1.color, reminderEvent2.color)
        assertEquals(reminderEvent1.useColor, reminderEvent2.useColor)
        assertEquals(reminderEvent1.amount, reminderEvent2.amount)
        assertEquals(reminderEvent1.status, reminderEvent2.status)
        assertEquals(reminderEvent1.remindedTimestamp, reminderEvent2.remindedTimestamp)
        assertEquals(
            reminderEvent1.processedTimestamp,
            reminderEvent2.processedTimestamp
        )
        assertEquals(reminderEvent1.reminderId, reminderEvent2.reminderId)
        assertEquals(reminderEvent1.iconId, reminderEvent2.iconId)
        assertEquals(reminderEvent1.tags, reminderEvent2.tags)
        assertEquals(
            reminderEvent1.lastIntervalReminderTimeInMinutes,
            reminderEvent2.lastIntervalReminderTimeInMinutes
        )
        assertEquals(reminderEvent1.notes, reminderEvent2.notes)
        assertEquals(reminderEvent1.reminderType, reminderEvent2.reminderType)
    }
}

