package com.futsch1.medtimer.overview;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.helpers.DialogHelper;
import com.futsch1.medtimer.helpers.TimeHelper;

import java.time.Instant;
import java.time.LocalDateTime;

public class ManualDose {

    private final Context context;
    private final MedicineRepository medicineRepository;
    private final FragmentActivity activity;

    public ManualDose(Context context, MedicineRepository medicineRepository, FragmentActivity activity) {
        this.context = context;
        this.medicineRepository = medicineRepository;
        this.activity = activity;
    }

    public void logManualDose(@Nullable Medicine medicine) {
        ReminderEvent reminderEvent = new ReminderEvent();
        // Manual dose is not assigned to an existing reminder
        reminderEvent.reminderId = -1;
        reminderEvent.status = ReminderEvent.ReminderStatus.TAKEN;
        if (medicine != null) {
            reminderEvent.medicineName = medicine.name;
            reminderEvent.color = medicine.color;
            reminderEvent.useColor = medicine.useColor;
            getAmountAndContinue(reminderEvent);
        } else {
            reminderEvent.color = 0;
            reminderEvent.useColor = false;
            DialogHelper.showTextInputDialog(context, R.string.log_additional_dose, R.string.medicine_name, name -> {
                reminderEvent.medicineName = name;
                getAmountAndContinue(reminderEvent);
            });

        }
    }

    private void getAmountAndContinue(ReminderEvent reminderEvent) {
        DialogHelper.showTextInputDialog(context, R.string.log_additional_dose, R.string.dosage, amount -> {
            reminderEvent.amount = amount;
            getTimeAndLog(reminderEvent);
        });
    }

    private void getTimeAndLog(ReminderEvent reminderEvent) {
        LocalDateTime localDateTime = LocalDateTime.now();
        TimeHelper.TimePickerWrapper timePicker = new TimeHelper.TimePickerWrapper(activity);
        timePicker.show(localDateTime.getHour(), localDateTime.getMinute(), minutes -> {
            reminderEvent.remindedTimestamp = TimeHelper.instantFromTodayMinutes(minutes).toEpochMilli() / 1000;
            reminderEvent.processedTimestamp = Instant.now().toEpochMilli() / 1000;

            medicineRepository.insertReminderEvent(reminderEvent);
        });
    }
}
