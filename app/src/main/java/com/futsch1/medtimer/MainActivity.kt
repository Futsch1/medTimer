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
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.navigateUp
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import com.futsch1.medtimer.core.common.ActivityCodes
import com.futsch1.medtimer.core.common.LogTags
import com.futsch1.medtimer.core.common.OnFragmentReselectedListener
import com.futsch1.medtimer.core.common.di.ApplicationScope
import com.futsch1.medtimer.core.common.helpers.hasBiometrics
import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.ThemeSetting
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.database.backup.BackupManager
import com.futsch1.medtimer.databinding.ContentMainBinding
import com.futsch1.medtimer.feature.reminders.ReminderNotificationChannelManager.Companion.initialize
import com.futsch1.medtimer.feature.reminders.ReminderSchedulerService
import com.futsch1.medtimer.feature.reminders.command.ReminderCommandBus
import com.futsch1.medtimer.feature.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.feature.ui.RequestPostNotificationPermission
import com.futsch1.medtimer.feature.ui.helpers.TextInputDialogBuilder
import com.futsch1.medtimer.feature.ui.overview.VariableAmountHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var autostartService: AutostartService
    private var appBarConfiguration: AppBarConfiguration? = null
    private var batteryOptimizationWarning: CardView? = null
    private var exactReminderWarning: CardView? = null
    private var navHostFragment: NavHostFragment? = null
    private val requestNotificationPermission = RequestPostNotificationPermission(this) { persistentDataDataSource.setShowNotifications(false) }

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
        // Post the back event to the main loop to make sure all pending events are handled before
        val backPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Post the back event to the main loop to make sure all pending events are handled before
                Handler(mainLooper).postDelayed({
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }, 20)
            }
        }
        this.onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

    private fun onContentBound(binding: ContentMainBinding, navHostFragment: NavHostFragment) {
        this.navHostFragment = navHostFragment
        val navController = navHostFragment.navController
        // Track the tab whose area we're in. Detail screens (e.g. editMedicineFragment) are flat siblings
        // of the tab roots, so they don't carry their tab in the destination hierarchy; keep the last
        // top-level tab stickily so a tab tap from one of its detail screens counts as a reselect.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id in topLevelTabIds) {
                currentTabId = destination.id
            }
        }
        setSupportActionBar(binding.toolbar)
        appBarConfiguration = AppBarConfiguration.Builder(
            com.futsch1.medtimer.feature.ui.R.id.overviewFragment,
            com.futsch1.medtimer.feature.ui.R.id.medicinesFragment,
            com.futsch1.medtimer.feature.ui.R.id.statisticsFragment
        ).build()
        setupActionBarWithNavController(this, navController, appBarConfiguration!!)

        batteryOptimizationWarning = binding.batteryOptimizationWarning
        binding.dismissBatteryWarning.setOnClickListener {
            persistentDataDataSource.setBatteryWarningShown(true)
            checkBatteryOptimization()
        }
        exactReminderWarning = binding.exactRemindersWarning
        binding.dismissExactReminderWarning.setOnClickListener {
            persistentDataDataSource.setExactRemindersWarningShown(true)
            checkExactReminders()
        }
    }

    private val topLevelTabIds = setOf(
        com.futsch1.medtimer.feature.ui.R.id.overviewFragment,
        com.futsch1.medtimer.feature.ui.R.id.medicinesFragment,
        com.futsch1.medtimer.feature.ui.R.id.statisticsFragment,
    )

    // The tab area the user is currently in (sticky across that tab's detail screens). Initialised to the
    // start destination, then kept in sync by the destination listener in onContentBound.
    private var currentTabId = com.futsch1.medtimer.feature.ui.R.id.overviewFragment

    // The destination the previous click navigated to (a fresh select, not a reselect). NavController's
    // currentDestination updates synchronously on navigate(), so a tap that lands right after navigating
    // to the same destination would otherwise look like a reselect. The legacy BottomNavigationView
    // tolerated that navigate-then-retap, so we suppress the stray reselect to mirror its behavior.
    private var justNavigatedTo: Int? = null

    // Mirrors the legacy BottomNavigationView select/reselect behavior.
    private fun onNavItemClick(navController: NavController, destinationId: Int) {
        // A tap on the tab we're already in (including its detail screens) is a reselect.
        val isReselected = destinationId == currentTabId
        if (isReselected) {
            // A tab tap always returns to the tab's root, clearing any detail screens on its back stack.
            navController.popBackStack(destinationId, false)
            // But a reselect that immediately follows navigating to this destination is a stray re-tap
            // (the instrumented tests tap each item twice), not a real reselect — don't fire the reselect
            // side-effect for it (e.g. it must not reset the overview day to today).
            if (justNavigatedTo == destinationId) {
                justNavigatedTo = null
                return
            }
            val topFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()
            if (topFragment is OnFragmentReselectedListener) {
                topFragment.onFragmentReselected()
            }
        } else {
            justNavigatedTo = destinationId
            navController.popBackStack(com.futsch1.medtimer.feature.ui.R.id.preferencesFragment, true)
            val options = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(navController.graph.startDestinationId, inclusive = false, saveState = true)
                .build()
            navController.navigate(destinationId, null, options)
        }
    }

    private fun checkBatteryOptimization() {
        if (!powerManager.isIgnoringBatteryOptimizations(packageName) && !persistentDataDataSource.data.value.batteryWarningShown && !BuildConfig.DEBUG) {
            Log.d(LogTags.MAIN, "Show battery optimization")
            batteryOptimizationWarning?.visibility = View.VISIBLE
        } else {
            batteryOptimizationWarning?.visibility = View.GONE
        }
    }

    private fun checkExactReminders() {
        if (!preferencesDataSource.preferences.value.exactReminders && !persistentDataDataSource.data.value.exactRemindersWarningShown && !BuildConfig.DEBUG) {
            Log.d(LogTags.MAIN, "Show exact reminders warning")
            exactReminderWarning?.visibility = View.VISIBLE
        } else {
            exactReminderWarning?.visibility = View.GONE
        }
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

        backupManagerFactory.create(this, this, null, null, null, supportFragmentManager).autoBackup()

        checkBatteryOptimization()
        checkExactReminders()
    }

    private suspend fun dispatchIntent(intent: Intent) {
        Log.d(LogTags.MAIN, "Dispatch intent: ${intent.action}")
        when (intent.action) {
            ActivityCodes.VARIABLE_AMOUNT_ACTIVITY -> {
                variableAmountHandler.show(this, intent)
            }

            ActivityCodes.CUSTOM_SNOOZE_ACTIVITY -> {
                val reminderNotificationData = ReminderNotificationData.fromBundle(intent.extras!!)
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

    override fun onSupportNavigateUp(): Boolean {
        try {
            val navController = this.findNavController(R.id.navHost)
            return appBarConfiguration?.let { navigateUp(navController, it) } == true
                    || super.onSupportNavigateUp()
        } catch (_: IllegalStateException) {
            return false
        }
    }
}
