package com.futsch1.medtimer.medicine

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.FragmentActivity
import android.app.AlertDialog
import android.content.DialogInterface
import com.futsch1.medtimer.overview.ManualDoseListEntryAdapter
import com.futsch1.medtimer.overview.ManualDose
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.DeleteHelper
import com.futsch1.medtimer.helpers.DialogHelper
import com.futsch1.medtimer.helpers.TimeHelper.TimePickerWrapper
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

class LinkedReminderHandling(
    val reminder: Reminder,
    val medicineRepository: MedicineRepository,
    val coroutineScope: CoroutineScope,
    val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    fun addLinkedReminder(fragmentActivity: FragmentActivity) {
        // Fetch medicines off the main thread to avoid Room's main-thread check
        coroutineScope.launch(dispatcher) {
            val medicines = medicineRepository.medicines
            val entries: MutableList<ManualDose.ManualDoseEntry> = ArrayList()
            for (m in medicines) {
                entries.add(ManualDose.ManualDoseEntry(m, null))
            }

            val adapter = ManualDoseListEntryAdapter(fragmentActivity, R.layout.manual_dose_list_entry, entries)

            Handler(Looper.getMainLooper()).post {
                AlertDialog.Builder(fragmentActivity)
                    .setAdapter(adapter) { _: DialogInterface?, which: Int ->
                        val selectedEntry = entries[which]
                        DialogHelper(fragmentActivity).title(R.string.add_linked_reminder)
                            .hint(R.string.create_reminder_dosage_hint)
                            .textSink { amount: String? ->
                                this@LinkedReminderHandling.createReminder(
                                    fragmentActivity,
                                    amount,
                                    selectedEntry.medicineId
                                )
                            }.show()
                    }
                    .setTitle(R.string.add_linked_reminder)
                    .show()
            }
        }
    }

    private fun createReminder(fragmentActivity: FragmentActivity, amount: String?, medicineRelId: Int) {
        val linkedReminder = Reminder(medicineRelId)
        linkedReminder.amount = amount
        linkedReminder.createdTimestamp = Instant.now().toEpochMilli() / 1000
        linkedReminder.cycleStartDay = LocalDate.now().plusDays(1).toEpochDay()
        linkedReminder.instructions = ""
        linkedReminder.linkedReminderId = reminder.reminderId

        TimePickerWrapper(
            fragmentActivity,
            R.string.linked_reminder_delay,
            TimeFormat.CLOCK_24H
        ).show(0, 0) { minutes: Int ->
            linkedReminder.timeInMinutes = minutes
            // Insert reminder off the main thread and then update UI on the main thread
            coroutineScope.launch(dispatcher) {
                val insertedId = medicineRepository.insertReminder(linkedReminder)
                Handler(Looper.getMainLooper()).post {
                    // show a brief confirmation and return
                    try {
                        android.widget.Toast.makeText(fragmentActivity, "Linked reminder created", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {
                    }
                    fragmentActivity.supportFragmentManager.popBackStack()
                }
            }
        }
    }

    fun deleteReminder(context: Context, postYesAction: () -> Unit, postNoAction: () -> Unit) {
        val deleteHelper = DeleteHelper(context)
        deleteHelper.deleteItem(R.string.are_you_sure_delete_reminder, {
            coroutineScope.launch(dispatcher) {
                internalDelete(reminder)
                Handler(Looper.getMainLooper()).post(postYesAction)
            }
        }, { Handler(Looper.getMainLooper()).post(postNoAction) })
    }

    private fun internalDelete(
        reminder: Reminder
    ) {
        val reminders: List<Reminder> =
            medicineRepository.getLinkedReminders(reminder.reminderId)
        for (r in reminders) {
            internalDelete(r)
        }

        medicineRepository.deleteReminder(reminder.reminderId)
    }
}

class LinkedReminderAlgorithms {
    fun sortRemindersList(reminders: List<Reminder>): List<Reminder> {
        return reminders.sortedBy { r -> getTotalTimeInMinutes(r, reminders) }
    }

    private fun getTotalTimeInMinutes(reminder: Reminder, reminders: List<Reminder>): Int {
        var total = if (reminder.reminderType != Reminder.ReminderType.WINDOWED_INTERVAL) reminder.timeInMinutes else reminder.intervalStartTimeOfDay
        for (r in reminders) {
            if (r.reminderId == reminder.linkedReminderId) {
                total += when (r.reminderType) {
                    Reminder.ReminderType.LINKED -> {
                        getTotalTimeInMinutes(r, reminders)
                    }

                    else -> {
                        r.timeInMinutes
                    }
                }
            }
        }
        return total
    }

}