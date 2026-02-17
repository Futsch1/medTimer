package com.futsch1.medtimer

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.database.JSONBackup
import com.futsch1.medtimer.database.JSONMedicineBackup
import com.futsch1.medtimer.database.JSONReminderEventBackup
import com.futsch1.medtimer.helpers.FileHelper
import com.futsch1.medtimer.helpers.PathHelper.backupFilename
import com.futsch1.medtimer.helpers.ProgressDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class BackupManager(
    private val context: Context,
    private val fragment: Fragment,
    private val menu: Menu,
    private val medicineViewModel: MedicineViewModel,
    private val openFileLauncher: ActivityResultLauncher<Intent?>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) {

    init {
        setupBackup()
    }

    private fun setupBackup() {
        var item = menu.findItem(R.id.create_backup)
        item.setOnMenuItemClickListener { _ ->
            selectBackupType()
            true
        }

        item = menu.findItem(R.id.restore_backup)
        item.setOnMenuItemClickListener { _ ->
            AlertDialog.Builder(context)
                .setTitle(R.string.restore)
                .setMessage(R.string.restore_start)
                .setCancelable(true)
                .setPositiveButton(R.string.ok) { _, _ -> openBackup() }
                .setNegativeButton(R.string.cancel) { _, _ -> }.show()
            true
        }
    }

    private fun selectBackupType() {
        val alertDialogBuilder = MaterialAlertDialogBuilder(context)
        alertDialogBuilder.setTitle(R.string.backup)
        val checkedItems = booleanArrayOf(true, true)
        alertDialogBuilder.setMultiChoiceItems(
            R.array.backup_types,
            checkedItems
        ) { _: DialogInterface?, which: Int, isChecked: Boolean -> checkedItems[which] = isChecked }
        alertDialogBuilder.setPositiveButton(R.string.ok) { _, _ ->
            fragment.lifecycleScope.launch(ioDispatcher) {
                performBackup(checkedItems)
            }
        }
        alertDialogBuilder.show()
    }

    private fun openBackup() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType("application/json")
        openFileLauncher.launch(intent)
    }

    private fun performBackup(checkedItems: BooleanArray) {
        val progressDialogFragment = ProgressDialogFragment()
        fragment.lifecycleScope.launch(mainDispatcher) {
            progressDialogFragment.show(fragment.getParentFragmentManager(), "backup")
        }

        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonObject = JsonObject()
        if (checkedItems[0]) {
            jsonObject.add(
                MEDICINE_KEY, createBackup(
                    JSONMedicineBackup(),
                    medicineViewModel.medicineRepository.medicines
                )
            )
        }
        if (checkedItems[1]) {
            jsonObject.add(
                EVENT_KEY, createBackup(
                    JSONReminderEventBackup(),
                    medicineViewModel.medicineRepository.allReminderEventsWithoutDeleted
                )
            )
        }
        if (checkedItems[0] || checkedItems[1]) {
            createAndSave(gson.toJson(jsonObject))
        }
        fragment.lifecycleScope.launch(mainDispatcher) {
            progressDialogFragment.dismiss()
        }
    }

    private fun <T> createBackup(jsonBackup: JSONBackup<T>, backupData: List<T>): JsonElement {
        return jsonBackup.createBackup(
            medicineViewModel.medicineRepository.version,
            backupData
        )
    }

    private fun createAndSave(fileContent: String?) {
        val file = File(context.cacheDir, backupFilename)
        if (FileHelper.saveToFile(file, fileContent)) {
            FileHelper.shareFile(context, file)
        } else {
            fragment.lifecycleScope.launch(mainDispatcher) {
                Toast.makeText(context, R.string.backup_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun fileSelected(data: Uri?) {
        fragment.lifecycleScope.launch(ioDispatcher) {
            val json = FileHelper.readFromUri(data, context.contentResolver)

            val progressDialogFragment = ProgressDialogFragment()
            withContext(mainDispatcher) {
                progressDialogFragment.show(fragment.getParentFragmentManager(), "restore")
            }
            var restoreSuccessful = false
            if (json != null) {
                Log.d(LogTags.BACKUP, "Starting backup restore: $data")
                restoreSuccessful = restoreCombinedBackup(json)
            }

            if (!restoreSuccessful && json != null) {
                // Try legacy backup formats
                restoreSuccessful = restoreBackup(json, JSONMedicineBackup()) || restoreBackup(json, JSONReminderEventBackup())
            }
            Log.d(LogTags.BACKUP, "Backup restore finished")
            withContext(mainDispatcher) {
                progressDialogFragment.dismiss()

                AlertDialog.Builder(context)
                    .setMessage(if (restoreSuccessful) R.string.restore_successful else R.string.restore_failed)
                    .setPositiveButton(R.string.ok) { _, _ -> }
                    .show()
            }
        }
    }

    private fun restoreCombinedBackup(json: String): Boolean {
        var restoreSuccessful = false
        try {
            val rootElement = JsonParser.parseString(json).getAsJsonObject()
            if (rootElement.has(MEDICINE_KEY)) {
                restoreSuccessful = restoreBackup(
                    rootElement[MEDICINE_KEY].toString(),
                    JSONMedicineBackup()
                )
            }
            if (rootElement.has(EVENT_KEY)) {
                restoreSuccessful = restoreSuccessful && restoreBackup(
                    rootElement[EVENT_KEY].toString(),
                    JSONReminderEventBackup()
                )
            }
        } catch (_: JsonSyntaxException) {
            restoreSuccessful = false
        }
        return restoreSuccessful
    }

    private fun <T> restoreBackup(json: String, backup: JSONBackup<T>): Boolean {
        val backupData: List<T>? = backup.parseBackup(json)
        if (backupData != null) {
            backup.applyBackup(backupData, medicineViewModel.medicineRepository)
            return true
        }
        return false
    }

    companion object {
        private const val MEDICINE_KEY = "medicines"
        private const val EVENT_KEY = "events"
    }
}
