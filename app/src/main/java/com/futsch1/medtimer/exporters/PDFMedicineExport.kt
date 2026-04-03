package com.futsch1.medtimer.exporters

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.ReminderSummaryFormatter
import com.futsch1.medtimer.helpers.TimeFormatter
import com.futsch1.medtimer.helpers.getActiveReminders
import com.futsch1.medtimer.medicine.LinkedReminderAlgorithms
import com.wwdablu.soumya.simplypdf.SimplyPdfDocument
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class PDFMedicineExport @AssistedInject constructor(
    @Assisted val medicines: List<FullMedicine>,
    @Assisted fragmentManager: FragmentManager,
    @param:ApplicationContext val context: Context,
    private val timeFormatter: TimeFormatter,
    private val reminderSummaryFormatter: ReminderSummaryFormatter,
    private val linkedReminderAlgorithms: LinkedReminderAlgorithms
) : Export(fragmentManager) {

    @AssistedFactory
    fun interface Factory {
        fun create(medicines: List<FullMedicine>, fragmentManager: FragmentManager): PDFMedicineExport
    }
    @OptIn(ExperimentalTime::class)
    override suspend fun exportInternal(file: File) {
        val simplyPdfDocument: SimplyPdfDocument = getDocument(context, file)

        simplyPdfDocument.text.write(context.getString(R.string.app_name) + " - " + context.getString(R.string.medicine_data), biggestBoldProperties)
        simplyPdfDocument.text.write(timeFormatter.secondsSinceEpochToDateTimeString(Clock.System.now().epochSeconds) + "\n", standardTextProperties)

        for (medicine in medicines) {
            val activeReminders = getActiveReminders(medicine)
            simplyPdfDocument.text.write(medicine.medicine.name, biggestBoldProperties)
            if (activeReminders.isNotEmpty()) {
                exportMedicine(simplyPdfDocument, activeReminders)
            } else {
                simplyPdfDocument.text.write(context.getString(R.string.no_reminders), standardTextProperties)
            }
        }

        withContext(Dispatchers.IO) {
            simplyPdfDocument.finish()
        }
    }

    private suspend fun exportMedicine(simplyPdfDocument: SimplyPdfDocument, activeReminders: List<Reminder>) {
        val reminders = linkedReminderAlgorithms.sortRemindersList(activeReminders)
        for (reminder in reminders) {
            if (reminder.isOutOfStockOrExpirationReminder) {
                continue
            }
            val firstLine =
                context.getString(R.string.dosage) + ": " + if (reminder.variableAmount) context.getString(R.string.variable_amount) else reminder.amount
            val secondLine = reminderSummaryFormatter.formatExportReminderSummary(reminder)

            simplyPdfDocument.text.write(firstLine + "\n" + secondLine, bulletTextProperties)
        }
    }

    override val extension = "pdf"
    override val type = "Medicine"
}