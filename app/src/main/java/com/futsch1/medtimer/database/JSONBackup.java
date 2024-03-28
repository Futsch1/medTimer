package com.futsch1.medtimer.database;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class JSONBackup {
    private static final int VERSION = 1;
    private static final String MEDICINES = "medicines";
    private static final String MEDICINE_NAME = "name";
    private static final String MEDICINE_USE_COLOR = "useColor";
    private static final String MEDICINE_COLOR = "color";
    private static final String MEDICINE_REMINDERS = "reminders";
    private static final String REMINDER_TIME = "time";
    private static final String REMINDER_AMOUNT = "amount";
    private static final String REMINDER_DAYS_BETWEEN = "daysBetweenReminders";
    private static final String REMINDER_INSTRUCTIONS = "instructions";

    private final JSONObject backupObject;

    public JSONBackup() {
        backupObject = new JSONObject();
    }

    public String createBackup(List<MedicineWithReminders> medicinesWithReminders) {
        try {
            backupObject.put("version", VERSION);
            JSONArray medicinesObject = new JSONArray();
            for (MedicineWithReminders medicineWithReminders : medicinesWithReminders) {
                medicinesObject.put(backupMedicineWithReminders(medicineWithReminders));
            }
            backupObject.put(MEDICINES, medicinesObject);

            return backupObject.toString(4);
        } catch (JSONException e) {
            return null;
        }
    }

    public JSONObject backupMedicineWithReminders(MedicineWithReminders medicineWithReminders) throws JSONException {
        JSONObject medicineObject = new JSONObject();

        medicineObject.put(MEDICINE_NAME, medicineWithReminders.medicine.name);
        medicineObject.put(MEDICINE_USE_COLOR, medicineWithReminders.medicine.useColor);
        medicineObject.put(MEDICINE_COLOR, medicineWithReminders.medicine.color);

        JSONArray reminders = new JSONArray();
        for (Reminder reminder : medicineWithReminders.reminders) {
            reminders.put(backupReminder(reminder));
        }
        medicineObject.put(MEDICINE_REMINDERS, reminders);

        return medicineObject;
    }

    private JSONObject backupReminder(Reminder reminder) throws JSONException {
        JSONObject reminderObject = new JSONObject();
        reminderObject.put(REMINDER_TIME, reminder.timeInMinutes);
        reminderObject.put(REMINDER_AMOUNT, reminder.amount);
        reminderObject.put(REMINDER_DAYS_BETWEEN, reminder.daysBetweenReminders);
        reminderObject.put(REMINDER_INSTRUCTIONS, reminder.instructions);
        return reminderObject;
    }
}
