package com.futsch1.medtimer.alarm

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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.R
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.preferences.MedTimerPreferencesDataSource
import com.futsch1.medtimer.reminders.ReminderContext
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class ReminderAlarmActivity : AppCompatActivity() {

    @Inject
    @Dispatcher(MedTimerDispatchers.Default)
    lateinit var backgroundDispatcher: CoroutineDispatcher

    @Inject
    lateinit var preferencesDataSource: MedTimerPreferencesDataSource

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var vibrator: Vibrator
    private var buildMediaPlayerJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContentView(R.layout.activity_alarm)

        addAlarmFragment(intent)

        buildMediaPlayerJob = lifecycleScope.launch(backgroundDispatcher) {
            buildMediaPlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(backgroundDispatcher) {
            startAlarm()
        }
    }

    override fun onPause() {
        super.onPause()
        lifecycleScope.launch(backgroundDispatcher) {
            pauseAlarm()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaPlayer()
        Log.d("ReminderAlarm", "Destroyed alarm activity")
    }

    private suspend fun awaitMediaPlayer(): MediaPlayer? {
        buildMediaPlayerJob?.join()
        return mediaPlayer
    }

    private fun buildMediaPlayer() {
        val audioContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            createAttributionContext("audioPlayback")
        } else {
            this@ReminderAlarmActivity
        }
        mediaPlayer =
            MediaPlayer.create(
                audioContext,
                preferencesDataSource.data.value.alarmRingtone ?: Settings.System.DEFAULT_ALARM_ALERT_URI,
                null,
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
                0
            ).apply { isLooping = true }
    }

    private suspend fun startAlarm() {
        Log.d("ReminderAlarm", "Executing startAlarm job")

        if (shallPlayAlarm()) {
            playAlarmTone()
        }

        if (shallVibrate()) {
            vibrate()
        }
    }

    private suspend fun pauseAlarm() {
        Log.d("ReminderAlarm", "Executing pauseAlarm job")

        val mediaPlayer = awaitMediaPlayer() ?: return

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
        mediaPlayer?.release()
        Log.d("ReminderAlarm", "Released media player")
    }

    private fun vibrate() {
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(500, 500), 0))
    }

    private suspend fun playAlarmTone() {
        val mediaPlayer = awaitMediaPlayer() ?: return
        mediaPlayer.start()
    }

    private fun shallPlayAlarm(): Boolean {
        return combinePreferenceAndRingerMode(preferencesDataSource.data.value.noAlarmSoundWhenSilent)
    }

    private fun shallVibrate(): Boolean {
        return combinePreferenceAndRingerMode(preferencesDataSource.data.value.noVibrationWhenSilent)
    }

    private fun combinePreferenceAndRingerMode(preferenceValue: Boolean): Boolean {
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
            supportFragmentManager.beginTransaction()
                .add(R.id.alarmFragmentContainer, AlarmFragment::class.java, intent.extras).commit()
        }
    }

    companion object {
        fun getIntent(
            reminderContext: ReminderContext,
            reminderNotificationData: ReminderNotificationData
        ): Intent {
            val intent = Intent()
            reminderContext.setIntentClass(intent, ReminderAlarmActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

            reminderNotificationData.toIntent(intent)
            return intent
        }
    }
}
