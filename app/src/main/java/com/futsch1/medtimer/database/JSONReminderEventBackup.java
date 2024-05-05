package com.futsch1.medtimer.database;

import com.google.gson.GsonBuilder;

import java.util.List;

public class JSONReminderEventBackup extends JSONBackup<ReminderEvent> {

    public JSONReminderEventBackup() {
        super(ReminderEvent.class);
    }

    protected boolean isInvalid(ReminderEvent reminderEvent) {
        return reminderEvent == null || reminderEvent.medicineName == null;
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
