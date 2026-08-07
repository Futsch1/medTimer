package com.futsch1.medtimer.feature.ui

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.core.common.di.ApplicationScope
import com.futsch1.medtimer.core.common.helpers.FileHelper
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.feature.reminders.api.command.ReminderCommandBus
import com.futsch1.medtimer.feature.ui.exporters.CSVEventExport
import com.futsch1.medtimer.feature.ui.exporters.CSVMedicineExport
import com.futsch1.medtimer.feature.ui.exporters.Export
import com.futsch1.medtimer.feature.ui.exporters.Export.ExporterException
import com.futsch1.medtimer.feature.ui.exporters.ExportBackupPath.getExportFilename
import com.futsch1.medtimer.feature.ui.exporters.PDFEventExport
import com.futsch1.medtimer.feature.ui.exporters.PDFMedicineExport
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/** Backs the shared options menu: event/medicine export and clearing the event log. */
@HiltViewModel
class AppOptionsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val medicineRepository: MedicineRepository,
    private val reminderEventRepository: ReminderEventRepository,
    private val pdfMedicineExportFactory: PDFMedicineExport.Factory,
    private val csvMedicineExportFactory: CSVMedicineExport.Factory,
    private val pdfEventExportFactory: PDFEventExport.Factory,
    private val csvEventExportFactory: CSVEventExport.Factory,
    private val commandBus: ReminderCommandBus,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    fun clearEvents() {
        viewModelScope.launch {
            reminderEventRepository.deleteAll()
            applicationScope.launch { commandBus.scheduleNextNotification() }
        }
    }

    fun exportEvents(isCSV: Boolean, tagFilter: TagFilterViewModel, fragmentManager: FragmentManager) {
        viewModelScope.launch {
            warnIfFiltered(tagFilter)
            val events = tagFilter.filterEvents(reminderEventRepository.getAllWithoutDeletedAndAcknowledged())
            export(
                if (isCSV) csvEventExportFactory.create(events, fragmentManager)
                else pdfEventExportFactory.create(events, fragmentManager)
            )
        }
    }

    fun exportMedicines(isCSV: Boolean, tagFilter: TagFilterViewModel, fragmentManager: FragmentManager) {
        viewModelScope.launch {
            warnIfFiltered(tagFilter)
            val medicines = tagFilter.filterMedicines(medicineRepository.getAll())
            export(
                if (isCSV) csvMedicineExportFactory.create(medicines, fragmentManager)
                else pdfMedicineExportFactory.create(medicines, fragmentManager)
            )
        }
    }

    // An active tag filter silently narrows the export, so say so rather than exporting a subset.
    private fun warnIfFiltered(tagFilter: TagFilterViewModel) {
        if (tagFilter.tagFilterActive()) {
            Toast.makeText(context, R.string.tag_filter_active, Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun export(export: Export) {
        val file = File(context.cacheDir, getExportFilename(export))
        try {
            export.export(file)
            FileHelper.shareFile(context, file)
        } catch (_: ExporterException) {
            Toast.makeText(context, R.string.export_failed, Toast.LENGTH_LONG).show()
        }
    }
}
