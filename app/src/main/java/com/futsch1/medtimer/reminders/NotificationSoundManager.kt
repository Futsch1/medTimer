package com.futsch1.medtimer.reminders

import android.app.NotificationManager
import android.media.AudioManager
import android.os.Build
import com.futsch1.medtimer.preferences.PreferencesNames
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages notification sound settings and "Do Not Disturb" overrides for reminders.
 *
 * This class handles the temporary unmuting of the ringer and modification of the notification
 * policy to ensure that medication reminders are audible. It tracks the original state of the
 * audio settings and restores them after a brief delay once the reminder has been triggered.
 */
class NotificationSoundManager(reminderContext: ReminderContext) {
    private val notificationManager: NotificationManager =
        reminderContext.notificationManager
    private val audioManager: AudioManager =
        reminderContext.audioManager

    init {
        if (notificationManager.isNotificationPolicyAccessGranted() && reminderContext.preferences.getBoolean(
                PreferencesNames.OVERRIDE_DND,
                false
            )
        ) {
            loadPendingRingerMode(audioManager, notificationManager)
        }
    }

    fun restore() {
        restorePendingRingerMode(audioManager)
    }

    companion object {
        private var pending = false
        private var pendingWasMuted = false
        private var restoreJob: Job? = null
        private val scope = CoroutineScope(Dispatchers.Default)

        @Synchronized
        fun loadPendingRingerMode(
            audioManager: AudioManager,
            notificationManager: NotificationManager
        ) {
            if (!pending) {
                pending = true
                if (audioManager.isStreamMute(AudioManager.STREAM_RING) && notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL) {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_RING,
                        AudioManager.ADJUST_UNMUTE,
                        0
                    )
                    pendingWasMuted = true
                }
                val policy = notificationManager.notificationPolicy
                notificationManager.notificationPolicy = NotificationManager.Policy(
                    policy.priorityCategories or NotificationManager.Policy.PRIORITY_CATEGORY_REMINDERS,
                    policy.priorityCallSenders,
                    policy.priorityMessageSenders,
                    0
                )
            }
        }

        @Synchronized
        fun restorePendingRingerMode(
            audioManager: AudioManager
        ) {
            if (pending) {
                restoreJob?.cancel()
                restoreJob = scope.launch {
                    delay(5000)
                    performRestore(audioManager)
                }
            }
        }

        @Synchronized
        private fun performRestore(audioManager: AudioManager) {
            if (pendingWasMuted) {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_RING,
                    AudioManager.ADJUST_MUTE,
                    0
                )
                if (Build.VERSION.SDK_INT > 28) {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                }
            }
            pending = false
            pendingWasMuted = false
        }
    }
}
