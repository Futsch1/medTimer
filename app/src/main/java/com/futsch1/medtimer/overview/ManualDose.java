package com.futsch1.medtimer.overview;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.helpers.DialogHelper;
import com.futsch1.medtimer.helpers.TimeHelper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ManualDose {

    private final Context context;
    private final MedicineRepository medicineRepository;
    private final FragmentActivity activity;
    private final SharedPreferences sharedPreferences;

    public ManualDose(Context context, MedicineRepository medicineRepository, FragmentActivity activity) {
        this.context = context;
        this.medicineRepository = medicineRepository;
        this.activity = activity;
        this.sharedPreferences = context.getSharedPreferences("medtimer.data", Context.MODE_PRIVATE);
    }

    public void logManualDose() {
        List<MedicineWithReminders> medicines = medicineRepository.getMedicines();
        List<ManualDoseEntry> entries = getManualDoseEntries(medicines);

        // But run the actual dialog on the UI thread again
        activity.runOnUiThread(() ->
                new AlertDialog.Builder(context)
                        .setItems(entries.stream().map(e -> e.name).toArray(String[]::new), (dialog, which) ->
                                startLogProcess(entries.get(which)))
                        .setTitle(R.string.tab_medicine)
                        .show());
    }

    @NonNull
    private List<ManualDoseEntry> getManualDoseEntries(List<MedicineWithReminders> medicines) {
        String lastCustomDose = getLastCustomDose();
        List<ManualDoseEntry> entries = new ArrayList<>();
        entries.add(new ManualDoseEntry(context.getString(R.string.custom)));
        if (!lastCustomDose.isBlank()) {
            entries.add(new ManualDoseEntry(lastCustomDose));
        }
        for (MedicineWithReminders medicine : medicines) {
            entries.add(new ManualDoseEntry(medicine.medicine));
        }
        return entries;
    }

    private void startLogProcess(ManualDoseEntry entry) {
        ReminderEvent reminderEvent = new ReminderEvent();
        // Manual dose is not assigned to an existing reminder
        reminderEvent.reminderId = -1;
        reminderEvent.status = ReminderEvent.ReminderStatus.TAKEN;
        reminderEvent.medicineName = entry.name;
        reminderEvent.color = entry.color;
        reminderEvent.useColor = entry.useColor;
        if (reminderEvent.medicineName.equals(context.getString(R.string.custom))) {
            DialogHelper.showTextInputDialog(context, R.string.log_additional_dose, R.string.medicine_name, name -> {
                setLastCustomDose(name);
                reminderEvent.medicineName = name;
                getAmountAndContinue(reminderEvent);
            });
        } else {
            getAmountAndContinue(reminderEvent);
        }
    }

    private String getLastCustomDose() {
        return sharedPreferences.getString("lastCustomDose", "");
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

    private void setLastCustomDose(String lastCustomDose) {
        sharedPreferences.edit().putString("lastCustomDose", lastCustomDose).apply();
    }

    private static class ManualDoseEntry {
        public final String name;
        public final int color;
        public final boolean useColor;

        public ManualDoseEntry(String name) {
            this.name = name;
            this.color = 0;
            this.useColor = false;
        }

        public ManualDoseEntry(Medicine medicine) {
            this.name = medicine.name;
            this.color = medicine.color;
            this.useColor = medicine.useColor;
        }
    }
}
