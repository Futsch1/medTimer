package com.futsch1.medtimer.exporters

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.medicine.LinkedReminderAlgorithms
import com.wwdablu.soumya.simplypdf.SimplyPdfDocument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class PDFMedicineExport(val medicines: List<FullMedicine>, fragmentManager: FragmentManager, val context: Context) : Export(fragmentManager) {
    @OptIn(ExperimentalTime::class)
    override fun exportInternal(file: File) {
        val simplyPdfDocument: SimplyPdfDocument = getDocument(context, file)

        simplyPdfDocument.text.write(TimeHelper.toLocalizedDatetimeString(context, Clock.System.now().epochSeconds) + "\n", standardTextProperties)

        for (medicine in medicines) {
            simplyPdfDocument.text.write(medicine.medicine.name, biggestBoldProperties)
            exportMedicine(simplyPdfDocument, medicine)
        }

        CoroutineScope(Dispatchers.IO).launch {
            simplyPdfDocument.finish()
        }
    }

    private fun exportMedicine(simplyPdfDocument: SimplyPdfDocument, medicine: FullMedicine) {
        val reminders = LinkedReminderAlgorithms().sortRemindersList(medicine.reminders)
        for (reminder in reminders) {
            val firstLine =
                context.getString(R.string.dosage) + ": " + if (reminder.variableAmount) context.getString(R.string.variable_amount) else reminder.amount
            val secondLine = getExportReminderSummary(context, reminder)

            simplyPdfDocument.text.write(firstLine + "\n" + secondLine, bulletTextProperties)
        }
    }

    override fun getExtension(): String {
        return "pdf"
    }

    override fun getType(): String {
        return "Medicine"
    }
}