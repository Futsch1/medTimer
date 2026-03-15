package com.futsch1.medtimer.preferences

import android.net.Uri
import android.provider.Settings
import java.time.LocalTime
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

enum class DismissNotificationAction {
    SKIP, SNOOZE, TAKE
}

enum class ThemeSetting {
    DEFAULT, ALTERNATIVE
}

enum class BackupInterval {
    NEVER, DAILY, WEEKLY, MONTHLY
}

data class MedTimerSettings(
    // Reminder settings
    val weekendTime: LocalTime,
    val weekendMode: Boolean,
    val weekendDays: Set<String>,
    val exactReminders: Boolean,
    val repeatReminders: Boolean,
    val numberOfRepetitions: Int,
    val repeatDelay: Duration,
    val snoozeDuration: Duration,
    val overrideDnd: Boolean,
    val stickyOnLockscreen: Boolean,
    val dismissNotificationAction: DismissNotificationAction,
    // Display settings
    val bigNotifications: Boolean,
    val combineNotifications: Boolean,
    val useRelativeDateTime: Boolean,
    val showTakenTimeInOverview: Boolean,
    val systemLocale: Boolean,
    val theme: ThemeSetting,
    // Security settings
    val hideMedicineName: Boolean,
    val appAuthentication: Boolean,
    val useSecureWindow: Boolean,
    // Alarm settings
    val alarmRingtone: Uri,
    val noAlarmSoundWhenSilent: Boolean,
    val noVibrationWhenSilent: Boolean,
    // Automatic backup settings
    val automaticBackupInterval: BackupInterval
) {
    companion object {
        fun default(): MedTimerSettings {
            return MedTimerSettings(
                weekendTime = LocalTime.of(9, 0),
                weekendMode = false,
                weekendDays = emptySet(),
                exactReminders = false,
                repeatReminders = false,
                numberOfRepetitions = 3,
                repeatDelay = 10.toDuration(DurationUnit.MINUTES),
                snoozeDuration = 15.toDuration(DurationUnit.MINUTES),
                overrideDnd = false,
                stickyOnLockscreen = false,
                dismissNotificationAction = DismissNotificationAction.SKIP,
                bigNotifications = false,
                combineNotifications = false,
                useRelativeDateTime = false,
                showTakenTimeInOverview = true,
                systemLocale = false,
                theme = ThemeSetting.DEFAULT,
                hideMedicineName = false,
                appAuthentication = false,
                useSecureWindow = false,
                alarmRingtone = Settings.System.DEFAULT_ALARM_ALERT_URI,
                noAlarmSoundWhenSilent = false,
                noVibrationWhenSilent = false,
                automaticBackupInterval = BackupInterval.NEVER
            )
        }
    }

    // Used from unit tests
    @Suppress("unused")
    fun copyWith(overrides: MedTimerSettings.() -> Unit): MedTimerSettings {
        val copy = this.copy()
        overrides(copy)
        return copy
    }
}

val DEFAULT_MEDTIMER_SETTINGS = MedTimerSettings.default()
