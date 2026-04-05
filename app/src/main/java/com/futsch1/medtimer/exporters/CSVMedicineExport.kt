package com.futsch1.medtimer.exporters

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.toModel
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.helpers.ReminderSummaryFormatter
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
    @Assisted private val medicines: List<FullMedicineEntity>,
    @Assisted fragmentManager: FragmentManager,
    @param:ApplicationContext val context: Context,
    private val reminderSummaryFormatter: ReminderSummaryFormatter,
    @param:Dispatcher(MedTimerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    private val linkedReminderAlgorithms: LinkedReminderAlgorithms
) : Export(fragmentManager) {

    @AssistedFactory
    fun interface Factory {
        fun create(medicines: List<FullMedicineEntity>, fragmentManager: FragmentManager): CSVMedicineExport
    }

    @Throws(ExporterException::class)
    public override suspend fun exportInternal(file: File) {
        try {
            withContext(ioDispatcher) {
                FileWriter(file).use { csvFile ->
                    val headerTexts = listOf(
                        context.getString(R.string.tab_medicine),
                        context.getString(R.string.dosage),
                        context.getString(R.string.time)
                    )
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

    private suspend fun exportMedicine(csvFile: FileWriter, medicine: FullMedicineEntity) {
        val reminders = linkedReminderAlgorithms.sortRemindersList(medicine.reminders.map { it.toModel() })
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