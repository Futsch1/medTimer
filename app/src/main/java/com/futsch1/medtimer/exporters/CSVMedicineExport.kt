package com.futsch1.medtimer.exporters

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.helpers.TableHelper
import com.futsch1.medtimer.medicine.LinkedReminderAlgorithms
import java.io.File
import java.io.FileWriter
import java.io.IOException

class CSVMedicineExport(val medicines: List<FullMedicine>, fragmentManager: FragmentManager, val context: Context) : Export(fragmentManager) {
    @Throws(ExporterException::class)
    public override fun exportInternal(file: File) {
        try {
            FileWriter(file).use { csvFile ->
                val headerTexts = TableHelper.getTableHeadersForMedicationExport(context)
                csvFile.write(java.lang.String.join(";", headerTexts) + "\n")
                for (medicine in medicines) {
                    exportMedicine(csvFile, medicine)
                }
            }
        } catch (_: IOException) {
            throw ExporterException()
        }
    }

    private fun exportMedicine(csvFile: FileWriter, medicine: FullMedicine) {
        val reminders = LinkedReminderAlgorithms().sortRemindersList(medicine.reminders)
        for (reminder in reminders) {
            val line = String.format(
                "%s;%s;%s\n",
                medicine.medicine.name,
                if (reminder.variableAmount) context.getString(R.string.variable_amount) else reminder.amount,
                getExportReminderSummary(context, reminder)
            )
            csvFile.write(line)
        }
        if (reminders.isEmpty()) {
            csvFile.write(String.format("%s;%s;%s\n", medicine.medicine.name, "", context.getString(R.string.no_reminders)))
        }
    }


    override fun getExtension(): String {
        return "csv"
    }

    override fun getType(): String {
        return "Medicine"
    }

}