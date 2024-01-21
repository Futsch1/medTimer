package com.futsch1.medtimer.database;

import java.util.List;

public class MedicineRepository {
    List<MedicineWithReminders> medicineWithReminders;

    public MedicineRepository(MedicineDao dao) {
        this.medicineWithReminders = dao.getMedicines();
    }
}
