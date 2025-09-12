package com.futsch1.medtimer.alarm

import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.futsch1.medtimer.AlarmNavigationDirections
import com.futsch1.medtimer.R

class ReminderAlarmActivity() : AppCompatActivity() {
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_alarm)

        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        keyguardManager.requestDismissKeyguard(this, null)

        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        val navHost = NavHostFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.alarmNavHost, navHost)
            .commit()
    }

    override fun onStart() {
        super.onStart()
        val navHost: NavHostFragment = supportFragmentManager.findFragmentById(R.id.alarmNavHost) as NavHostFragment
        val graph = navHost.navController.navInflater.inflate(R.navigation.navigation)
        graph.setStartDestination(R.id.action_global_alarmFragment)
        navHost.navController.graph = graph
        navController = navHost.navController
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        addAlarmFragment(intent)
    }

    private fun addAlarmFragment(intent: Intent?) {
        if (intent != null) {
            Log.d("ReminderAlarmActivity", "Adding alarm fragment")
            navController.navigate(
                getRoute(intent)
            )
        }
    }

    private fun getRoute(intent: Intent): AlarmNavigationDirections.ActionGlobalAlarmFragment = AlarmNavigationDirections.actionGlobalAlarmFragment(
        intent.getIntExtra("reminderEventId", 0),
        intent.getStringExtra("remindTime") ?: "?",
        intent.getIntExtra("notificationId", 0)
    )

}