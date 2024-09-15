package com.futsch1.medtimer;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MedicineViewModel extends AndroidViewModel {

    public final MedicineRepository medicineRepository;
    private final LiveData<List<MedicineWithReminders>> medicines;

    public MedicineViewModel(Application application) {
        super(application);
        medicineRepository = new MedicineRepository(application);
        medicines = medicineRepository.getLiveMedicines();
    }

    public LiveData<List<MedicineWithReminders>> getMedicines() {
        return medicines;
    }

    public Medicine getMedicine(int medicineId) {
        return medicineRepository.getMedicine(medicineId);
    }

    public int insertMedicine(Medicine medicine) {
        return (int) medicineRepository.insertMedicine(medicine);
    }

    public void updateMedicine(Medicine medicine) {
        medicineRepository.updateMedicine(medicine);
    }

    public void deleteMedicine(Medicine medicine) {
        medicineRepository.deleteMedicine(medicine);
    }


    public LiveData<List<Reminder>> getLiveReminders(int medicineId) {
        return medicineRepository.getLiveReminders(medicineId);
    }

    public List<Reminder> getReminders(int medicineId) {
        return medicineRepository.getReminders(medicineId);
    }

    public Reminder getReminder(int reminderId) {
        return medicineRepository.getReminder(reminderId);
    }

    public void insertReminder(Reminder reminder) {
        medicineRepository.insertReminder(reminder);
    }

    public void updateReminder(Reminder reminder) {
        medicineRepository.updateReminder(reminder);
    }

    public void deleteReminder(Reminder reminder) {
        medicineRepository.deleteReminder(reminder);
    }

    public LiveData<List<ReminderEvent>> getLiveReminderEvents(int limit, long timeStamp, boolean withDeleted) {
        return medicineRepository.getLiveReminderEvents(limit, timeStamp, withDeleted);
    }

    public void deleteReminderEvents() {
        medicineRepository.deleteReminderEvents();
    }

    public void deleteAll() {
        medicineRepository.deleteAll();
    }

    public @Nullable ReminderEvent getReminderEvent(int reminderEventId) {
        return medicineRepository.getReminderEvent(reminderEventId);
    }

    public void updateReminderEvent(ReminderEvent reminderEvent) {
        medicineRepository.updateReminderEvent(reminderEvent);
    }
}
