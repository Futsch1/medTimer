package com.futsch1.medtimer.core.domain.backup

data class SettingsBackup(
    val weekendStartTimeMinutes: Int,
    val weekendEndTimeMinutes: Int,
    val weekendMode: Boolean,
    val weekendDays: Set<String>,
    val exactReminders: Boolean,
    val repeatReminders: Boolean,
    val numberOfRepetitions: Int,
    val repeatDelayMinutes: Long,
    val snoozeDurationMinutes: Long,
    val overrideDnd: Boolean,
    val stickyOnLockscreen: Boolean,
    val dismissNotificationAction: String,
    val cannotSkipReminders: Boolean,
    val bigNotifications: Boolean,
    val combineNotifications: Boolean,
    val useRelativeDateTime: Boolean,
    val showTakenTimeInOverview: Boolean,
    val systemLocale: Boolean,
    val theme: String,
    val hideMedicineName: Boolean,
    val appAuthentication: Boolean,
    val useSecureWindow: Boolean,
    val disableWidget: Boolean,
    val alarmRingtone: String?,
    val noAlarmSoundWhenSilent: Boolean,
    val noVibrationWhenSilent: Boolean,
    val automaticBackupInterval: String,
    val automaticBackupDirectory: String?,
    val locationBasedSnooze: Boolean,
    val homeLatitude: Double?,
    val homeLongitude: Double?,
    val homeRadiusMeters: Float?,
    val prescriptionPickupDays: Int = 3,
    val prescriptionContact: String? = null,
    val prescriptionMessageTemplate: String? = null,
    // Nullable and added after this class already shipped: Gson leaves it null (rather than the
    // JVM zero-value primitives get) when restoring a backup written before this field existed, so
    // that case is distinguishable from "restoring an empty PersistentDataBackup" for free.
    val persistentData: PersistentDataBackup? = null
)
