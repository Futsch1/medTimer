package com.futsch1.medtimer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import android.graphics.Color;

import com.futsch1.medtimer.database.JSONReminderEventBackup;
import com.futsch1.medtimer.database.ReminderEvent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class JSONReminderEventBackupUnitTest {

    // creates a backup object with a version number and a medicines array
    @Test
    void test_backup() {
        JSONReminderEventBackup jsonReminderEventBackup = new JSONReminderEventBackup();
        List<ReminderEvent> reminderEvents = new ArrayList<>();
        ReminderEvent reminderEvent = new ReminderEvent();
        reminderEvent.medicineName = "Medicine A";
        reminderEvent.color = Color.RED;
        reminderEvent.useColor = true;
        reminderEvent.amount = "1";
        reminderEvent.status = ReminderEvent.ReminderStatus.TAKEN;
        reminderEvent.remindedTimestamp = 1;
        reminderEvent.processedTimestamp = 2;
        reminderEvent.reminderId = 3;
        reminderEvent.iconId = 4;
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
      "iconId": 4
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
        assertEquals(reminderEvent1.medicineName, reminderEvent2.medicineName);
        assertEquals(reminderEvent1.color, reminderEvent2.color);
        assertEquals(reminderEvent1.useColor, reminderEvent2.useColor);
        assertEquals(reminderEvent1.amount, reminderEvent2.amount);
        assertEquals(reminderEvent1.status, reminderEvent2.status);
        assertEquals(reminderEvent1.remindedTimestamp, reminderEvent2.remindedTimestamp);
        assertEquals(reminderEvent1.processedTimestamp, reminderEvent2.processedTimestamp);
        assertEquals(reminderEvent1.reminderId, reminderEvent2.reminderId);
        assertEquals(reminderEvent1.iconId, reminderEvent2.iconId);
    }
}

