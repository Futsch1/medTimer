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
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.navigateUp
import androidx.navigation.ui.NavigationUI.onNavDestinationSelected
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.futsch1.medtimer.ReminderNotificationChannelManager.Companion.initialize
import com.futsch1.medtimer.database.backup.BackupManager
import com.futsch1.medtimer.helpers.TextInputDialogBuilder
import com.futsch1.medtimer.helpers.hasBiometrics
import com.futsch1.medtimer.model.ThemeSetting
import com.futsch1.medtimer.overview.VariableAmountHandler
import com.futsch1.medtimer.preferences.PersistentDataDataSource
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.ReminderProcessorBroadcastReceiver
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Select theme
        if (preferencesDataSource.preferences.value.theme == ThemeSetting.ALTERNATIVE) {
            setTheme(R.style.Theme_MedTimer2)
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
        setContentView(R.layout.activity_main)
        setupNavigation()
        batteryOptimizationWarning = findViewById(R.id.batteryOptimizationWarning)
        findViewById<View>(R.id.dismissBatteryWarning)?.setOnClickListener { _: View ->
            persistentDataDataSource.setBatteryWarningShown(true)
            checkBatteryOptimization()
        }

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

    private fun setupNavigation() {
        val navHostFragment: NavHostFragment = supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment
        val navController = navHostFragment.navController
        setSupportActionBar(findViewById(R.id.toolbar))
        appBarConfiguration = AppBarConfiguration.Builder(
            R.id.overviewFragment, R.id.medicinesFragment, R.id.statisticsFragment
        )
            .build()
        setupActionBarWithNavController(this, navController, appBarConfiguration!!)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        setupWithNavController(bottomNavigationView, navController)
        bottomNavigationView.setOnItemReselectedListener { item: MenuItem? ->
            navController.popBackStack(item!!.itemId, false)
            val topFragment = navHostFragment.getChildFragmentManager().fragments.firstOrNull()
            if (topFragment is OnFragmentReselectedListener) {
                // Forward the reselection event to the current fragment
                topFragment.onFragmentReselected()
            }
        }
        bottomNavigationView.setOnItemSelectedListener { item: MenuItem? ->
            navController.popBackStack(R.id.preferencesFragment, true)
            onNavDestinationSelected(item!!, navController)
            true
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
                        .title(R.string.snooze_duration)
                        .hint(R.string.minutes_string)
                        .initialText("")
                        .inputType(InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_CLASS_NUMBER)
                        .textSink { snoozeTime: String? ->
                            snoozeTime?.toIntOrNull()?.toDuration(DurationUnit.MINUTES)
                                ?.let { ReminderProcessorBroadcastReceiver.requestSnooze(this, reminderNotificationData, it) }
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
            return navigateUp(navController, appBarConfiguration!!)
                    || super.onSupportNavigateUp()
        } catch (_: IllegalStateException) {
            return false
        }
    }
}
