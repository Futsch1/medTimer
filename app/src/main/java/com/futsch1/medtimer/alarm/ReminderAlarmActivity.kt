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
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.ActivityCodes.EXTRA_NOTIFICATION_ID
import com.futsch1.medtimer.ActivityCodes.EXTRA_NOTIFICATION_TIME_STRING
import com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_EVENT_ID
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.ReminderEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReminderAlarmActivity(
    private val ioCoroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AppCompatActivity() {
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var vibrator: Vibrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        keyguardManager.requestDismissKeyguard(this, null)

        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContentView(R.layout.activity_alarm)

        addAlarmFragment(intent)
    }

    override fun onResume() {
        super.onResume()
        startAlarmTone()
    }

    override fun onPause() {
        super.onPause()
        if (this::mediaPlayer.isInitialized) {
            mediaPlayer.stop()
            mediaPlayer.release()
        }
        if (this::vibrator.isInitialized) {
            vibrator.cancel()
        }
    }

    private fun startAlarmTone() {
        lifecycleScope.launch {
            withContext(ioCoroutineDispatcher) {
                val alarmURI = Settings.System.DEFAULT_ALARM_ALERT_URI
                mediaPlayer =
                    MediaPlayer.create(this@ReminderAlarmActivity, alarmURI, null, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(), 0)
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
        }
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
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

            val bundle = Bundle()
            bundle.putInt(EXTRA_REMINDER_EVENT_ID, reminderEvent.reminderEventId)
            bundle.putInt(EXTRA_NOTIFICATION_ID, notificationId)
            bundle.putString(EXTRA_NOTIFICATION_TIME_STRING, remindTime)
            intent.putExtras(bundle)

            return intent
        }
    }
}
