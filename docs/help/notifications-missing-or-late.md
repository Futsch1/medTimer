# Notifications missing or late

A reminder shown in **Overview** is a schedule preview, not proof that Android delivered a notification.

First check the schedule in MedTimer. If it is correct, check Android's permission and notification settings. A schedule
problem and a delivery problem need different checks.

## 1. Check the schedule in MedTimer

1. Open **Medicine**, select the medicine, and check that it has a reminder. A medicine without a reminder does not
   schedule notifications.
2. Check that the reminder is enabled, and review its time or interval, dosage, weekdays, active date range, and cycle
   where applicable. Use **Advanced settings** to review recurrence limits.
3. Open **Overview**, select the expected date, and check the scheduled entry. Include **Scheduled** in the status
   filters, or clear the status filters. Check any tag filter too. A valid schedule may have nothing due on the day
   currently selected.
4. For one medicine's schedule, open its **Calendar** link from the medicine screen and select a date. This is also a
   schedule preview, not a delivery check.

For a daily time-based reminder created after today's chosen time, check tomorrow. Other settings can shift or suppress
an expected notification:

- **Weekend mode** can move reminders within its configured time window on selected days.
- An interval reminder follows its starting time or daily window; it may not occur at a fixed clock time.
- A reminder set to **Automatically mark as taken** is not shown as its usual reminder notification.

Do not mark a dose as taken just to test a notification. If the schedule and expected time look right, continue with
Android's settings.

## 2. Check Android notification permission and categories

On Android 13 and later, allow MedTimer's notification permission. If permission was denied or changed, open Android
**Settings** for MedTimer and check that notifications are allowed. Android's menu labels can vary by version and
device.

In MedTimer, open **Settings → Notification and reminder settings**. These entries open Android's settings for MedTimer
notification categories:

- **Notification settings default priority**
- **Notification settings high priority**
- **Notification settings out of stock**

Check that the relevant category is enabled. A medicine's **Notification priority**, or a reminder-specific priority
override, determines which dose-reminder category to check. If a notification appears but is silent, inspect that
category's sound settings. Category settings can also affect other reminders using the same category.

Silent mode and Do Not Disturb can affect sound. **Always play notification sound** can request Do Not Disturb access
when needed; enabling it does not guarantee sound in every device state.

## 3. Review exact-reminder and battery settings

In **Settings → Notification and reminder settings**:

- **Exact reminders** is available on Android 12 and later. If exact alarms are not allowed, MedTimer uses inexact
  alarms, so a reminder may appear later than its scheduled time (from a few seconds to several minutes). Exact alarms
  do not guarantee delivery or sound.
- **Ignore battery optimization** opens an Android request to exempt MedTimer from battery optimization. This entry is
  hidden when Android reports that the app is already exempt. If notifications are missing or late, you can also review
  the device's app battery restrictions.

Some devices add their own background-app restrictions. This guide does not cover every manufacturer's settings;
changing a setting does not prove that future notifications will arrive on time.

### What the Overview warnings mean

Overview may show **Battery optimization is enabled** or **Exact reminders are disabled**. These are prompts to review
settings, not tests of notification delivery.

Selecting **OK** dismisses a warning persistently. It does not change the related setting or grant Android permission.
The exact-reminders warning's **Enable** action turns on the app's **Exact reminders** option and opens Android's
exact-alarm permission settings if permission is needed.

The exact-reminders warning checks the app option; it does not independently certify Android's exact-alarm permission. A
dismissed or absent warning does not prove that Android will deliver a reminder. You can always open **Settings →
Notification and reminder settings**, or open this guide from the main **Help** entry after dismissing a warning.

## 4. If a notification appeared but behaved unexpectedly

These settings affect what happens after a reminder is raised; they do not fix a missing schedule or replace
notification permission:

- **Repeat reminders** controls additional reminders after a delay, until marked taken or skipped.
- **Snooze settings → Snooze duration** controls how long a snoozed reminder is delayed.
- **Action when dismissing notification** controls the action taken when a notification is dismissed. **Reminders cannot
  be skipped** can restrict dismissal actions globally or for an individual medicine.
- **High + Alarm** uses alarm presentation. On Android 14 and later, full-screen alarm access is separate from
  exact-alarm scheduling permission. Review **Alarm settings** for ringtone and silent-mode sound or vibration options.

These options behave separately from notification permission and scheduling. If the schedule is correct and Android
settings appear correct but notifications are still missing or late, MedTimer cannot confirm the cause or guarantee
delivery; Android behavior also depends on device software and settings.

[Help contents](index.md)
