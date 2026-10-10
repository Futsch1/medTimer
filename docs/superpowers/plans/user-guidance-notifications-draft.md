# Notifications missing or late

> Draft for review. Not yet bundled in the app. App labels and behavior are source-reviewed. Android settings vary by version and device; live permission, battery, and alarm-settings walkthroughs remain unverified.

Start by checking whether the reminder is scheduled when you expect it. Then check whether Android allows the app to show it. These are different problems: a scheduled entry in Overview does not confirm notification delivery.

## 1. Check setup and the expected time

1. Open **Medicine** and select your medicine.
2. Check that it has a reminder. A medicine without reminders does not schedule notifications. If needed, follow [Getting started](user-guidance-getting-started-draft.md).
3. Check the reminder's time, dosage, and whether it is active.
4. If you changed **Advanced settings**, review the weekdays, days of the month, active date range, and cycle. These can exclude the day you are checking.
5. Open **Overview** and select the expected date. Include **Scheduled** in any selected status filters, or clear all status filters. Check any tag filter too.

You can also open **Medicine**, select your medicine, and use its **Calendar** link (calendar icon) to check only that
medicine's schedule. Browse future dates and select a day to see its scheduled reminders and times. This validates the
schedule, not notification delivery.

For a daily time-based reminder created after today's chosen time, check tomorrow. A valid schedule can have nothing due on the selected day.

Other settings can explain a different time or a deliberately absent notification:

- Interval reminders have a starting time or daily window and a choice of what starts the next interval. Review those values rather than expecting a fixed clock time.
- **Settings → Weekend mode** can move reminders within its configured time window on selected days.
- A reminder configured to be automatically marked taken records the dose without showing its usual reminder notification. Review that setting if you did not intend this behavior.

Do not mark a dose taken just to test delivery. If the expected upcoming time is correct, continue with the delivery checks below.

## 2. Check Android notification permission and categories

On Android 13 and later, MedTimer needs notification permission. Allow notifications when prompted. If you previously denied permission, open Android's app settings for MedTimer and check whether notifications are allowed.

In MedTimer, open **Settings** from the main overflow menu, then **Notification and reminder settings**. The following entries open Android's notification-category settings:

- **Notification settings default priority**
- **Notification settings high priority**
- **Notification settings out of stock**

Check that the category used by your reminder is enabled. Check its sound settings if a notification appears but is silent. The medicine's **Notification priority**, and any reminder-specific priority override, determine which dose-reminder category to check. Category settings affect other reminders using the same category too.

Android's silent mode and Do Not Disturb settings can affect what you hear. MedTimer's **Always play notification sound** option requests additional Android access when needed; do not assume enabling it guarantees sound in every device state.

> Still to verify before publication: the exact Android permission and category navigation on supported versions, denial/regrant behavior, and sound behavior with silent mode and Do Not Disturb. Android's menu names may differ from those in MedTimer.

## 3. Check timing and battery settings

In **Settings → Notification and reminder settings**:

- **Exact reminders** is available on Android 12 and later. Enabling it may ask you to allow exact alarms in Android settings. Check both the app option and Android's permission. Without the app option and required permission, MedTimer uses inexact alarms; notifications can be later than the displayed schedule. Exact alarms do not guarantee delivery or audible alerts.
- **Ignore battery optimization** opens an Android request to exempt MedTimer from battery optimization. The entry is hidden if the app is already exempt. If you experience missed or late notifications, review this setting and your device's app battery restrictions.

Some devices have additional background-app restrictions. This guide does not cover every manufacturer's settings, and changing battery settings is not proof that future notifications will arrive.

> Still to verify before publication: exact-alarm grant/revoke and return-to-app behavior, the battery-exemption request on supported Android versions, and device-specific restrictions. Do not infer a maximum delay from this draft.

### What the Overview warnings mean

Overview may show **Battery optimization is enabled** or **Exact reminders are disabled**. These are prompts to review settings, not a notification-delivery test.

Selecting **OK** dismisses a warning persistently; it does not enable the setting or grant permission. The exact-reminders warning also has an **Enable** action, which turns on the app option and opens Android's exact-alarm permission settings if needed. Follow through with the system permission check.

The exact-reminders warning reflects the app option, not an independent check that Android permits exact alarms. A missing warning does not prove that the required permission is granted. You can still open **Settings → Notification and reminder settings** after dismissing either warning.

## 4. Check what happened after a reminder appeared

These settings change an existing reminder's behavior; they do not fix a missing initial schedule or replace notification permission:

- **Repeat reminders** controls additional reminders after a delay, up to the configured number of repetitions, until marked taken or skipped.
- **Snooze settings → Snooze duration** controls how long a snoozed reminder is delayed.
- **Action when dismissing notification** controls what happens when you dismiss a notification. Review it if a reminder is unexpectedly skipped, taken, or snoozed.
- **Reminders cannot be skipped** restricts dismissal actions, globally or per medicine.
- **Persistent reminders on lock screen** changes dismissal behavior on Android 14 and later; it is not a permission to deliver notifications.

If you use **High + Alarm** priority, Android may also require access for full-screen alarms on Android 14 and later. Review **Alarm settings** for the alarm ringtone and silent-mode sound/vibration options. These are separate from exact-alarm scheduling permission.

> Still to verify before publication: full-screen access prompts and behavior when locked/unlocked, alarm sound and vibration settings, repeat limits, and dismissal interactions on supported versions. Ordinary notification and alarm behavior must be checked separately.

_Last reviewed: 2026-10-10._
