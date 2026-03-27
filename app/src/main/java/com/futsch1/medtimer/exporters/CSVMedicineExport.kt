package com.futsch1.medtimer.exporters

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.helpers.ReminderSummaryFormatter
import com.futsch1.medtimer.helpers.TableHelper
import com.futsch1.medtimer.medicine.LinkedReminderAlgorithms
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.IOException

class CSVMedicineExport @AssistedInject constructor(
    @Assisted val medicines: List<FullMedicine>,
    @Assisted fragmentManager: FragmentManager,
    @param:ApplicationContext val context: Context,
    private val reminderSummaryFormatter: ReminderSummaryFormatter,
    @param:Dispatcher(MedTimerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : Export(fragmentManager) {

    @AssistedFactory
    fun interface Factory {
        fun create(medicines: List<FullMedicine>, fragmentManager: FragmentManager): CSVMedicineExport
    }
    @Throws(ExporterException::class)
    public override suspend fun exportInternal(file: File) {
        try {
            withContext(ioDispatcher) {
                FileWriter(file).use { csvFile ->
                    val headerTexts = TableHelper.getTableHeadersForMedicationExport(context)
                    csvFile.write(java.lang.String.join(";", headerTexts) + "\n")
                    for (medicine in medicines) {
                        exportMedicine(csvFile, medicine)
                    }
                }
            }
        } catch (_: IOException) {
            throw ExporterException()
        }
    }

    private suspend fun exportMedicine(csvFile: FileWriter, medicine: FullMedicine) {
        val reminders = LinkedReminderAlgorithms().sortRemindersList(medicine.reminders)
        for (reminder in reminders) {
            if (reminder.isOutOfStockOrExpirationReminder) {
                continue
            }
            val line = String.format(
                "%s;%s;%s\n",
                medicine.medicine.name,
                if (reminder.variableAmount) context.getString(R.string.variable_amount) else reminder.amount,
                reminderSummaryFormatter.formatExportReminderSummary(reminder)
            )
            withContext(ioDispatcher) {
                csvFile.write(line)
            }
        }
        if (reminders.isEmpty()) {
            withContext(ioDispatcher) {
                csvFile.write(
                    String.format(
                        "%s;%s;%s\n",
                        medicine.medicine.name,
                        "",
                        context.getString(R.string.no_reminders)
                    )
                )
            }
        }
    }

    override val extension = "csv"
    override val type = "Medicine"
}