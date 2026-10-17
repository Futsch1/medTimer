package com.futsch1.medtimer.robots

import androidx.annotation.StringRes

/**
 * The app settings. The interface is the setting to change; finding it and coming back out again is
 * the implementation, so a new nesting level does not move where every caller ends up.
 */
class SettingsRobot(private val menus: MenuRobot, private val preferences: PreferenceScreenRobot) {

    /**
     * Opens the settings, walks down [path] of nested preference screens, runs [block] there, and
     * returns to the screen the caller started from.
     */
    fun inSection(@StringRes vararg path: Int, block: () -> Unit) {
        menus.clickAppOption(com.futsch1.medtimer.core.ui.R.string.tab_settings)
        path.forEach { preferences.click(it) }
        block()
        preferences.leave(path.size + 1)
    }

    /** Opens the notification troubleshooting topic from its contextual link in notification settings. */
    fun openNotificationsHelp() {
        menus.clickAppOption(com.futsch1.medtimer.core.ui.R.string.tab_settings)
        preferences.click(com.futsch1.medtimer.core.ui.R.string.notification_reminder_settings)
        preferences.click(com.futsch1.medtimer.core.ui.R.string.notifications_missing_or_late)
    }

    fun returnToOverviewFromNotificationSettings() = preferences.leave(2)

    /** Clicks the last entry of [path], with the entries before it read as nested screens. */
    fun click(@StringRes vararg path: Int) {
        val sections = path.dropLast(1).toIntArray()
        inSection(*sections) { preferences.click(path.last()) }
    }
}
