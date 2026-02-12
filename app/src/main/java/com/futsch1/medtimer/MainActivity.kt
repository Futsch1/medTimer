package com.futsch1.medtimer

import android.Manifest.permission
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.ApplicationExitInfo
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.navigateUp
import androidx.navigation.ui.NavigationUI.onNavDestinationSelected
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.Autostart.Companion.restoreNotifications
import com.futsch1.medtimer.ReminderNotificationChannelManager.Companion.initialize
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.preferences.PreferencesNames
import com.futsch1.medtimer.reminders.ReminderContext
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private var appBarConfiguration: AppBarConfiguration? = null
    private var batteryOptimizationWarning: CardView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Select theme
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val theme: String = sharedPref.getString("theme", "0")!!
        if (theme == "1") {
            setTheme(R.style.Theme_MedTimer2)
        }

        // Screen capture
        if (sharedPref.getBoolean(PreferencesNames.SECURE_WINDOW, false)) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }

        showIntro(sharedPref)

        this.enableEdgeToEdge()

        initialize(ReminderContext(this))

        TimeHelper.onChangedUseSystemLocale()

        authenticate(sharedPref)
    }

    private fun showIntro(sharedPref: SharedPreferences) {
        val introShown = sharedPref.getBoolean("intro_shown", false)
        if (!introShown && !BuildConfig.DEBUG) {
            Log.d(LogTags.MAIN, "Show MedTimer intro")
            startActivity(Intent(applicationContext, MedTimerAppIntro::class.java))
            sharedPref.edit { putBoolean("intro_shown", true) }
        } else {
            checkPermissions()
        }
    }

    private fun authenticate(sharedPref: SharedPreferences) {
        val biometrics = Biometrics(
            this,
            {
                start()
            }, {
                this.finish()
            })
        if (sharedPref.getBoolean("app_authentication", false) && biometrics.hasBiometrics()) {
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
            RequestPostNotificationPermission(this).requestPermission()
        }
    }

    private fun start() {
        setContentView(R.layout.activity_main)
        setupNavigation()
        batteryOptimizationWarning = findViewById(R.id.batteryOptimizationWarning)
        findViewById<View>(R.id.dismissBatteryWarning)?.setOnClickListener { _: View ->
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            sharedPref.edit { putBoolean(BATTERY_WARNING_DISMISSED, true) }
            checkBatteryOptimization()
        }

        dispatch(this, this.intent)
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
            onNavDestinationSelected(item!!, navController)
            true
        }
    }

    private fun checkBatteryOptimization() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val warningDismissed = sharedPref.getBoolean(BATTERY_WARNING_DISMISSED, false)

        if (!powerManager.isIgnoringBatteryOptimizations(packageName) && !warningDismissed && !BuildConfig.DEBUG) {
            Log.d(LogTags.MAIN, "Show battery optimization")
            batteryOptimizationWarning?.visibility = View.VISIBLE
        } else {
            batteryOptimizationWarning?.visibility = View.GONE
        }
    }

    private fun checkForceStopped() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            val exitInfos: List<ApplicationExitInfo> = activityManager.getHistoricalProcessExitReasons(null, 0, 1)

            if (exitInfos.isNotEmpty() && exitInfos[0].reason == ApplicationExitInfo.REASON_USER_REQUESTED) {
                Log.w(LogTags.MAIN, "MedTimer was force stopped")

                restoreNotifications(applicationContext)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        // hack for https://issuetracker.google.com/issues/113122354
        // taken from https://stackoverflow.com/questions/52013545/android-9-0-not-allowed-to-start-service-app-is-in-background-after-onresume
        val runningAppProcesses = activityManager.runningAppProcesses
        if (runningAppProcesses != null) {
            val importance = runningAppProcesses[0].importance
            if (importance <= RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                startService(Intent(applicationContext, ReminderSchedulerService::class.java))
            }
        }

        checkBatteryOptimization()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        dispatch(this, intent)
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

    companion object {
        private const val BATTERY_WARNING_DISMISSED = "battery_warning_dismissed"
    }
}
