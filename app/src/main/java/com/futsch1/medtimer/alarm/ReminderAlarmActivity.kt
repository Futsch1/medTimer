package com.futsch1.medtimer.alarm

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.R
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.preferences.PreferencesDataSource
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
    lateinit var preferencesDataSource: PreferencesDataSource

    @Inject
    lateinit var vibrator: Vibrator

    @Inject
    lateinit var audioManager: AudioManager

    private var mediaPlayer: MediaPlayer? = null
    private var buildMediaPlayerJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        val tmpMediaPlayer = MediaPlayer.create(
            audioContext,
            preferencesDataSource.preferences.value.alarmRingtone ?: Settings.System.DEFAULT_ALARM_ALERT_URI,
            null,
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
            0
        ) ?: MediaPlayer.create(
            audioContext,
            Settings.System.DEFAULT_ALARM_ALERT_URI,
            null,
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
            0
        )
        if (tmpMediaPlayer != null) {
            tmpMediaPlayer.isLooping = true
            mediaPlayer = tmpMediaPlayer
        }
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
        mediaPlayer = null
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
        return combinePreferenceAndRingerMode(preferencesDataSource.preferences.value.noAlarmSoundWhenSilent)
    }

    private fun shallVibrate(): Boolean {
        return combinePreferenceAndRingerMode(preferencesDataSource.preferences.value.noVibrationWhenSilent)
    }

    private fun combinePreferenceAndRingerMode(preferenceValue: Boolean): Boolean {
        if (preferenceValue) {
            // If the silent mode is active, do not ring the alarm
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
            context: Context,
            reminderNotificationData: ReminderNotificationData
        ): Intent {
            val intent = Intent(context, ReminderAlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            reminderNotificationData.toIntent(intent)
            return intent
        }
    }
}
