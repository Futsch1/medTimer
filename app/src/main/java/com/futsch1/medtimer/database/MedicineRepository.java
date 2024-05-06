package com.futsch1.medtimer.database;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MedicineRepository {

    private final MedicineDao medicineDao;
    private final MedicineRoomDatabase database;

    public MedicineRepository(Application application) {
        database = MedicineRoomDatabase.getDatabase(application);
        medicineDao = database.medicineDao();
    }

    public int getVersion() {
        return database.getVersion();
    }

    public LiveData<List<MedicineWithReminders>> getLiveMedicines() {
        return medicineDao.getLiveMedicines();
    }

    public List<MedicineWithReminders> getMedicines() {
        return medicineDao.getMedicines();
    }

    public Medicine getMedicine(int medicineId) {
        return medicineDao.getMedicine(medicineId);
    }


    public LiveData<List<Reminder>> getLiveReminders(int medicineId) {
        return medicineDao.getReminders(medicineId);
    }

    public Reminder getReminder(int reminderId) {
        return medicineDao.getReminder(reminderId);
    }

    public LiveData<List<ReminderEvent>> getLiveReminderEvents(int limit, long timeStamp) {
        if (limit == 0) {
            return medicineDao.getLiveReminderEvents(timeStamp);
        } else {
            return medicineDao.getReminderEvents(limit);
        }
    }

    public List<ReminderEvent> getAllReminderEvents() {
        return medicineDao.getReminderEvents(0L);
    }

    public List<ReminderEvent> getLastDaysReminderEvents(int days) {
        return medicineDao.getReminderEvents(Instant.now().toEpochMilli() / 1000 - ((long) days * 24 * 60 * 60));
    }

    public long insertMedicine(Medicine medicine) {
        return internalInsert(medicine, medicineDao::insertMedicine);
    }

    private <T> long internalInsert(T insertType, Insert<T> f) {
        try {
            return MedicineRoomDatabase.databaseWriteExecutor.submit(() -> f.insert(insertType)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e1) {
            //noinspection CallToPrintStackTrace
            e1.printStackTrace();
        }
        return 0;
    }

    public void updateMedicine(Medicine medicine) {
        MedicineRoomDatabase.databaseWriteExecutor.execute(() -> medicineDao.updateMedicine(medicine));
    }

    public void deleteMedicine(Medicine medicine) {
        MedicineRoomDatabase.databaseWriteExecutor.execute(() -> medicineDao.deleteMedicine(medicine));
    }

    public void insertReminder(Reminder reminder) {
        MedicineRoomDatabase.databaseWriteExecutor.execute(() -> medicineDao.insertReminder(reminder));
    }

    public void updateReminder(Reminder reminder) {
        MedicineRoomDatabase.databaseWriteExecutor.execute(() -> medicineDao.updateReminder(reminder));
    }

    public void deleteReminder(Reminder reminder) {
        MedicineRoomDatabase.databaseWriteExecutor.execute(() -> medicineDao.deleteReminder(reminder));
    }

    public long insertReminderEvent(ReminderEvent reminderEvent) {
        return internalInsert(reminderEvent, medicineDao::insertReminderEvent);
    }

    public ReminderEvent getReminderEvent(int reminderEventId) {
        return medicineDao.getReminderEvent(reminderEventId);
    }

    public void updateReminderEvent(ReminderEvent reminderEvent) {
        MedicineRoomDatabase.databaseWriteExecutor.execute(() -> medicineDao.updateReminderEvent(reminderEvent));
    }

    public void deleteReminderEvents() {
        MedicineRoomDatabase.databaseWriteExecutor.execute(medicineDao::deleteReminderEvents);
    }

    public void deleteReminders() {
        MedicineRoomDatabase.databaseWriteExecutor.execute(medicineDao::deleteReminders);
    }

    public void deleteMedicines() {
        MedicineRoomDatabase.databaseWriteExecutor.execute(medicineDao::deleteMedicines);
    }

    public void deleteAll() {
        try {
            MedicineRoomDatabase.databaseWriteExecutor.submit(database::clearAllTables).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e1) {
            //noinspection CallToPrintStackTrace
            e1.printStackTrace();
        }
    }

    interface Insert<T> {
        long insert(T item);
    }
}
