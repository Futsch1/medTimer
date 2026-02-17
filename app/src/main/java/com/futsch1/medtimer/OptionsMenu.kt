package com.futsch1.medtimer

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.net.toUri
import androidx.core.view.MenuCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.exporters.CSVEventExport
import com.futsch1.medtimer.exporters.CSVMedicineExport
import com.futsch1.medtimer.exporters.Export
import com.futsch1.medtimer.exporters.Export.ExporterException
import com.futsch1.medtimer.exporters.PDFEventExport
import com.futsch1.medtimer.exporters.PDFMedicineExport
import com.futsch1.medtimer.helpers.EntityEditOptionsMenu
import com.futsch1.medtimer.helpers.FileHelper
import com.futsch1.medtimer.helpers.PathHelper.getExportFilename
import com.futsch1.medtimer.helpers.SimpleIdlingResource
import com.futsch1.medtimer.helpers.safeStartActivity
import com.futsch1.medtimer.medicine.tags.TagDataFromPreferences
import com.futsch1.medtimer.medicine.tags.TagsFragment
import com.futsch1.medtimer.reminders.ReminderProcessorBroadcastReceiver.Companion.requestScheduleNextNotification
import java.io.File
import java.lang.reflect.InvocationTargetException

class OptionsMenu(
    private val fragment: Fragment,
    private val medicineViewModel: MedicineViewModel,
    private val navController: NavController,
    private val hideFilter: Boolean
) : EntityEditOptionsMenu {
    private val context: Context = fragment.requireContext()
    private val backgroundThread: HandlerThread
    private val openFileLauncher: ActivityResultLauncher<Intent?>
    private val idlingResource: SimpleIdlingResource = SimpleIdlingResource("OptionsMenu_" + fragment.javaClass.getName())
    private var menu: Menu? = null
    private var backupManager: BackupManager? = null

    init {
        this.openFileLauncher =
            fragment.registerForActivityResult<Intent?, ActivityResult?>(StartActivityForResult()) { result: ActivityResult? ->
                if (result!!.resultCode == Activity.RESULT_OK && result.data != null) {
                    this.fileSelected(result.data!!.data)
                }
            }
        backgroundThread = HandlerThread("Export")
        backgroundThread.start()
        idlingResource.setIdle()
    }

    fun fileSelected(data: Uri?) {
        Handler(backgroundThread.getLooper()).post { backupManager!!.fileSelected(data) }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.main, menu)
        MenuCompat.setGroupDividerEnabled(menu, true)
        enableOptionalIcons(menu)

        this.backupManager = BackupManager(context, fragment.getParentFragmentManager(), menu, medicineViewModel, openFileLauncher)
        this.menu = menu
        setupSettings()
        setupVersion()
        setupAppURL()
        setupClearEvents()
        setupExport()
        setupGenerateTestData()
        setupShowAppIntro()

        handleTagFilter()
    }

    private fun setupSettings() {
        val item = menu!!.findItem(R.id.settings)
        item.setOnMenuItemClickListener { _: MenuItem? ->
            try {
                navController.navigate(R.id.action_global_preferencesFragment)
                return@setOnMenuItemClickListener true
            } catch (_: IllegalStateException) {
                return@setOnMenuItemClickListener false
            }
        }
    }

    private fun setupVersion() {
        val item = menu!!.findItem(R.id.version)
        item.title = context.getString(R.string.version, BuildConfig.VERSION_NAME)
    }

    private fun setupAppURL() {
        val item = menu!!.findItem(R.id.app_url)
        item.setOnMenuItemClickListener { _: MenuItem? ->
            val myIntent = Intent(Intent.ACTION_VIEW, "https://github.com/Futsch1/medTimer".toUri())
            safeStartActivity(context, myIntent)
            true
        }
    }

    private fun setupClearEvents() {
        val item = menu!!.findItem(R.id.clear_events)
        item.setOnMenuItemClickListener { _ ->
            val builder = AlertDialog.Builder(context)
            builder.setTitle(R.string.confirm)
            builder.setMessage(R.string.are_you_sure_delete_events)
            builder.setCancelable(false)
            builder.setPositiveButton(R.string.yes) { _, _ ->
                medicineViewModel.medicineRepository.deleteReminderEvents()
                requestScheduleNextNotification(context)
            }
            builder.setNegativeButton(R.string.cancel) { _, _ -> }
            builder.show()
            true
        }
    }

    private fun setupExport() {
        val handler = Handler(backgroundThread.getLooper())

        var item = menu!!.findItem(R.id.export_csv)
        item.setOnMenuItemClickListener { _ ->
            handler.post { eventExport(true) }
            true
        }
        item = menu!!.findItem(R.id.export_pdf)
        item.setOnMenuItemClickListener { _ ->
            handler.post { eventExport(false) }
            true
        }
        item = menu!!.findItem(R.id.export_medicine_csv)
        item.setOnMenuItemClickListener { _ ->
            handler.post { medicineExport(true) }
            true
        }
        item = menu!!.findItem(R.id.export_medicine_pdf)
        item.setOnMenuItemClickListener { _ ->
            handler.post { medicineExport(false) }
            true
        }
    }

    fun setupGenerateTestData() {
        val item = menu!!.findItem(R.id.generate_test_data)
        val itemWithEvents = menu!!.findItem(R.id.generate_test_data_and_events)
        if (BuildConfig.DEBUG) {
            itemWithEvents.isVisible = true
            item.isVisible = true
            val menuItemClickListener = MenuItem.OnMenuItemClickListener { menuItem: MenuItem? ->
                idlingResource.setBusy()
                val handler = Handler(backgroundThread.getLooper())
                handler.post {
                    medicineViewModel.medicineRepository.deleteAll()
                    val generateTestData = GenerateTestData(medicineViewModel, menuItem === itemWithEvents)
                    generateTestData.generateTestMedicine()
                    requestScheduleNextNotification(context)
                    idlingResource.setIdle()
                }
                true
            }
            item.setOnMenuItemClickListener(menuItemClickListener)
            itemWithEvents.setOnMenuItemClickListener(menuItemClickListener)
        } else {
            item.isVisible = false
            itemWithEvents.isVisible = false
        }
    }

    private fun setupShowAppIntro() {
        val item = menu!!.findItem(R.id.show_app_intro)
        if (BuildConfig.DEBUG) {
            item.isVisible = true
            item.setOnMenuItemClickListener { _ ->
                context.startActivity(Intent(context, MedTimerAppIntro::class.java))
                true
            }
        } else {
            item.isVisible = false
        }
    }

    private fun handleTagFilter() {
        if (!hideFilter) {
            Handler(backgroundThread.getLooper()).post {
                if (medicineViewModel.medicineRepository.hasTags()) {
                    try {
                        fragment.requireActivity().runOnUiThread { this.setupTagFilter() }
                    } catch (_: IllegalStateException) {
                        // Intentionally empty, do nothing
                    }
                } else {
                    medicineViewModel.validTagIds.postValue(setOf())
                }
            }
        }
    }

    private fun eventExport(isCSV: Boolean) {
        if (medicineViewModel.tagFilterActive()) {
            Toast.makeText(context, R.string.tag_filter_active, Toast.LENGTH_LONG).show()
        }
        val reminderEvents: List<ReminderEvent> =
            medicineViewModel.filterEvents(medicineViewModel.medicineRepository.allReminderEventsWithoutDeletedAndAcknowledged)
        val exporter = if (isCSV) CSVEventExport(reminderEvents, fragment.getParentFragmentManager(), context) else PDFEventExport(
            reminderEvents,
            fragment.getParentFragmentManager(),
            context
        )
        export(exporter)
    }

    private fun medicineExport(isCSV: Boolean) {
        if (medicineViewModel.tagFilterActive()) {
            Toast.makeText(context, R.string.tag_filter_active, Toast.LENGTH_LONG).show()
        }
        val medicines: List<FullMedicine> = medicineViewModel.filterMedicines(medicineViewModel.medicineRepository.medicines)
        val exporter = if (isCSV) CSVMedicineExport(medicines, fragment.getParentFragmentManager(), context) else PDFMedicineExport(
            medicines,
            fragment.getParentFragmentManager(),
            context
        )
        export(exporter)
    }

    private fun setupTagFilter() {
        if (fragment.view == null) {
            return
        }

        val item = menu!!.findItem(R.id.tag_filter)
        item.isVisible = true
        item.setOnMenuItemClickListener { _ ->
            val tagDataFromPreferences = TagDataFromPreferences(fragment)
            val dialog: DialogFragment = TagsFragment(tagDataFromPreferences)
            dialog.show(fragment.getParentFragmentManager(), "tags")
            true
        }
        medicineViewModel.validTagIds.observe(fragment.getViewLifecycleOwner(), Observer { validTagIds: Set<Int> ->
            if (validTagIds.isEmpty()) {
                item.setIcon(R.drawable.tag)
            } else {
                item.setIcon(R.drawable.tag_fill)
            }
        })
    }

    private fun export(export: Export) {
        val csvFile = File(context.cacheDir, getExportFilename(export))
        try {
            export.export(csvFile)
            FileHelper.shareFile(context, csvFile)
        } catch (_: ExporterException) {
            Toast.makeText(context, R.string.export_failed, Toast.LENGTH_LONG).show()
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
    }

    override fun onDestroy() {
        backgroundThread.quit()
        idlingResource.destroy()
    }

    companion object {
        @SuppressLint("RestrictedApi")
        fun enableOptionalIcons(menu: Menu) {
            if (menu is MenuBuilder) {
                try {
                    val m = menu.javaClass.getDeclaredMethod(
                        "setOptionalIconsVisible", Boolean::class.java
                    )
                    m.invoke(menu, true)
                } catch (_: NoSuchMethodException) {
                    // Intentionally empty
                } catch (_: IllegalAccessException) {
                } catch (_: InvocationTargetException) {
                }
            }
        }
    }
}
