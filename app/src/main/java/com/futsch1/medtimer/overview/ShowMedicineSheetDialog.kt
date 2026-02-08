package com.futsch1.medtimer.overview

import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.helpers.reminderSummary
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.sidesheet.SideSheetDialog
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShowMedicineSheetDialog(
    val activity: FragmentActivity,
    val reminderId: Int,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) {
    init {
        activity.lifecycleScope.launch(ioDispatcher) {
            val medicineRepository = MedicineRepository(activity.application)
            val reminder = medicineRepository.getReminder(reminderId)
            val fullMedicine = medicineRepository.getMedicine(reminder!!.medicineRelId)!!
            launch(mainDispatcher) {
                showMedicine(fullMedicine, reminder)
            }
        }
    }

    private fun showMedicine(fullMedicine: FullMedicine, reminder: Reminder) {

        val showMedicineSheetDialog =
            if (activity.resources.configuration.orientation == ORIENTATION_PORTRAIT) BottomSheetDialog(activity) else SideSheetDialog(activity)
        showMedicineSheetDialog.setContentView(R.layout.sheet_show_medicine)
        showMedicineSheetDialog.findViewById<TextView>(R.id.medicineName)?.text = MedicineHelper.getMedicineNameWithStockText(activity, fullMedicine)

        val datesTextView = showMedicineSheetDialog.findViewById<TextView>(R.id.medicineDates)
        datesTextView?.text = MedicineHelper.getDatesText(activity, fullMedicine)
        checkIfTextElseGone(datesTextView)

        val notesTextView = showMedicineSheetDialog.findViewById<TextView>(R.id.medicineNotes)
        notesTextView?.text = fullMedicine.medicine.notes
        checkIfTextElseGone(notesTextView)

        val reminderTextView = showMedicineSheetDialog.findViewById<TextView>(R.id.reminderText)
        reminderTextView?.text = reminderSummary(reminder, activity)
        reminderTextView?.setCompoundDrawablesWithIntrinsicBounds(reminder.reminderType.icon, 0, 0, 0)

        showMedicineSheetDialog.show()
    }

    private fun checkIfTextElseGone(textView: TextView?) {
        if (textView?.text?.isEmpty() == true) {
            textView.visibility = View.GONE
        }
    }
}