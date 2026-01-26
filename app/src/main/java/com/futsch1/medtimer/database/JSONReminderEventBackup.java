package com.futsch1.medtimer.database;

import com.google.gson.GsonBuilder;

import java.util.List;

public class JSONReminderEventBackup extends JSONBackup<ReminderEvent> {

    public JSONReminderEventBackup() {
        super(ReminderEvent.class);
    }

    @Override
    protected GsonBuilder registerTypeAdapters(GsonBuilder builder) {
        return builder
                .registerTypeAdapter(ReminderEvent.class, new FullDeserialize<ReminderEvent>());
    }

    protected boolean isInvalid(ReminderEvent reminderEvent) {
        return reminderEvent == null;
    }

    public void applyBackup(List<ReminderEvent> listOfReminderEvents, MedicineRepository medicineRepository) {
        medicineRepository.deleteReminderEvents();

        listOfReminderEvents.forEach(medicineRepository::insertReminderEvent);
    }
}
