package com.futsch1.medtimer

import android.Manifest.permission
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.ApplicationExitInfo
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.text.InputType
import android.util.Log
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.futsch1.medtimer.core.common.ActivityCodes
import com.futsch1.medtimer.core.common.LogTags
import com.futsch1.medtimer.core.common.di.ApplicationScope
import com.futsch1.medtimer.core.common.helpers.hasBiometrics
import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.ThemeSetting
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.database.backup.BackupManager
import com.futsch1.medtimer.feature.reminders.ReminderNotificationChannelManager.Companion.initialize
import com.futsch1.medtimer.feature.reminders.ReminderSchedulerService
import com.futsch1.medtimer.feature.reminders.api.command.ReminderCommandBus
import com.futsch1.medtimer.feature.reminders.api.notificationData.toReminderNotificationData
import com.futsch1.medtimer.feature.ui.RequestPostNotificationPermission
import com.futsch1.medtimer.feature.ui.helpers.TextInputDialogBuilder
import com.futsch1.medtimer.feature.ui.overview.OverviewFragment
import com.futsch1.medtimer.feature.ui.overview.VariableAmountHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private var navHostFragment: NavHostFragment? = null

    @Inject
    lateinit var autostartService: AutostartService
    private val requestNotificationPermission =
        RequestPostNotificationPermission(this) { persistentDataDataSource.setShowNotifications(false) }

    @Inject
    lateinit var preferencesDataSource: PreferencesDataSource

    @Inject
    lateinit var persistentDataDataSource: PersistentDataDataSource

    @Inject
    lateinit var backupManagerFactory: BackupManager.Factory

    @Inject
    lateinit var variableAmountHandler: VariableAmountHandler

    @Inject
    lateinit var biometricsFactory: Biometrics.Factory

    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var powerManager: PowerManager

    @Inject
    lateinit var activityManager: ActivityManager

    @Inject
    lateinit var commandBus: ReminderCommandBus

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Select theme
        if (preferencesDataSource.preferences.value.theme == ThemeSetting.ALTERNATIVE) {
            setTheme(com.futsch1.medtimer.core.ui.R.style.Theme_MedTimer2)
        }

        // Screen capture
        if (preferencesDataSource.preferences.value.useSecureWindow) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }

        showIntro()

        this.enableEdgeToEdge()

        initialize(this, notificationManager)

        lifecycleScope.launch {
            authenticate(preferencesDataSource)
        }
    }

    private fun showIntro() {
        if (!persistentDataDataSource.data.value.introShown && !BuildConfig.DEBUG) {
            Log.d(LogTags.MAIN, "Show MedTimer intro")
            startActivity(Intent(applicationContext, MedTimerAppIntro::class.java))
            persistentDataDataSource.setIntroShown(true)
        } else {
            checkPermissions()
        }
    }

    private suspend fun authenticate(preferencesDataSource: PreferencesDataSource) {
        val biometrics = biometricsFactory.create(
            this,
            {
                lifecycleScope.launch {
                    start()
                }
            }, {
                this.finish()
            })
        if (preferencesDataSource.preferences.value.appAuthentication && this.hasBiometrics()) {
            Log.d(LogTags.MAIN, "Start biometric authentication")
            biometrics.authenticate()
        } else {
            start()
        }

        handleBackPressed()
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(
                this,
                permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.requestPermission()
        }
    }

    private suspend fun start() {
        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    MedTimerTheme {
                        AppNavigationScaffold(
                            onContentBound = ::onContentBound,
                            onNavItemClick = ::onNavItemClick,
                        )
                    }
                }
            }
        )

        dispatchIntent(this.intent)
        this.intent = Intent()

        checkForceStopped()
    }

    private fun handleBackPressed() {
        val backPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Handler(mainLooper).postDelayed({
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }, 20)
            }
        }
        this.onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

    private fun onContentBound(navHostFragment: NavHostFragment) {
        this.navHostFragment = navHostFragment
        updateCurrentTab(navHostFragment)
    }

    private var currentTabId = com.futsch1.medtimer.feature.ui.R.id.overviewFragment

    private fun updateCurrentTab(navHostFragment: NavHostFragment) {
        navHostFragment.navController.addOnDestinationChangedListener { _, destination, _ ->
            topLevelDestinationId(destination).takeIf { it != 0 }?.let { currentTabId = it }
        }
    }

    private fun onNavItemClick(navController: NavController, destinationId: Int) {
        val currentDestination = navController.currentDestination
        if (currentTabId == destinationId) {
            if (currentDestination?.id != destinationId) {
                navController.popBackStack(destinationId, false)
            }
            if (destinationId == com.futsch1.medtimer.feature.ui.R.id.overviewFragment) {
                navHostFragment?.childFragmentManager?.fragments
                    ?.filterIsInstance<OverviewFragment>()
                    ?.lastOrNull()
                    ?.jumpToToday()
            }
            return
        }

        navController.popBackStack(com.futsch1.medtimer.feature.ui.R.id.preferencesFragment, true)
        val options = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .setPopUpTo(navController.graph.startDestinationId, inclusive = false, saveState = true)
            .build()
        navController.navigate(destinationId, null, options)
    }

    private suspend fun checkForceStopped() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val exitInfos: List<ApplicationExitInfo> = activityManager.getHistoricalProcessExitReasons(null, 0, 1)

            if (exitInfos.isNotEmpty() && exitInfos[0].reason == ApplicationExitInfo.REASON_USER_REQUESTED) {
                Log.w(LogTags.MAIN, "MedTimer was force stopped")

                autostartService.restoreNotifications()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // hack for https://issuetracker.google.com/issues/113122354
        // taken from https://stackoverflow.com/questions/52013545/android-9-0-not-allowed-to-start-service-app-is-in-background-after-onresume
        val runningAppProcesses = activityManager.runningAppProcesses
        if (runningAppProcesses != null) {
            val importance = runningAppProcesses[0].importance
            if (importance <= RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                startService(Intent(applicationContext, ReminderSchedulerService::class.java))
            }
        }

        backupManagerFactory.create(this, this, null, null, supportFragmentManager).autoBackup()
    }

    private suspend fun dispatchIntent(intent: Intent) {
        Log.d(LogTags.MAIN, "Dispatch intent: ${intent.action}")
        when (intent.action) {
            ActivityCodes.VARIABLE_AMOUNT_ACTIVITY -> {
                variableAmountHandler.show(this, intent)
            }

            ActivityCodes.CUSTOM_SNOOZE_ACTIVITY -> {
                val reminderNotificationData = intent.extras!!.toReminderNotificationData()
                if (reminderNotificationData.valid) {
                    TextInputDialogBuilder(this)
                        .title(com.futsch1.medtimer.core.ui.R.string.snooze_duration)
                        .hint(com.futsch1.medtimer.core.ui.R.string.minutes_string)
                        .initialText("")
                        .inputType(InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_CLASS_NUMBER)
                        .textSink { snoozeTime: String? ->
                            snoozeTime?.toIntOrNull()?.toDuration(DurationUnit.MINUTES)
                                ?.let { duration ->
                                    applicationScope.launch {
                                        commandBus.snooze(reminderNotificationData, duration)
                                    }
                                }
                        }
                        .cancelCallback {
                            Log.d(LogTags.REMINDER, "Snooze dialog cancelled")
                        }
                        .show()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        lifecycleScope.launch {
            dispatchIntent(intent)
        }
    }
}
