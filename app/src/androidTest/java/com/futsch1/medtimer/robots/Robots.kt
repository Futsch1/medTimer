package com.futsch1.medtimer.robots

import androidx.annotation.StringRes
import androidx.compose.ui.test.junit4.ComposeTestRule

/**
 * Builds the robot graph. One construction site, so a robot that starts needing another is wired
 * here rather than by giving the test base another field.
 */
class Robots(rule: ComposeTestRule) {

    private val ui = ComposeUi(rule)

    val dialogs = DialogRobot(ui)
    val pickers = MaterialPickers()
    val preferences = PreferenceScreenRobot(ui, dialogs)
    val navigation = NavigationRobot(ui)
    val menus = MenuRobot(ui)

    val overview = OverviewRobot(ui)
    val medicines = MedicinesRobot(ui, navigation, dialogs)
    val statistics = StatisticsRobot(ui)
    val settings = SettingsRobot(menus, preferences)
    val notifications = NotificationShadeRobot()

    val reminderSettings = ReminderSettingsRobot(ui, preferences, pickers, dialogs)
    val reminders = ReminderListRobot(ui, reminderSettings)
    val medicineEditor = MedicineEditorRobot(ui, menus, preferences, pickers)
    val medicineSettings = MedicineSettingsRobot(menus, preferences, IconColorPickerRobot())
    val eventEditor = EventEditorRobot(overview)
    val manualDose = ManualDoseRobot(overview, dialogs, pickers)
    val tags = TagsRobot(menus, dialogs)
    val notes = MedicineNotesRobot(menus)
    val calendar = MedicineCalendarRobot(menus)
    val appIntro = AppIntroRobot(ui, menus)
    val alarm = AlarmScreenRobot()
    val shareSheet = ShareSheetRobot()
    val export = ExportRobot(menus, shareSheet)

    fun getString(@StringRes textRes: Int): String = ui.getString(textRes)
}
