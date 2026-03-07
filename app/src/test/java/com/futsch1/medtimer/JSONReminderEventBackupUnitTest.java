package com.futsch1.medtimer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import android.graphics.Color;

import com.futsch1.medtimer.database.JSONReminderEventBackup;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class JSONReminderEventBackupUnitTest {

    // creates a backup object with a version number and a medicines array
    @Test
    void testBackup() {
        JSONReminderEventBackup jsonReminderEventBackup = new JSONReminderEventBackup();
        List<ReminderEvent> reminderEvents = new ArrayList<>();
        ReminderEvent reminderEvent = new ReminderEvent();
        reminderEvent.setMedicineName("Medicine A");
        reminderEvent.setColor(Color.RED);
        reminderEvent.setUseColor(true);
        reminderEvent.setAmount("1");
        reminderEvent.setStatus(ReminderEvent.ReminderStatus.TAKEN);
        reminderEvent.setRemindedTimestamp(1);
        reminderEvent.setProcessedTimestamp(2);
        reminderEvent.setReminderId(3);
        reminderEvent.setIconId(4);
        List<String> tags = new ArrayList<>();
        tags.add("Tag A");
        reminderEvent.setTags(tags);
        reminderEvent.setLastIntervalReminderTimeInMinutes(12);
        reminderEvent.setNotes("Notes");
        reminderEvent.setReminderType(Reminder.ReminderType.LINKED);
        reminderEvents.add(reminderEvent);

        String result = jsonReminderEventBackup.createBackupAsString(1, reminderEvents);

        assertNotNull(result);

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
}""", result);
        // @formatter:on

        List<ReminderEvent> parsedReminders = jsonReminderEventBackup.parseBackup(result);
        assertNotNull(parsedReminders);
        compareListReminderEvents(parsedReminders, reminderEvents);
    }

    private void compareListReminderEvents(List<ReminderEvent> actual, List<ReminderEvent> expected) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < actual.size(); i++) {
            compareReminderEvent(actual.get(i), expected.get(i));
        }
    }

    private void compareReminderEvent(ReminderEvent reminderEvent1, ReminderEvent reminderEvent2) {
        assertEquals(reminderEvent1.getMedicineName(), reminderEvent2.getMedicineName());
        assertEquals(reminderEvent1.getColor(), reminderEvent2.getColor());
        assertEquals(reminderEvent1.getUseColor(), reminderEvent2.getUseColor());
        assertEquals(reminderEvent1.getAmount(), reminderEvent2.getAmount());
        assertEquals(reminderEvent1.getStatus(), reminderEvent2.getStatus());
        assertEquals(reminderEvent1.getRemindedTimestamp(), reminderEvent2.getRemindedTimestamp());
        assertEquals(reminderEvent1.getProcessedTimestamp(), reminderEvent2.getProcessedTimestamp());
        assertEquals(reminderEvent1.getReminderId(), reminderEvent2.getReminderId());
        assertEquals(reminderEvent1.getIconId(), reminderEvent2.getIconId());
        assertEquals(reminderEvent1.getTags(), reminderEvent2.getTags());
        assertEquals(reminderEvent1.getLastIntervalReminderTimeInMinutes(), reminderEvent2.getLastIntervalReminderTimeInMinutes());
        assertEquals(reminderEvent1.getNotes(), reminderEvent2.getNotes());
        assertEquals(reminderEvent1.getReminderType(), reminderEvent2.getReminderType());
    }
}

