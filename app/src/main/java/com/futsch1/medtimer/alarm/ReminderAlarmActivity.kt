package com.futsch1.medtimer.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.ActivityCodes.EXTRA_NOTIFICATION_ID
import com.futsch1.medtimer.ActivityCodes.EXTRA_NOTIFICATION_TIME_STRING
import com.futsch1.medtimer.R
import com.futsch1.medtimer.reminders.NotificationTriplet
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ReminderAlarmActivity : AppCompatActivity() {

    // A single-threaded coroutine dispatcher for handling media player and vibrator operations
    private val alarmExecutor = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

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

        lifecycleScope.launch(alarmExecutor) {
            buildMediaPlayerAndVibrator()
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(alarmExecutor) {
            startAlarm()
        }
    }

    override fun onPause() {
        super.onPause()
        lifecycleScope.launch(alarmExecutor) {
            pauseAlarm()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (alarmExecutor.executor as ExecutorService).awaitTermination(1, TimeUnit.SECONDS)
        releaseMediaPlayer()
        Log.d("ReminderAlarm", "Destroyed alarm activity")
    }

    private fun buildMediaPlayerAndVibrator() {
        val alarmURI = PreferenceManager.getDefaultSharedPreferences(this)
            .getString("alarm_ringtone", Settings.System.DEFAULT_ALARM_ALERT_URI.toString())!!.toUri()
        val audioContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            createAttributionContext("audioPlayback")
        } else {
            this@ReminderAlarmActivity
        }
        mediaPlayer =
            MediaPlayer.create(
                audioContext,
                alarmURI,
                null,
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
                0
            )
        mediaPlayer.isLooping = true

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun startAlarm() {
        // Launch a coroutine on the single-threaded dispatcher
        Log.d("ReminderAlarm", "Executing startAlarm job")

        if (shallPlayAlarm()) {
            playAlarmTone()
        }

        if (shallVibrate()) {
            vibrate()
        }
    }

    private fun pauseAlarm() {
        Log.d("ReminderAlarm", "Executing pauseAlarm job")

        try {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
            }
        } catch (_: IllegalStateException) {
            // Ignore
        }

        vibrator.cancel()
    }

    private fun releaseMediaPlayer() {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
        Log.d("ReminderAlarm", "Released media player")
    }

    private fun vibrate() {
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(500, 500), 0))
    }

    private fun playAlarmTone() {
        mediaPlayer.start()
    }

    private fun shallPlayAlarm(): Boolean {
        return combinePreferenceAndRingerMode("no_alarm_sound_when_silent")
    }

    private fun shallVibrate(): Boolean {
        return combinePreferenceAndRingerMode("no_vibration_when_silent")
    }

    private fun combinePreferenceAndRingerMode(preferenceName: String): Boolean {
        val preferenceValue = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(preferenceName, false)
        if (preferenceValue) {
            // If the silent mode is active, do not ring the alarm
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            return audioManager.ringerMode != AudioManager.RINGER_MODE_SILENT
        }
        return true
    }

    private fun addAlarmFragment(intent: Intent?) {
        if (intent != null) {
            Log.d("ReminderAlarm", "Adding alarm fragment")
            supportFragmentManager.beginTransaction().add(R.id.alarmFragmentContainer, AlarmFragment::class.java, intent.extras).commit()
        }
    }

    companion object {
        fun getIntent(context: Context, notificationTriplets: List<NotificationTriplet>, remindTime: String, notificationId: Int): Intent {
            val intent = Intent(context, ReminderAlarmActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

            val bundle = Bundle()
            NotificationTriplet.toBundle(bundle, notificationTriplets)
            bundle.putInt(EXTRA_NOTIFICATION_ID, notificationId)
            bundle.putString(EXTRA_NOTIFICATION_TIME_STRING, remindTime)
            intent.putExtras(bundle)

            return intent
        }
    }
}