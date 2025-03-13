package com.futsch1.medtimer.overview

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.DialogHelper
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.helpers.TimeHelper.TimePickerWrapper
import com.futsch1.medtimer.helpers.isReminderActive
import com.futsch1.medtimer.reminders.ReminderProcessor
import java.time.LocalDateTime
import java.util.stream.Collectors

class ManualDose(
    private val context: Context,
    private val medicineRepository: MedicineRepository,
    private val activity: FragmentActivity
) {
    @Suppress("kotlin:S6291") // Preferences do not contain sensitive date
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("medtimer.data", Context.MODE_PRIVATE)

    fun logManualDose() {
        val medicines = medicineRepository.medicines
        val entries = getManualDoseEntries(medicines)
        val names =
            entries.stream().map { e: ManualDoseEntry -> e.name }.collect(Collectors.toList())
                .toTypedArray()

        // But run the actual dialog on the UI thread again
        activity.runOnUiThread {
            AlertDialog.Builder(context)
                .setItems(names) { _: DialogInterface?, which: Int -> startLogProcess(entries[which]) }
                .setTitle(R.string.tab_medicine)
                .show()
        }
    }

    private fun getManualDoseEntries(medicines: List<FullMedicine>): List<ManualDoseEntry> {
        val lastCustomDose = lastCustomDose!!
        val entries: MutableList<ManualDoseEntry> = ArrayList()
        entries.add(ManualDoseEntry(context.getString(R.string.custom)))
        if (lastCustomDose.isNotBlank()) {
            entries.add(ManualDoseEntry(lastCustomDose))
        }
        for (medicine in medicines) {
            entries.add(ManualDoseEntry(medicine, null))
            addInactiveReminders(medicine, entries)
        }
        return entries
    }

    private fun startLogProcess(entry: ManualDoseEntry) {
        val reminderEvent = ReminderEvent()
        // Manual dose is not assigned to an existing reminder
        reminderEvent.reminderId = -1
        reminderEvent.status = ReminderEvent.ReminderStatus.TAKEN
        reminderEvent.medicineName = entry.name
        reminderEvent.color = entry.color
        reminderEvent.useColor = entry.useColor
        reminderEvent.iconId = entry.iconId
        reminderEvent.tags = entry.tags
        if (reminderEvent.medicineName == context.getString(R.string.custom)) {
            DialogHelper(context).title(R.string.log_additional_dose).hint(R.string.medicine_name)
                .textSink { name: String? ->
                    lastCustomDose = name
                    reminderEvent.medicineName = name
                    getAmountAndContinue(reminderEvent, -1)
                }.show()
        } else {
            if (entry.amount == null) {
                getAmountAndContinue(reminderEvent, entry.medicineId)
            } else {
                reminderEvent.amount = entry.amount
                getTimeAndLog(reminderEvent, entry.medicineId)
            }
        }
    }

    private var lastCustomDose: String?
        get() = sharedPreferences.getString("lastCustomDose", "")
        set(lastCustomDose) {
            sharedPreferences.edit { putString("lastCustomDose", lastCustomDose) }
        }

    private fun getAmountAndContinue(reminderEvent: ReminderEvent, medicineId: Int) {
        DialogHelper(context).title(R.string.log_additional_dose).hint(R.string.dosage)
            .textSink { amount: String? ->
                reminderEvent.amount = amount
                getTimeAndLog(reminderEvent, medicineId)
            }.show()
    }

    private fun getTimeAndLog(reminderEvent: ReminderEvent, medicineId: Int) {
        val localDateTime = LocalDateTime.now()
        val timePicker = TimePickerWrapper(activity)
        timePicker.show(localDateTime.hour, localDateTime.minute) { minutes: Int ->
            reminderEvent.remindedTimestamp =
                TimeHelper.instantFromTodayMinutes(minutes).epochSecond
            reminderEvent.processedTimestamp = reminderEvent.remindedTimestamp
            medicineRepository.insertReminderEvent(reminderEvent)
        }
        if (medicineId != -1) {
            ReminderProcessor.requestStockHandling(context, reminderEvent.amount!!, medicineId)
        }
    }

    private class ManualDoseEntry {
        val name: String
        val color: Int
        val useColor: Boolean
        val amount: String?
        val iconId: Int
        val medicineId: Int
        val tags: List<String>

        constructor(name: String) {
            this.name = name
            this.color = 0
            this.useColor = false
            this.amount = null
            this.iconId = 0
            this.medicineId = -1
            this.tags = ArrayList()
        }

        constructor(medicine: FullMedicine, amount: String?) {
            if (amount != null) {
                this.name = medicine.medicine.name + " (" + amount + ")"
            } else {
                this.name = medicine.medicine.name
            }
            this.color = medicine.medicine.color
            this.useColor = medicine.medicine.useColor
            this.amount = amount
            this.iconId = medicine.medicine.iconId
            this.medicineId = medicine.medicine.medicineId
            this.tags = medicine.tags.stream().map { t -> t.name }.collect(Collectors.toList())
        }
    }

    companion object {
        private fun addInactiveReminders(
            medicine: FullMedicine,
            entries: MutableList<ManualDoseEntry>
        ) {
            for (reminder in medicine.reminders) {
                if (!isReminderActive(reminder)) {
                    entries.add(ManualDoseEntry(medicine, reminder.amount))
                }
            }
        }
    }
}
