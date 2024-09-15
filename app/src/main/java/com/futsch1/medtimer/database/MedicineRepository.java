package com.futsch1.medtimer.database;

import android.app.Application;

import androidx.lifecycle.LiveData;

import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class MedicineRepository {

    private final MedicineDao medicineDao;
    private final MedicineRoomDatabase database;
    private final List<ReminderEvent.ReminderStatus> allStatusValues = Arrays.stream(new ReminderEvent.ReminderStatus[]{ReminderEvent.ReminderStatus.DELETED, ReminderEvent.ReminderStatus.RAISED, ReminderEvent.ReminderStatus.SKIPPED, ReminderEvent.ReminderStatus.TAKEN}).
            collect(Collectors.toList());
    private final List<ReminderEvent.ReminderStatus> statusValuesWithoutDelete = Arrays.stream(new ReminderEvent.ReminderStatus[]{ReminderEvent.ReminderStatus.RAISED, ReminderEvent.ReminderStatus.SKIPPED, ReminderEvent.ReminderStatus.TAKEN}).
            collect(Collectors.toList());

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
        return medicineDao.getLiveReminders(medicineId);
    }

    public List<Reminder> getReminders(int medicineId) {
        return medicineDao.getReminders(medicineId);
    }

    public Reminder getReminder(int reminderId) {
        return medicineDao.getReminder(reminderId);
    }

    public LiveData<List<ReminderEvent>> getLiveReminderEvents(int limit, long timeStamp, boolean withDeleted) {
        if (limit == 0) {
            return medicineDao.getLiveReminderEventsStartingFrom(timeStamp, withDeleted ? allStatusValues : statusValuesWithoutDelete);
        } else {
            return medicineDao.getLiveReminderEvents(limit, withDeleted ? allStatusValues : statusValuesWithoutDelete);
        }
    }

    public List<ReminderEvent> getAllReminderEventsWithoutDeleted() {
        return medicineDao.getLiveReminderEvents(0L, statusValuesWithoutDelete);
    }

    public List<ReminderEvent> getLastDaysReminderEvents(int days) {
        return medicineDao.getLiveReminderEvents(Instant.now().toEpochMilli() / 1000 - ((long) days * 24 * 60 * 60), allStatusValues);
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

    public @Nullable ReminderEvent getReminderEvent(int reminderEventId) {
        return medicineDao.getReminderEvent(reminderEventId);
    }

    public void updateReminderEvent(ReminderEvent reminderEvent) {
        MedicineRoomDatabase.databaseWriteExecutor.execute(() -> medicineDao.updateReminderEvent(reminderEvent));
    }

    public void deleteAll() {
        deleteReminders();
        deleteMedicines();
        deleteReminderEvents();
    }

    public void deleteReminders() {
        MedicineRoomDatabase.databaseWriteExecutor.execute(medicineDao::deleteReminders);
    }

    public void deleteMedicines() {
        MedicineRoomDatabase.databaseWriteExecutor.execute(medicineDao::deleteMedicines);
    }

    public void deleteReminderEvents() {
        MedicineRoomDatabase.databaseWriteExecutor.execute(medicineDao::deleteReminderEvents);
    }

    interface Insert<T> {
        long insert(T item);
    }
}
