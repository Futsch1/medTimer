# User guidance — task 1 evidence inventory

**Audience:** Contributors reviewing the beginner and notification drafts.
**Snapshot:** 2026-10-10, source revision `e756cf57`.
**Scope:** [Task 1](user-guidance-tasks.md#task-1--inventory-behavior-and-draft-content), governed by the [agreed plan](user-guidance.md).

## Deliverables and verification boundary

- [Getting started draft](user-guidance-getting-started-draft.md): complete medicine → reminder → type choice → upcoming-time path.
- [Notifications missing or late draft](user-guidance-notifications-draft.md): scheduling checks separated from delivery checks, with unverified Android claims flagged inline.
- This inventory holds implementation references, defaults, migration candidates, and gaps; those do not belong in the user instructions.

These are review drafts alongside the plan, **not a settled production source layout**. Task 2 selects the reader, canonical layout, destinations, and packaging. No app behavior, resource strings, reader dependencies, or existing user instructions were changed. [Use cases](../../UseCases.md) remains available as existing documentation pending section-by-section review, not proof of current behavior.

The walkthrough below follows source and creation-owning tests. It was **not performed live**: `adb devices -l` returned no connected devices. Do not mark the live walkthrough or task 1 wholly complete until it is verified. Debug-only tests do not establish release first-launch, warning, permission, or delivery behavior.

## Beginner walkthrough: current labels, navigation, and defaults

| Step | Current behavior and labels | Evidence |
| --- | --- | --- |
| Enter setup | Main destination is **Medicine** (singular), not “Medicines”. Add button is **Add medicine**. Main navigation adapts to window size; avoid “bottom tab” as a universal instruction. | [English strings][strings], [MedicinesFragment][medicines], [MedicinesScreen][medicine-screen] |
| Create medicine | Name dialog uses **Medicine name**, **OK**, **Cancel**. OK trims the name, writes a medicine, then navigates directly to its edit screen. There is no separate Save step here and no automatic reminder creation. | `addMedicine`, `getAlertBuilder`, `navigateToMedicineId` in [MedicinesFragment][medicines] |
| Resume interrupted setup | Reopen **Medicine** and select the medicine card. The medicine persists without reminders; medicine-only input schedules nothing. | [MedicinesRobot][medicines-robot]; `scheduleEmptyLists` in [ReminderSchedulerUnitTest][scheduler-test] |
| Start reminder | Medicine edit screen has **Add reminder**. It opens a type chooser, not the dosage dialog immediately. | `setupAddReminderButton` in [EditMedicineFragment][editor]; [NewReminderTypeDialog][type-dialog] |
| Choose type | Chooser explicitly offers **Time based reminder**, **Continuous interval reminder**, **Windowed interval reminder**, plus stock and expiration choices. Existing cards already have explanatory help strings; task 5 should review/adapt them, not assume guidance is wholly absent. | [NewReminderTypeDialog][type-dialog], [English strings][strings] (`*_reminder_help`) |
| Daily example | Time-based dialog fields are **Dosage** and **Time**, with **Create reminder** / **Cancel**. Default clock time is 08:00; reminder is active, cycle is 1 active / 0 pause days, no weekday/month-day/date limits, no automatic-taking or variable amount. Empty weekday/month-day lists mean unrestricted, not “no days”. | [NewReminderDialog][reminder-dialog], [dialog layout][reminder-layout], [Reminder defaults][reminder-model], [StandardScheduling][standard-scheduling] |
| Confirm creation | Valid time input writes the reminder, displays **Successfully created new reminder**, dismisses the dialog, and leaves the medicine editor open. The reminder list updates from repository flow. Creation does not currently navigate to Overview or show a next-step help hint. | [NewReminderDialog][reminder-dialog], [EditMedicineFragment][editor] |
| Check upcoming | Go to **Overview**, select the expected date, compare name, dosage, and time. Daily reminder creation after the chosen time produces the next day's occurrence. Overview merges recorded events with simulated upcoming reminders; a preview is not confirmation of delivery. | `getFiltered` in [OverviewViewModel][overview-vm], [SimulatedReminderEvent][simulated-event], `scheduleSameDayReminder` / `scheduleEmptyLists` in [ReminderSchedulerUnitTest][scheduler-test] |
| Avoid false “missing setup” | Day selection, status filters, and tag filters can hide a valid schedule. Status labels: **Taken**, **Skipped**, **Raised**, **Scheduled**. No active status filters shows all states; otherwise pending previews require Scheduled. Time display is optional for existing users. | [OverviewFilterRow][filters], `isOverviewEventVisible` in [OverviewViewModel][overview-vm], [SimulatedReminderEvent][simulated-event], [display settings][display-settings] |

The draft uses synthetic **Medicine A**, dosage **1**, and **08:00** solely to demonstrate daily fixed-time setup. It expressly makes no clinical recommendation. If the time is already past, the reader checks tomorrow rather than expecting an immediate notification.

Interval defaults: continuous start is set from the current instant; creation interval editor starts at 12 hours. Windowed start/end defaults are 08:00/23:00. “Interval starts when reminded” is initially selected. **Unresolved:** the windowed creation dialog displays the start-condition radio group, but `NewReminderDialog` copies `intervalStartsFromProcessed` only for continuous intervals. Verify windowed creation versus later editing before publishing detailed interval steps; do not silently fix it in documentation work.

## Which tests actually own creation?

| Evidence | What it establishes (when run) | What it does not establish |
| --- | --- | --- |
| `BasicUITest.basicUITest` | Calls `medicines.create` and `medicineEditor.addReminder`, then edits/duplicates and asserts Overview content. | A clean, single-reminder beginner capture or real notification delivery. |
| `ReminderTest.reminderTypeTest` | Creates a medicine through UI; creates time-based, linked, continuous, and windowed reminders; checks time/amount in Overview. | Production permissions, beginner-only presentation, or all interval option combinations. |
| `MedicineHandlingTest.medicineMoveTest` | Owns medicine name creation/navigation, then rename and reorder. | Reminder creation or delivery. |
| [MedicinesRobot][medicines-robot] / [MedicineEditorRobot][editor-robot] | Expose actual dialog actions, picker confirmation, type selection, and creation toast. | Passing results in this session: robots are source evidence only. |
| Other `ReminderTest` cases, most `NotificationTest` cases, `BasicUITest.menuHandlingTest` / filters | Seed repository fixtures, then exercise editing, display, scheduling actions, or notifications. | UI creation; `seed.medicine` and `seed.remindersOf` bypass the creation dialogs. |
| `ScreenshotsTest.screenshotsTest` | Uses existing Screengrab/locale harness and debug **Generate test data** action for populated store captures. | First-use setup, creation, empty states, or focused English help screenshot provenance. |
| JVM scheduler tests | Time calculations, empty medicine behavior, and next-day scheduling using synthetic models. | UI labels or real Android delivery. |

Test files: [BasicUITest][basic-test], [ReminderTest][reminder-test], [MedicineHandlingTest][medicine-test], [NotificationTest][notification-test], [ScreenshotsTest][screenshots-test], [Seed][seed]. Keep the existing UI ownership when adding focused documentation scenarios.

## Notification and Android settings inventory

Main overflow **Settings** → **Notification and reminder settings** is the current permanent route after warnings disappear. Settings and processing are shared by both flavors. No bundled Help entry exists yet.

| Setting or behavior | Current default / implementation | Evidence and boundary |
| --- | --- | --- |
| Notification permission | Android 13+ POST_NOTIFICATIONS requested when absent. Processing checks Android permission before showing a notification. | [MainActivity][main], [RequestPostNotificationPermission][permission], [ReminderNotificationProcessor][notification-processor]. Denial/regrant not exercised here. |
| Notification categories | **Notification settings default priority**, **… high priority**, **… out of stock** open Android channel settings. Medicine priority and reminder override select the applicable dose channel. | [NotificationSettingsFragment][notification-settings], [Reminder model][reminder-model], [Medicine model][medicine-model]. System category sound/disable screens need live review. |
| Exact reminders | Default off. Preference visible on Android 12+; enabling prompts if exact-alarm permission is absent. On return, missing permission resets the preference. Alarm processing uses exact alarms only if preference and required system permission allow it; otherwise inexact. | [NotificationSettingsFragment][notification-settings], [AlarmProcessor][alarm]. API 28–30 UI hides this preference although scheduler still reads it; avoid claiming all versions expose the switch. |
| Battery exemption | **Ignore battery optimization** requests Android exemption; hidden if already exempt. | [NotificationSettingsFragment][notification-settings]. No manufacturer-specific or real-device reliability evidence. |
| Do Not Disturb | **Always play notification sound**, default off, requests notification-policy access; resets when access is missing on resume. | [notification preferences][notification-xml], [NotificationSettingsFragment][notification-settings]. Sound behavior needs separate verification; setting name is not a guarantee. |
| Repeat | **Repeat reminders**, off; configured default 3 repetitions, 10-minute delay. | [UserPreferences][preferences], [repeat preferences][repeat-xml], [RepeatProcessor][repeat]. Not a remedy for permission or setup errors. |
| Dismiss / skip | **Action when dismissing notification**, default Skip. **Reminders cannot be skipped**, off globally; per-medicine option also exists. Enabling globally changes dismissal from Skip to Snooze if necessary. | [UserPreferences][preferences], [NotificationSettingsFragment][notification-settings], [NotificationTest][notification-test]. Test fixtures bypass creation. |
| Snooze | **Snooze settings → Snooze duration**, default 15 minutes; custom duration exists. Location-based snooze is flavor-dependent and outside the beginner slice. | [UserPreferences][preferences], [snooze preferences][snooze-xml], `customSnooze` in [NotificationTest][notification-test]. |
| Lock screen | **Persistent reminders on lock screen**, off, visible only Android 14+. | [notification preferences][notification-xml], [NotificationSettingsFragment][notification-settings]. Not proof of delivery. |
| Alarm presentation | **High + Alarm** priority; Android 14+ full-screen access check separate from exact-alarm permission. **Alarm settings** exposes ringtone and silent-mode sound/vibration options, both suppression options default off. | [RequestPostNotificationPermission][permission], [UserPreferences][preferences], [alarm preferences][alarm-xml], `alarmTest` in [NotificationTest][notification-test]. Locked/unlocked presentation unverified here. |
| Scheduling / display | **Weekend mode** off; can defer times on configured days. Automatic-taking reminder setting off; processing records Taken and filters it out of notification display. Display settings can hide reminded time, combine notifications, or use relative times. | [UserPreferences][preferences], [ReminderNotificationProcessor][notification-processor], [SimulatedReminderEvent][simulated-event], `weekendMode` in [ReminderTest][reminder-test]. |

### Warnings, dismissal, and intro

- Battery warning: visible only when warnings are not suppressed, exemption is absent, and persistent dismissal flag is false.
- Exact-reminders warning: visible only when warnings are not suppressed, **app preference** is false, and persistent dismissal flag is false. It does not independently read system exact-alarm permission.
- Both **OK** actions persist dismissal. Exact warning **Enable** sets the app preference, persists dismissal, and requests system exact-alarm access if needed. Disappearance is not proof of system permission or delivery.
- Debug builds suppress both warnings. [ExactRemindersWarningTest][warning-test] injects `@IsDebugBuild = false` to exercise the warning, but the harness pregrants exact-alarm and notification permissions; that does not cover denied system permission.
- Release first launch starts the six-slide intro when `introShown` is false, then immediately persists `introShown = true`. Later launches check notification permission. Debug first launch bypasses the intro.
- Slides: welcome, Medicine, Reminders, Show notifications, Overview, Analysis. On Android 13+, AppIntro registers notification permission for slide index 1. **Skip** explicitly calls the permission helper then finishes; **Done** finishes and relies on AppIntro's permission flow. Live skip/complete/deny behavior must be confirmed before modifying it.
- The main overflow **Show intro** replay action is debug-only. `BasicUITest.appIntro` replays, checks welcome, skips, and checks Overview; it is not release first-launch or denied-permission coverage.
- Permission-helper denial writes persistent `showNotifications = false`. Repository search found no current consumption of that field by notification display; do **not** document it as an available global “Show notifications” switch or claim it independently disables delivery. The intro title is not a settings control.

Evidence: [OverviewWarnings][warning-state], [Warnings UI][warnings-ui], [OverviewViewModel][overview-vm], [OverviewScreen][overview-screen], [PersistentDataDataSource][persistent], [MainActivity][main], [MedTimerAppIntro][intro], [AppOptionsMenu][menu], [test harness][harness]. Task 2 must choose release-like verification for intro and warnings without weakening production checks.

## Existing use-case migration candidates

All sections remain at [docs/UseCases.md](../../UseCases.md). This is a candidate map, not verified replacement coverage; no section is retired in task 1.

| Existing section | Proposed destination / review need |
| --- | --- |
| Birth control pills | More tasks: configure active/pause cycles. Replace medical example with synthetic schedule; verify current cycle labels. |
| Tapering off a medicine | More tasks: limit reminders to date ranges. Explain app configuration without recommending dose changes. |
| Reminder every n weeks | More tasks: weekday and cycle recurrence. Recheck arithmetic and current selectors. |
| Disable a medicine | More tasks: deactivate/reactivate all reminders. |
| Validate reminder settings | Beginner: upcoming-time check; More tasks: preview a medicine calendar. Replace notification certainty with schedule-preview wording. |
| Different notification sounds | Troubleshooting: correct notification category; More tasks: customize sounds/priority. |
| Modify events | More tasks: correct a logged event. Verify current swipe/action gestures. |
| Additional dose with preset amount | More tasks: log a manual dose using a preset. |
| Export medication history | More tasks: export events as CSV/PDF. Review menu labels and privacy warning; never commit exports. |
| Further customization to snooze | More tasks: snooze duration/custom snooze. Android notification snoozing is version/device-specific. |
| More nagging, repeating reminders | Troubleshooting: distinguish repeats from delivery fixes; More tasks: configure repetitions/delay. |
| Doses that shall be taken at a specific time after the previous dose (following doses) | More tasks: linked reminders. Verify Taken/Skipped timing semantics and maximum delay; remove medical schedule suggestion. |
| Interval reminders | Beginner: short type distinction only; More tasks: continuous interval/start condition. Replace “exactly the same” delivery implications. |
| Medicine stock tracking | More tasks: stock, refill, thresholds, and stock-reminder types. Old settings description needs review against separate reminder creation. |
| Stock tracking for another person | More tasks: automatic-taking/stock bookkeeping. Explicitly distinguish automatic records from confirmed doses. |
| Using MedTimer for several people | More tasks: organize/filter with tags. |
| Intervals during daytime | Beginner: short type distinction; More tasks: windowed interval and start condition (creation gap above). |
| Alarm reminders | Troubleshooting / More tasks: alarm priority and full-screen access. Verify device-state claims. |

## Content, screenshot, and verification gaps

Before treating the drafts as publishable:

1. Perform the fresh synthetic daily setup live, including interrupt/resume, type choice, time picker, creation feedback, and the next-day Overview check. Confirm ordinary Android Back and main navigation on phone/tablet.
2. Confirm existing-user filters and hidden-time behavior. Verify the draft's route through **Settings → Display settings → Show reminded time in overview** when time is hidden.
3. Verify Android notification denial/regrant, exact-alarm grant/revoke, battery exemption, DND, and full-screen alarms on supported versions. Do not promise a fixed delay, guaranteed delivery, or exhaustive manufacturer coverage.
4. Verify release intro first launch, skip, Done, and notification request behavior; verify persistent warning dismissal and access to troubleshooting afterward. Existing pregranting/debug harness is insufficient for these cases.
5. Resolve the windowed creation start-condition claim before detailed interval instructions; keep advanced topics outside the first beginner slice.
6. Task 2 must settle reader/layout/internal links, language notice, offline and release packaging, large text, screen-reader usability, and Back. These drafts do not claim bundled Help exists.
7. Task 4 needs English-only captures of: no medicines; name creation dialog; medicine without reminders; type chooser; time-based reminder entry with synthetic dosage/time; created reminder; Overview with expected upcoming date/time. Capture interrupted/resumed setup if its UI differs. Notification-settings and release warning captures can follow with task 8.
8. Existing numeric-name store captures and `docs/*.png` have not been visually revalidated or approved for the new guide. Do not copy them as current beginner evidence. Reuse Screengrab, LocaleTestRule, harness, and robots; assert state before capture. Commit approved images later, with provenance and a focused capture command.

None of these gaps requires complete feature documentation before the reader spike. Keep unverified claims visible; do not mark blocked checks passed.

## Verification performed

```bash
adb devices -l
# No connected devices: live walkthrough and instrumented verification not run.

./gradlew :feature:ui:testFullDebugUnitTest \
  --tests 'com.futsch1.medtimer.feature.ui.overview.OverviewWarningsTest' \
  --tests 'com.futsch1.medtimer.feature.ui.overview.WarningsTest' \
  :app:testFullDebugUnitTest \
  --tests 'com.futsch1.medtimer.schedulertests.ReminderSchedulerUnitTest' \
  --tests 'com.futsch1.medtimer.processortests.AlarmProcessorTest' --console=plain
```

Result: **BUILD SUCCESSFUL**; 24 tests, zero failures/errors/skips (8 warning-state, 2 warning-button, 12 scheduler, 2 alarm-processor). Alarm tests verify requested API calls, not actual delivery. No new/changed instrumented tests, full instrumented suite, coverage run, reader/build assets, or app changes. Both-flavor/release packaging and build/lint gates remain for implementation/release tasks, not claimed by this documentation-only inventory.

_Last reviewed: 2026-10-10._

[strings]: ../../../core/ui/src/main/res/values/strings.xml
[medicines]: ../../../feature/ui/src/main/java/com/futsch1/medtimer/feature/ui/medicine/MedicinesFragment.kt
[medicine-screen]: ../../../feature/ui/src/main/java/com/futsch1/medtimer/feature/ui/medicine/MedicinesScreen.kt
[editor]: ../../../feature/ui/src/main/java/com/futsch1/medtimer/feature/ui/medicine/EditMedicineFragment.kt
[type-dialog]: ../../../feature/ui/src/main/java/com/futsch1/medtimer/feature/ui/medicine/dialogs/NewReminderTypeDialog.kt
[reminder-dialog]: ../../../feature/ui/src/main/java/com/futsch1/medtimer/feature/ui/medicine/dialogs/NewReminderDialog.kt
[reminder-layout]: ../../../feature/ui/src/main/res/layout/dialog_new_reminder.xml
[reminder-model]: ../../../core/domain/src/main/java/com/futsch1/medtimer/core/domain/model/Reminder.kt
[medicine-model]: ../../../core/domain/src/main/java/com/futsch1/medtimer/core/domain/model/Medicine.kt
[standard-scheduling]: ../../../feature/reminders/api/src/main/java/com/futsch1/medtimer/feature/reminders/api/scheduling/StandardScheduling.kt
[overview-vm]: ../../../feature/ui/src/main/java/com/futsch1/medtimer/feature/ui/overview/OverviewViewModel.kt
[overview-screen]: ../../../feature/ui/src/main/java/com/futsch1/medtimer/feature/ui/overview/OverviewScreen.kt
[simulated-event]: ../../../feature/ui/src/main/java/com/futsch1/medtimer/feature/ui/overview/model/SimulatedReminderEvent.kt
[filters]: ../../../feature/ui/src/main/java/com/futsch1/medtimer/feature/ui/overview/OverviewFilterRow.kt
[display-settings]: ../../../feature/ui/src/main/res/xml/display_settings.xml
[medicines-robot]: ../../../app/src/androidTest/java/com/futsch1/medtimer/robots/MedicinesRobot.kt
[editor-robot]: ../../../app/src/androidTest/java/com/futsch1/medtimer/robots/MedicineEditorRobot.kt
[basic-test]: ../../../app/src/androidTest/java/com/futsch1/medtimer/BasicUITest.kt
[reminder-test]: ../../../app/src/androidTest/java/com/futsch1/medtimer/ReminderTest.kt
[medicine-test]: ../../../app/src/androidTest/java/com/futsch1/medtimer/MedicineHandlingTest.kt
[notification-test]: ../../../app/src/androidTest/java/com/futsch1/medtimer/NotificationTest.kt
[screenshots-test]: ../../../app/src/androidTest/java/com/futsch1/medtimer/ScreenshotsTest.kt
[seed]: ../../../app/src/androidTest/java/com/futsch1/medtimer/harness/Seed.kt
[scheduler-test]: ../../../app/src/test/java/com/futsch1/medtimer/schedulertests/ReminderSchedulerUnitTest.kt
[main]: ../../../app/src/main/java/com/futsch1/medtimer/MainActivity.kt
[permission]: ../../../feature/ui/src/main/java/com/futsch1/medtimer/feature/ui/RequestPostNotificationPermission.kt
[notification-processor]: ../../../feature/reminders/src/main/java/com/futsch1/medtimer/feature/reminders/ReminderNotificationProcessor.kt
[notification-settings]: ../../../feature/ui/src/main/java/com/futsch1/medtimer/feature/ui/preferences/NotificationSettingsFragment.kt
[notification-xml]: ../../../feature/ui/src/main/res/xml/notification_settings.xml
[preferences]: ../../../core/domain/src/main/java/com/futsch1/medtimer/core/domain/model/UserPreferences.kt
[alarm]: ../../../feature/reminders/src/main/java/com/futsch1/medtimer/feature/reminders/AlarmProcessor.kt
[repeat]: ../../../feature/reminders/src/main/java/com/futsch1/medtimer/feature/reminders/RepeatProcessor.kt
[repeat-xml]: ../../../feature/ui/src/main/res/xml/repeat_reminders_preferences.xml
[snooze-xml]: ../../../feature/ui/src/main/res/xml/snooze_settings.xml
[alarm-xml]: ../../../feature/ui/src/main/res/xml/alarm_settings.xml
[warning-state]: ../../../feature/ui/src/main/java/com/futsch1/medtimer/feature/ui/overview/OverviewWarnings.kt
[warnings-ui]: ../../../feature/ui/src/main/java/com/futsch1/medtimer/feature/ui/overview/Warnings.kt
[warning-test]: ../../../app/src/androidTest/java/com/futsch1/medtimer/ExactRemindersWarningTest.kt
[persistent]: ../../../core/datastore/src/main/java/com/futsch1/medtimer/core/datastore/PersistentDataDataSource.kt
[intro]: ../../../app/src/main/java/com/futsch1/medtimer/MedTimerAppIntro.kt
[menu]: ../../../feature/ui/src/main/java/com/futsch1/medtimer/feature/ui/AppOptionsMenu.kt
[harness]: ../../../app/src/androidTest/java/com/futsch1/medtimer/harness/MedTimerTestHarness.kt
