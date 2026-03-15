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
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.database.JSONBackup
import com.futsch1.medtimer.database.JSONMedicineBackup
import com.futsch1.medtimer.database.JSONReminderEventBackup
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.helpers.FileHelper
import com.futsch1.medtimer.helpers.PathHelper.backupFilename
import com.futsch1.medtimer.helpers.ProgressDialogFragment
import com.futsch1.medtimer.model.BackupInterval
import com.futsch1.medtimer.preferences.PersistentDataDataSource
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

class BackupManager @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val lifecycleOwner: LifecycleOwner,
    @Assisted private val menu: Menu?,
    @Assisted private val medicineViewModel: MedicineViewModel,
    @Assisted("openFile") private val openFileLauncher: ActivityResultLauncher<Intent>?,
    @Assisted("openDirectory") private val openDirectoryLauncher: ActivityResultLauncher<Intent>?,
    @Assisted private val fragmentManager: FragmentManager? = null,
    private val preferencesDataSource: PreferencesDataSource,
    private val persistentDataDataSource: PersistentDataDataSource,
    @param:Dispatcher(MedTimerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    @param:Dispatcher(MedTimerDispatchers.Main) private val mainDispatcher: CoroutineDispatcher
) {

    @AssistedFactory
    interface Factory {
        fun create(
            context: Context,
            lifecycleOwner: LifecycleOwner,
            menu: Menu?,
            medicineViewModel: MedicineViewModel,
            @Assisted("openFile") openFileLauncher: ActivityResultLauncher<Intent>?,
            @Assisted("openDirectory") openDirectoryLauncher: ActivityResultLauncher<Intent>?,
            fragmentManager: FragmentManager? = null,
        ): BackupManager
    }

    init {
        setupBackup()
    }

    private fun setupBackup() {
        if (menu != null) {
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

            item = menu.findItem(R.id.automatic_backup)
            item.setOnMenuItemClickListener { _ ->
                selectAutomaticBackupInterval()
                true
            }
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
            lifecycleOwner.lifecycleScope.launch(ioDispatcher) {
                performBackup(checkedItems)
            }
        }
        alertDialogBuilder.show()
    }

    private fun selectAutomaticBackupInterval() {
        val currentInterval = preferencesDataSource.preferences.value.automaticBackupInterval
        val options = context.resources.getStringArray(R.array.automatic_backup_options)
        val checkedItem = currentInterval.ordinal

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.automatic_backup)
            .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                val selectedValue = BackupInterval.entries[which]
                preferencesDataSource.setAutomaticBackupInterval(selectedValue)
                if (selectedValue != BackupInterval.NEVER) {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    openDirectoryLauncher?.launch(intent)
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    fun directorySelected(uri: Uri?) {
        if (uri != null) {
            preferencesDataSource.setAutomaticBackupDirectory(uri)
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }

    private fun openBackup() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType("application/json")
        openFileLauncher?.launch(intent)
    }

    private fun performBackup(checkedItems: BooleanArray) {
        val progressDialogFragment = ProgressDialogFragment()
        lifecycleOwner.lifecycleScope.launch(mainDispatcher) {
            fragmentManager?.let { progressDialogFragment.show(it, "backup") }
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
        lifecycleOwner.lifecycleScope.launch(mainDispatcher) {
            if (fragmentManager != null) {
                progressDialogFragment.dismiss()
            }
        }
    }

    private fun <T> createBackup(jsonBackup: JSONBackup<T>, backupData: List<T>): JsonElement {
        return jsonBackup.createBackup(
            medicineViewModel.databaseVersion,
            backupData
        )
    }

    private fun createAndSave(fileContent: String?) {
        val file = File(context.cacheDir, backupFilename)
        if (FileHelper.saveToFile(file, fileContent)) {
            FileHelper.shareFile(context, file)
        } else {
            lifecycleOwner.lifecycleScope.launch(mainDispatcher) {
                Toast.makeText(context, R.string.backup_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun fileSelected(data: Uri) {
        lifecycleOwner.lifecycleScope.launch(ioDispatcher) {
            val json = FileHelper.readFromUri(data, context.contentResolver)

            val progressDialogFragment = ProgressDialogFragment()
            withContext(mainDispatcher) {
                fragmentManager?.let { progressDialogFragment.show(it, "restore") }
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
                if (fragmentManager != null) {
                    progressDialogFragment.dismiss()
                }

                AlertDialog.Builder(context)
                    .setMessage(if (restoreSuccessful) R.string.restore_successful else R.string.restore_failed)
                    .setPositiveButton(R.string.ok) { _, _ -> }
                    .show()
            }
        }
    }

    private suspend fun restoreCombinedBackup(json: String): Boolean {
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

    private suspend fun <T> restoreBackup(json: String, backup: JSONBackup<T>): Boolean {
        val backupData: List<T>? = backup.parseBackup(json)
        if (backupData != null) {
            backup.applyBackup(backupData, medicineViewModel.medicineRepository)
            return true
        }
        return false
    }

    fun autoBackup() {
        val interval = preferencesDataSource.preferences.value.automaticBackupInterval
        if (interval == BackupInterval.NEVER) return

        val lastBackup = persistentDataDataSource.data.value.lastAutomaticBackup
        val now = LocalDate.now()

        val shouldBackup = when (interval) {
            BackupInterval.DAILY -> lastBackup.plusDays(1) <= now
            BackupInterval.WEEKLY -> lastBackup.plusDays(7) <= now
            BackupInterval.MONTHLY -> lastBackup.plusDays(30) <= now
            else -> false
        }

        if (shouldBackup) {
            Log.d(LogTags.BACKUP, "Starting auto backup, last was at $lastBackup")
            val directoryUri = preferencesDataSource.preferences.value.automaticBackupDirectory ?: return

            lifecycleOwner.lifecycleScope.launch(ioDispatcher) {
                performAutoBackup(directoryUri)
            }
        }
    }

    private fun performAutoBackup(directoryUri: Uri) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonObject = JsonObject()
        jsonObject.add(
            MEDICINE_KEY, createBackup(
                JSONMedicineBackup(),
                medicineViewModel.medicineRepository.medicines
            )
        )
        jsonObject.add(
            EVENT_KEY, createBackup(
                JSONReminderEventBackup(),
                medicineViewModel.medicineRepository.allReminderEventsWithoutDeleted
            )
        )

        val json = gson.toJson(jsonObject)
        val filename = backupFilename
        val success = saveToDirectory(directoryUri, filename, json)

        lifecycleOwner.lifecycleScope.launch(mainDispatcher) {
            if (success) {
                persistentDataDataSource.setLastAutomaticBackup(LocalDate.now())
                Toast.makeText(context, context.getString(R.string.backup_successful_to, filename), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, R.string.backup_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveToDirectory(directoryUri: Uri, filename: String, content: String): Boolean {
        return try {
            val root = DocumentFile.fromTreeUri(context, directoryUri)
            val file = root?.createFile("application/json", filename)
            if (file != null) {
                context.contentResolver.openOutputStream(file.uri)?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(LogTags.BACKUP, "Auto backup failed", e)
            false
        }
    }

    companion object {
        private const val MEDICINE_KEY = "medicines"
        private const val EVENT_KEY = "events"
    }
}
