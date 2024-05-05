package com.futsch1.medtimer.database;

import androidx.annotation.Nullable;

import com.google.gson.GsonBuilder;

import java.util.List;

public class JSONReminderEventBackup extends JSONBackup<ReminderEvent> {

    public JSONReminderEventBackup() {
        super(ReminderEvent.class);
    }

    @Override
    public @Nullable List<ReminderEvent> parseBackup(String jsonFile) {
        List<ReminderEvent> reminderEvents = super.parseBackup(jsonFile);
        if (reminderEvents != null) {
            for (ReminderEvent reminderEvent : reminderEvents) {
                if (reminderEvent == null || reminderEvent.medicineName == null) {
                    return null;
                }
            }
        }
        return reminderEvents;
    }

    @Override
    protected GsonBuilder registerTypeAdapters(GsonBuilder builder) {
        return builder
                .registerTypeAdapter(ReminderEvent.class, new FullDeserialize<ReminderEvent>());
    }

    public void applyBackup(List<ReminderEvent> listOfReminderEvents, MedicineRepository medicineRepository) {
        medicineRepository.deleteReminderEvents();

        listOfReminderEvents.forEach(medicineRepository::insertReminderEvent);
    }
}
