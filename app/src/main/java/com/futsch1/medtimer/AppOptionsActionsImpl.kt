package com.futsch1.medtimer

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.core.common.di.ApplicationScope
import com.futsch1.medtimer.core.common.di.Dispatcher
import com.futsch1.medtimer.core.common.di.MedTimerDispatchers
import com.futsch1.medtimer.core.common.helpers.SimpleIdlingResource
import com.futsch1.medtimer.database.DatabaseManager
import com.futsch1.medtimer.database.backup.BackupManager
import com.futsch1.medtimer.feature.reminders.api.command.ReminderCommandBus
import com.futsch1.medtimer.feature.ui.AppOptionsActions
import com.futsch1.medtimer.feature.ui.AppOptionsActionsFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * `:app` side of [AppOptionsActions]. Created per hosting fragment, in `onCreate`, because backup
 * registers activity-result launchers.
 */
class AppOptionsActionsImpl @AssistedInject constructor(
    @Assisted private val fragment: Fragment,
    private val backupManagerFactory: BackupManager.Factory,
    private val databaseManager: DatabaseManager,
    private val generateTestDataFactory: GenerateTestData.Factory,
    private val commandBus: ReminderCommandBus,
    @param:Dispatcher(MedTimerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) : AppOptionsActions {

    @AssistedFactory
    fun interface Factory : AppOptionsActionsFactory {
        override fun create(fragment: Fragment): AppOptionsActionsImpl
    }

    private val context: Context get() = fragment.requireContext()
    private val idlingResource = SimpleIdlingResource("AppOptionsActions_${fragment.javaClass.name}").apply { setIdle() }

    private val openFileLauncher =
        fragment.registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    fragment.lifecycleScope.launch(ioDispatcher) { backupManager.fileSelected(uri) }
                }
            }
        }

    private val openDirectoryLauncher =
        fragment.registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                backupManager.directorySelected(result.data?.data)
            }
        }

    private val backupManager: BackupManager by lazy {
        backupManagerFactory.create(
            context,
            fragment,
            openFileLauncher,
            openDirectoryLauncher,
            fragment.parentFragmentManager,
        )
    }

    override val versionName: String = BuildConfig.VERSION_NAME
    override val isDebugBuild: Boolean = BuildConfig.DEBUG

    override fun createBackup() = backupManager.createBackup()

    override fun restoreBackup() = backupManager.restoreBackup()

    override fun configureAutomaticBackup() = backupManager.configureAutomaticBackup()

    override fun generateTestData(withEvents: Boolean) {
        idlingResource.setBusy()
        fragment.lifecycleScope.launch(ioDispatcher) {
            databaseManager.deleteAll()
            generateTestDataFactory.create(withEvents).generateTestMedicine()
            applicationScope.launch { commandBus.scheduleNextNotification() }
            idlingResource.setIdle()
        }
    }

    override fun showAppIntro() {
        context.startActivity(Intent(context, MedTimerAppIntro::class.java))
    }

    override fun onDestroy() = idlingResource.destroy()
}
