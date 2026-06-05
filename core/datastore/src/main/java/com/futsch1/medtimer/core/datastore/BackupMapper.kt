package com.futsch1.medtimer.core.datastore

import androidx.core.net.toUri
import com.futsch1.medtimer.core.domain.backup.SettingsBackup
import com.futsch1.medtimer.core.domain.model.BackupInterval
import com.futsch1.medtimer.core.domain.model.DismissNotificationAction
import com.futsch1.medtimer.core.domain.model.HomeLocation
import com.futsch1.medtimer.core.domain.model.ThemeSetting
import com.futsch1.medtimer.core.domain.model.UserPreferences
import java.time.LocalTime

fun UserPreferences.toSettingsBackup(): SettingsBackup = SettingsBackup(
    weekendStartTimeMinutes = weekendStartTime.toSecondOfDay() / 60,
    weekendEndTimeMinutes = weekendEndTime.toSecondOfDay() / 60,
    weekendMode = weekendMode,
    weekendDays = weekendDays,
    exactReminders = exactReminders,
    repeatReminders = repeatReminders,
    numberOfRepetitions = numberOfRepetitions,
    repeatDelayMinutes = repeatDelay.inWholeMinutes,
    snoozeDurationMinutes = snoozeDuration.inWholeMinutes,
    overrideDnd = overrideDnd,
    stickyOnLockscreen = stickyOnLockscreen,
    dismissNotificationAction = dismissNotificationAction.name,
    cannotSkipReminders = cannotSkipReminders,
    bigNotifications = bigNotifications,
    combineNotifications = combineNotifications,
    useRelativeDateTime = useRelativeDateTime,
    showTakenTimeInOverview = showTakenTimeInOverview,
    systemLocale = systemLocale,
    theme = theme.name,
    hideMedicineName = hideMedicineName,
    appAuthentication = appAuthentication,
    useSecureWindow = useSecureWindow,
    disableWidget = disableWidget,
    alarmRingtone = alarmRingtone?.toString(),
    noAlarmSoundWhenSilent = noAlarmSoundWhenSilent,
    noVibrationWhenSilent = noVibrationWhenSilent,
    automaticBackupInterval = automaticBackupInterval.name,
    automaticBackupDirectory = automaticBackupDirectory?.toString(),
    locationBasedSnooze = locationBasedSnooze,
    homeLatitude = homeLocation?.latitude,
    homeLongitude = homeLocation?.longitude,
    homeRadiusMeters = homeLocation?.radiusMeters,
)

fun SettingsBackup.applyTo(preferencesDataSource: PreferencesDataSource) {
    preferencesDataSource.setWeekendStartTime(
        LocalTime.of(weekendStartTimeMinutes / 60, weekendStartTimeMinutes % 60)
    )
    preferencesDataSource.setWeekendEndTime(
        LocalTime.of(weekendEndTimeMinutes / 60, weekendEndTimeMinutes % 60)
    )
    preferencesDataSource.putBoolean(PreferencesDataSource.WEEKEND_MODE, weekendMode)
    preferencesDataSource.putStringSet(PreferencesDataSource.WEEKEND_DAYS, weekendDays)
    preferencesDataSource.putBoolean(PreferencesDataSource.EXACT_REMINDERS, exactReminders)
    preferencesDataSource.putBoolean(PreferencesDataSource.REPEAT_REMINDERS, repeatReminders)
    preferencesDataSource.putString(PreferencesDataSource.NUMBER_OF_REPETITIONS, numberOfRepetitions.toString())
    preferencesDataSource.putString(PreferencesDataSource.REPEAT_DELAY, repeatDelayMinutes.toString())
    preferencesDataSource.putString(PreferencesDataSource.SNOOZE_DURATION, snoozeDurationMinutes.toString())
    preferencesDataSource.putBoolean(PreferencesDataSource.OVERRIDE_DND, overrideDnd)
    preferencesDataSource.putBoolean(PreferencesDataSource.STICKY_ON_LOCKSCREEN, stickyOnLockscreen)
    preferencesDataSource.putString(
        PreferencesDataSource.DISMISS_NOTIFICATION_ACTION,
        DismissNotificationAction.valueOf(dismissNotificationAction).ordinal.toString()
    )
    preferencesDataSource.putBoolean(PreferencesDataSource.CANNOT_SKIP_REMINDERS, cannotSkipReminders)
    preferencesDataSource.putBoolean(PreferencesDataSource.BIG_NOTIFICATIONS, bigNotifications)
    preferencesDataSource.putBoolean(PreferencesDataSource.COMBINE_NOTIFICATIONS, combineNotifications)
    preferencesDataSource.putBoolean(PreferencesDataSource.USE_RELATIVE_DATE_TIME, useRelativeDateTime)
    preferencesDataSource.putBoolean(PreferencesDataSource.SHOW_TAKEN_TIME_IN_OVERVIEW, showTakenTimeInOverview)
    preferencesDataSource.putBoolean(PreferencesDataSource.SYSTEM_LOCALE, systemLocale)
    preferencesDataSource.putString(
        PreferencesDataSource.THEME,
        ThemeSetting.valueOf(theme).ordinal.toString()
    )
    preferencesDataSource.putBoolean(PreferencesDataSource.HIDE_MED_NAME, hideMedicineName)
    preferencesDataSource.putBoolean(PreferencesDataSource.APP_AUTHENTICATION, appAuthentication)
    preferencesDataSource.putBoolean(PreferencesDataSource.SECURE_WINDOW, useSecureWindow)
    preferencesDataSource.putBoolean(PreferencesDataSource.DISABLE_WIDGET, disableWidget)
    preferencesDataSource.putString(PreferencesDataSource.ALARM_RINGTONE, alarmRingtone)
    preferencesDataSource.putBoolean(PreferencesDataSource.NO_ALARM_SOUND_WHEN_SILENT, noAlarmSoundWhenSilent)
    preferencesDataSource.putBoolean(PreferencesDataSource.NO_VIBRATION_WHEN_SILENT, noVibrationWhenSilent)
    preferencesDataSource.setAutomaticBackupInterval(BackupInterval.valueOf(automaticBackupInterval))
    automaticBackupDirectory?.let { preferencesDataSource.setAutomaticBackupDirectory(it.toUri()) }
    preferencesDataSource.putBoolean(PreferencesDataSource.LOCATION_SNOOZE_ENABLED, locationBasedSnooze)
    val lat = homeLatitude
    val lon = homeLongitude
    if (lat != null && lon != null) {
        preferencesDataSource.saveHomeLocation(HomeLocation(lat, lon, homeRadiusMeters ?: 150f))
    } else {
        preferencesDataSource.clearHomeLocation()
    }
}
