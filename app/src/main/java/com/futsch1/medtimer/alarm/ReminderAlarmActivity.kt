package com.futsch1.medtimer.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
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
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var vibrator: Vibrator

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

        val bundle = Bundle()
        bundle.putInt(EXTRA_REMINDER_EVENT_ID, 1)
        bundle.putInt(EXTRA_NOTIFICATION_ID, 2)
        bundle.putString(EXTRA_NOTIFICATION_TIME_STRING, "12")
        intent.putExtras(bundle)

        addAlarmFragment(intent)
    }

    override fun onResume() {
        super.onResume()
        startAlarmTone()
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer.stop()
        mediaPlayer.release()
        vibrator.cancel()
    }

    private fun startAlarmTone() {
        val alarmURI = Settings.System.DEFAULT_ALARM_ALERT_URI
        mediaPlayer = MediaPlayer.create(this, alarmURI, null, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(), 0)
        mediaPlayer.isLooping = true
        mediaPlayer.start()

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(500, 500), 0))
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