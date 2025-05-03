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
        val adapter = ManualDoseListEntryAdapter(context, R.layout.manual_dose_list_entry, entries)

        // But run the actual dialog on the UI thread again
        activity.runOnUiThread {
            AlertDialog.Builder(context)
                .setAdapter(adapter) { _: DialogInterface?, which: Int ->
                    startLogProcess(entries[which])
                }
                .setTitle(R.string.log_additional_dose)
                .show()
        }
    }

    private fun getManualDoseEntries(medicines: List<FullMedicine>): List<ManualDoseEntry> {
        val entries: MutableList<ManualDoseEntry> = ArrayList()
        entries.add(ManualDoseEntry(context.getString(R.string.custom)))
        addCustomDoses(entries)
        for (medicine in medicines) {
            val entry = ManualDoseEntry(medicine, null)
            entries.add(entry)
            addInactiveReminders(medicine, entries)
        }
        return entries
    }

    private fun addCustomDoses(
        entries: MutableList<ManualDoseEntry>
    ) {
        val lastCustomDose = lastCustomDose
        if (lastCustomDose.first != null && lastCustomDose.first!!.isNotBlank()) {
            entries.add(ManualDoseEntry(lastCustomDose.first!!))
            if (lastCustomDose.second != null && lastCustomDose.second!!.isNotBlank()) {
                entries.add(ManualDoseEntry(lastCustomDose.first!!, lastCustomDose.second))
            }
        }
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
                    reminderEvent.medicineName = name
                    entry.baseName = name!!
                    getAmountAndContinue(reminderEvent, entry)
                }.show()
        } else {
            if (entry.amount == null || entry.medicineId == -1) {
                getAmountAndContinue(reminderEvent, entry)
            } else {
                getTimeAndLog(reminderEvent, entry.medicineId)
            }
        }
    }

    private var lastCustomDose: Pair<String?, String?>
        get() {
            val name = sharedPreferences.getString("lastCustomDose", "")
            val amount = sharedPreferences.getString("lastCustomDoseAmount", "")
            return Pair(name, amount)
        }
        set(lastCustomDose) {
            sharedPreferences.edit { putString("lastCustomDose", lastCustomDose.first); putString("lastCustomDoseAmount", lastCustomDose.second) }
        }

    private fun getAmountAndContinue(reminderEvent: ReminderEvent, entry: ManualDoseEntry) {
        var dialog = DialogHelper(context).title(R.string.log_additional_dose).hint(R.string.dosage)
            .textSink { amount: String? ->
                reminderEvent.amount = amount
                if (entry.medicineId == -1) {
                    lastCustomDose = Pair(entry.baseName, amount)
                }
                getTimeAndLog(reminderEvent, entry.medicineId)
            }
        if (entry.amount != null && !entry.amount.isBlank()) {
            dialog = dialog.initialText(entry.amount)
        }
        dialog.show()
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

    class ManualDoseEntry {
        var baseName: String
        lateinit var name: String
        val color: Int
        val useColor: Boolean
        val amount: String?
        val iconId: Int
        val medicineId: Int
        val tags: List<String>

        constructor(name: String, amount: String? = null) {
            this.baseName = name
            this.color = 0
            this.useColor = false
            this.amount = amount
            this.iconId = 0
            this.medicineId = -1
            this.tags = ArrayList()
            amendName()
        }

        constructor(medicine: FullMedicine, amount: String?) {
            this.baseName = medicine.medicine.name
            this.color = medicine.medicine.color
            this.useColor = medicine.medicine.useColor
            this.amount = amount
            this.iconId = medicine.medicine.iconId
            this.medicineId = medicine.medicine.medicineId
            this.tags = medicine.tags.stream().map { t -> t.name }.collect(Collectors.toList())
            amendName()
        }

        private fun amendName() {
            if (amount != null) {
                this.name = this.baseName + " (" + amount + ")"
            } else {
                this.name = this.baseName
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val that = other as ManualDoseEntry
            return !(name != that.name || amount != that.amount)
        }

        override fun hashCode(): Int {
            return super.hashCode()
        }
    }

    companion object {
        private fun addInactiveReminders(
            medicine: FullMedicine,
            entries: MutableList<ManualDoseEntry>
        ) {
            for (reminder in medicine.reminders) {
                val entry = ManualDoseEntry(medicine, reminder.amount)
                if (!isReminderActive(reminder) && !entries.contains(entry)) {
                    entries.add(entry)
                }
            }
        }
    }
}
