package com.futsch1.medtimer;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;

import java.util.List;

public class MedicineViewModel extends AndroidViewModel {

    public final MedicineRepository medicineRepository;
    private final LiveData<List<MedicineWithReminders>> medicines;

    public MedicineViewModel(Application application) {
        super(application);
        medicineRepository = new MedicineRepository(application);
        medicines = medicineRepository.getLiveMedicines();
    }

    LiveData<List<MedicineWithReminders>> getMedicines() {
        return medicines;
    }

    Medicine getMedicine(int medicineId) {
        return medicineRepository.getMedicine(medicineId);
    }

    void insertMedicine(Medicine medicine) {
        medicineRepository.insertMedicine(medicine);
    }

    void updateMedicine(Medicine medicine) {
        medicineRepository.updateMedicine(medicine);
    }

    public void deleteMedicine(Medicine medicine) {
        medicineRepository.deleteMedicine(medicine);
    }


    public LiveData<List<Reminder>> getReminders(int medicineId) {
        return medicineRepository.getLiveReminders(medicineId);
    }

    public Reminder getReminder(int reminderId) {
        return medicineRepository.getReminder(reminderId);
    }

    void insertReminder(Reminder reminder) {
        medicineRepository.insertReminder(reminder);
    }

    public void updateReminder(Reminder reminder) {
        medicineRepository.updateReminder(reminder);
    }

    public void deleteReminder(Reminder reminder) {
        medicineRepository.deleteReminder(reminder);
    }

    public LiveData<List<ReminderEvent>> getReminderEvents(int limit, long timeStamp) {
        return medicineRepository.getLiveReminderEvents(limit, timeStamp);
    }

    public void updateReminderEvent(ReminderEvent reminderEvent) {
        medicineRepository.updateReminderEvent(reminderEvent);
    }

    public void deleteReminderEvents() {
        medicineRepository.deleteReminderEvents();
    }
}
