package com.futsch1.medtimer.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.futsch1.medtimer.ActivityCodes.EXTRA_NOTIFICATION_ID
import com.futsch1.medtimer.ActivityCodes.EXTRA_NOTIFICATION_TIME_STRING
import com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_EVENT_ID
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.ReminderEvent

class ReminderAlarmActivity() : AppCompatActivity() {
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

        addAlarmFragment(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        addAlarmFragment(intent)
    }

    private fun addAlarmFragment(intent: Intent?) {
        if (intent != null) {
            Log.d("ReminderAlarmActivity", "Adding alarm fragment")
            supportFragmentManager.beginTransaction().add(R.id.alarmFragmentContainer, AlarmFragment::class.java, intent.extras).commit()
        }
    }

    companion object {
        fun getIntent(context: Context, reminderEvent: ReminderEvent, remindTime: String, notificationId: Int): Intent {
            val intent = Intent(context, ReminderAlarmActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            val bundle = Bundle()
            bundle.putInt(EXTRA_REMINDER_EVENT_ID, reminderEvent.reminderEventId)
            bundle.putInt(EXTRA_NOTIFICATION_ID, notificationId)
            bundle.putString(EXTRA_NOTIFICATION_TIME_STRING, remindTime)
            intent.putExtras(bundle)

            return intent
        }
    }
}