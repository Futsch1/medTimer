package com.futsch1.medtimer.logic;

import java.util.List;

public class Medicines {
    List<Medicine> medicines;

    public Medicines(MedicineDao dao) {
        this.medicines = dao.getAll();
    }
}
