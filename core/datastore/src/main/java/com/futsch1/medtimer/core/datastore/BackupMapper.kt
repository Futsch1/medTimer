package com.futsch1.medtimer.core.datastore

import androidx.core.net.toUri
import com.futsch1.medtimer.core.domain.backup.PersistentDataBackup
import com.futsch1.medtimer.core.domain.backup.SettingsBackup
import com.futsch1.medtimer.core.domain.model.BackupInterval
import com.futsch1.medtimer.core.domain.model.DismissNotificationAction
import com.futsch1.medtimer.core.domain.model.HomeLocation
import com.futsch1.medtimer.core.domain.model.OverviewFilter
import com.futsch1.medtimer.core.domain.model.PersistentData
import com.futsch1.medtimer.core.domain.model.StatisticFragment
import com.futsch1.medtimer.core.domain.model.ThemeSetting
import com.futsch1.medtimer.core.domain.model.UserPreferences
import java.time.LocalTime

fun PersistentData.toPersistentDataBackup(): PersistentDataBackup = PersistentDataBackup(
    analysisDays = analysisDays,
    iconColor = iconColor,
    activeStatisticsFragment = activeStatisticsFragment.name,
    lastCustomDose = lastCustomDose,
    lastCustomDoseAmount = lastCustomDoseAmount,
    filterTags = filterTags,
    checkedFilters = checkedFilters.map { it.name }.toSet()
)

fun PersistentDataBackup.applyTo(persistentDataDataSource: PersistentDataDataSource) {
    persistentDataDataSource.setAnalysisDays(analysisDays)
    persistentDataDataSource.setIconColor(iconColor)
    persistentDataDataSource.setActiveStatisticsFragment(
        runCatching { StatisticFragment.valueOf(activeStatisticsFragment) }.getOrDefault(StatisticFragment.CHARTS)
    )
    persistentDataDataSource.setLastCustomDose(lastCustomDose)
    persistentDataDataSource.setLastCustomDoseAmount(lastCustomDoseAmount)
    persistentDataDataSource.setFilterTags(filterTags)
    persistentDataDataSource.setCheckedFilters(
        checkedFilters.mapNotNull { runCatching { OverviewFilter.valueOf(it) }.getOrNull() }.toSet()
    )
}

fun UserPreferences.toSettingsBackup(persistentData: PersistentData): SettingsBackup = SettingsBackup(
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
    prescriptionPickupDays = prescriptionPickupDays,
    prescriptionContact = prescriptionContact,
    prescriptionMessageTemplate = prescriptionMessageTemplate,
    persistentData = persistentData.toPersistentDataBackup(),
)

fun SettingsBackup.applyTo(preferencesDataSource: PreferencesDataSource, persistentDataDataSource: PersistentDataDataSource) {
    persistentData?.applyTo(persistentDataDataSource)
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
    // Gson bypasses the constructor default for this field (added after SettingsBackup already
    // shipped), so an old backup JSON missing the key deserializes it to 0, not 3 - treat 0 as
    // "not present in this backup" rather than overwriting the current setting with a bogus value.
    if (prescriptionPickupDays > 0) {
        preferencesDataSource.putString(
            PreferencesDataSource.PRESCRIPTION_PICKUP_DAYS,
            prescriptionPickupDays.toString()
        )
    }
    prescriptionContact?.let { preferencesDataSource.putString(PreferencesDataSource.PRESCRIPTION_CONTACT, it) }
    prescriptionMessageTemplate?.let {
        preferencesDataSource.putString(PreferencesDataSource.PRESCRIPTION_MESSAGE_TEMPLATE, it)
    }
}
