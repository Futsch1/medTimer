package com.futsch1.medtimer.database.backup

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
import com.futsch1.medtimer.R
import com.futsch1.medtimer.core.common.LogTags
import com.futsch1.medtimer.core.common.di.Dispatcher
import com.futsch1.medtimer.core.common.di.MedTimerDispatchers
import com.futsch1.medtimer.core.common.helpers.FileHelper
import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.BackupInterval
import com.futsch1.medtimer.core.domain.repository.BackupRepository
import com.futsch1.medtimer.core.ui.ProgressDialogFragment
import com.futsch1.medtimer.helpers.ExportBackupPath
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
    @Assisted("openFile") private val openFileLauncher: ActivityResultLauncher<Intent>?,
    @Assisted("openDirectory") private val openDirectoryLauncher: ActivityResultLauncher<Intent>?,
    @Assisted private val fragmentManager: FragmentManager? = null,
    private val preferencesDataSource: PreferencesDataSource,
    private val persistentDataDataSource: PersistentDataDataSource,
    private val backupRepository: BackupRepository,
    @param:Dispatcher(MedTimerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    @param:Dispatcher(MedTimerDispatchers.Main) private val mainDispatcher: CoroutineDispatcher
) {

    @AssistedFactory
    fun interface Factory {
        fun create(
            context: Context,
            lifecycleOwner: LifecycleOwner,
            menu: Menu?,
            @Assisted("openFile") openFileLauncher: ActivityResultLauncher<Intent>?,
            @Assisted("openDirectory") openDirectoryLauncher: ActivityResultLauncher<Intent>?,
            fragmentManager: FragmentManager?
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
                MaterialAlertDialogBuilder(context)
                    .setTitle(com.futsch1.medtimer.core.ui.R.string.restore)
                    .setMessage(com.futsch1.medtimer.core.ui.R.string.restore_start)
                    .setCancelable(true)
                    .setPositiveButton(com.futsch1.medtimer.core.ui.R.string.ok) { _, _ -> openBackup() }
                    .setNegativeButton(com.futsch1.medtimer.core.ui.R.string.cancel) { _, _ -> }.show()
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
        alertDialogBuilder.setTitle(com.futsch1.medtimer.core.ui.R.string.backup)
        val checkedItems = booleanArrayOf(true, true)
        alertDialogBuilder.setMultiChoiceItems(
            com.futsch1.medtimer.core.ui.R.array.backup_types,
            checkedItems
        ) { _: DialogInterface?, which: Int, isChecked: Boolean -> checkedItems[which] = isChecked }
        alertDialogBuilder.setPositiveButton(com.futsch1.medtimer.core.ui.R.string.ok) { _, _ ->
            lifecycleOwner.lifecycleScope.launch {
                performBackup(checkedItems)
            }
        }
        alertDialogBuilder.show()
    }

    private fun selectAutomaticBackupInterval() {
        val currentInterval = preferencesDataSource.preferences.value.automaticBackupInterval
        val options = context.resources.getStringArray(com.futsch1.medtimer.core.ui.R.array.automatic_backup_options)
        val checkedItem = currentInterval.ordinal

        MaterialAlertDialogBuilder(context)
            .setTitle(com.futsch1.medtimer.core.ui.R.string.automatic_backup)
            .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                val selectedValue = BackupInterval.entries[which]
                preferencesDataSource.setAutomaticBackupInterval(selectedValue)
                if (selectedValue != BackupInterval.NEVER) {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    openDirectoryLauncher?.launch(intent)
                }
                dialog.dismiss()
            }
            .setNegativeButton(com.futsch1.medtimer.core.ui.R.string.cancel, null)
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

    private suspend fun performBackup(checkedItems: BooleanArray) {
        val progressDialogFragment = ProgressDialogFragment()
        lifecycleOwner.lifecycleScope.launch(mainDispatcher) {
            fragmentManager?.let { progressDialogFragment.show(it, "backup") }
        }

        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonObject = JsonObject()
        if (checkedItems[0]) {
            jsonObject.add(
                MEDICINE_KEY, createBackup(
                    JSONMedicineBackup(backupRepository),
                    backupRepository.getMedicineBackup()
                )
            )
        }
        if (checkedItems[1]) {
            jsonObject.add(
                EVENT_KEY, createBackup(
                    JSONReminderEventBackup(backupRepository),
                    backupRepository.getReminderEventBackup()
                )
            )
        }
        if (checkedItems[0] || checkedItems[1]) {
            jsonObject.add(SETTINGS_KEY, JSONSettingsBackup(preferencesDataSource).createBackup())
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
            backupRepository.databaseVersion,
            backupData
        )
    }

    private fun createAndSave(fileContent: String?) {
        val file = File(context.cacheDir, ExportBackupPath.backupFilename)
        if (FileHelper.saveToFile(file, fileContent)) {
            FileHelper.shareFile(context, file)
        } else {
            lifecycleOwner.lifecycleScope.launch(mainDispatcher) {
                Toast.makeText(context, com.futsch1.medtimer.core.ui.R.string.backup_failed, Toast.LENGTH_LONG).show()
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
                restoreSuccessful = restoreBackup(json, JSONMedicineBackup(backupRepository)) || restoreBackup(
                    json,
                    JSONReminderEventBackup(backupRepository)
                )
            }
            Log.d(LogTags.BACKUP, "Backup restore finished")
            withContext(mainDispatcher) {
                if (fragmentManager != null) {
                    progressDialogFragment.dismiss()
                }

                MaterialAlertDialogBuilder(context)
                    .setMessage(if (restoreSuccessful) com.futsch1.medtimer.core.ui.R.string.restore_successful else com.futsch1.medtimer.core.ui.R.string.restore_failed)
                    .setPositiveButton(com.futsch1.medtimer.core.ui.R.string.ok) { _, _ -> }
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
                    JSONMedicineBackup(backupRepository)
                )
            }
            if (rootElement.has(EVENT_KEY)) {
                restoreSuccessful = restoreSuccessful && restoreBackup(
                    rootElement[EVENT_KEY].toString(),
                    JSONReminderEventBackup(backupRepository)
                )
            }
            if (rootElement.has(SETTINGS_KEY)) {
                JSONSettingsBackup(preferencesDataSource).applyBackup(rootElement[SETTINGS_KEY].toString())
            }
        } catch (_: JsonSyntaxException) {
            restoreSuccessful = false
        }
        return restoreSuccessful
    }

    private suspend fun <T> restoreBackup(json: String, backup: JSONBackup<T>): Boolean {
        val backupData: List<T>? = backup.parseBackup(json)
        if (backupData != null) {
            backup.applyBackup(backupData)
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

            lifecycleOwner.lifecycleScope.launch {
                performAutoBackup(directoryUri)
            }
        }
    }

    private suspend fun performAutoBackup(directoryUri: Uri) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonObject = JsonObject()
        jsonObject.add(
            MEDICINE_KEY, createBackup(
                JSONMedicineBackup(backupRepository),
                backupRepository.getMedicineBackup()
            )
        )
        jsonObject.add(
            EVENT_KEY, createBackup(
                JSONReminderEventBackup(backupRepository),
                backupRepository.getReminderEventBackup()
            )
        )
        jsonObject.add(SETTINGS_KEY, JSONSettingsBackup(preferencesDataSource).createBackup())

        val json = gson.toJson(jsonObject)
        val filename = ExportBackupPath.backupFilename
        val result = saveToDirectory(directoryUri, filename, json)

        lifecycleOwner.lifecycleScope.launch(mainDispatcher) {
            when (result) {
                SaveResult.SUCCESS -> {
                    persistentDataDataSource.setLastAutomaticBackup(LocalDate.now())
                    Toast.makeText(context, context.getString(com.futsch1.medtimer.core.ui.R.string.backup_successful_to, filename), Toast.LENGTH_LONG).show()
                }
                // The most common cause: the SAF permission for the chosen folder was revoked,
                // which happens whenever the app is reinstalled (a fresh debug build with a
                // different signature counts as a reinstall too) - the fix is re-selecting the
                // folder, not retrying, so this gets its own message rather than the generic one.
                SaveResult.PERMISSION_LOST -> {
                    MaterialAlertDialogBuilder(context)
                        .setMessage(com.futsch1.medtimer.core.ui.R.string.backup_folder_permission_lost)
                        .setPositiveButton(com.futsch1.medtimer.core.ui.R.string.ok) { _, _ -> }
                        .show()
                }
                SaveResult.FAILED -> {
                    MaterialAlertDialogBuilder(context)
                        .setMessage(com.futsch1.medtimer.core.ui.R.string.backup_failed)
                        .setPositiveButton(com.futsch1.medtimer.core.ui.R.string.ok) { _, _ -> }
                        .show()
                }
            }
        }
    }

    private enum class SaveResult { SUCCESS, PERMISSION_LOST, FAILED }

    private fun saveToDirectory(directoryUri: Uri, filename: String, content: String): SaveResult {
        return try {
            val root = DocumentFile.fromTreeUri(context, directoryUri)
            val file = root?.createFile("application/json", filename)
            if (file != null) {
                context.contentResolver.openOutputStream(file.uri)?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                }
                SaveResult.SUCCESS
            } else {
                SaveResult.FAILED
            }
        } catch (e: SecurityException) {
            Log.e(LogTags.BACKUP, "Auto backup failed: permission to the backup folder was lost", e)
            SaveResult.PERMISSION_LOST
        } catch (e: Exception) {
            Log.e(LogTags.BACKUP, "Auto backup failed", e)
            SaveResult.FAILED
        }
    }

    companion object {
        private const val MEDICINE_KEY = "medicines"
        private const val EVENT_KEY = "events"
        private const val SETTINGS_KEY = "settings"
    }
}