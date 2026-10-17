# Getting started

> Draft for review. Not yet bundled in the app. Labels and steps have been checked against source and existing tests;
> no live walkthrough was performed. Source inspection is accepted for this draft; screenshots and later UI verification
> belong to the implementation tasks.

MedTimer keeps a medicine and its reminders separately. First add the medicine, then add a reminder for it. Adding a
medicine alone does not schedule a notification.

This example uses **Medicine A**, a fictional medicine, with a daily reminder at **08:00** and the example dosage **1**.
These values demonstrate app setup, not a recommended medication schedule. For your own medicines, use the times and
dosage you have been instructed to follow.

## 1. Add a medicine

1. Open **Medicine** from the main navigation.
2. Select **Add medicine** (the plus button).
3. Enter **Medicine A** in **Medicine name** and select **OK**.

The medicine's edit screen opens. You have created the medicine, but it does not have a reminder yet. If you leave now,
open **Medicine** and select **Medicine A** to continue later.

## 2. Add its reminder

1. On the medicine's edit screen, select **Add reminder**.
2. Choose **Time based reminder**.
3. Enter **1** in **Dosage**.
4. Select **Time** and set it to **08:00**. Check AM/PM if your time picker uses a 12-hour clock, then confirm the time.
5. Select **Create reminder**.

The app shows **Successfully created new reminder** and returns to the medicine's edit screen. Check that the reminder
is listed with the intended time and dosage.

A new time-based reminder is active every day unless you change its scheduling settings. You do not need to configure a
cycle or select weekdays for this daily example.

### Which type should I choose?

- **Time based reminder** uses a clock time, such as 08:00 each day. This is the type used in this example.
- **Continuous interval reminder** uses an interval from a starting date and time. It can continue across days.
- **Windowed interval reminder** uses an interval within a daily start and end time.

Interval reminders also let you choose whether the interval starts when reminded or when marked taken or skipped. That
choice can change upcoming times. Choose the type that matches your intended setup; MedTimer does not choose a medical
schedule for you. Stock and expiration-date reminders serve different purposes and are not needed for this example.

## 3. Check the upcoming time

1. Open **Overview** from the main navigation.
2. Select the date when you expect the next reminder. For this example, if today's 08:00 has already passed, select
   tomorrow.
3. Find **Medicine A** and check the displayed dosage and time against what you entered.

Overview can show past events as well as scheduled reminders. If you have selected status filters, include **Scheduled
**, or clear all status filters to show all states. If a tag filter is active, make sure it includes your medicine.

An empty selected day does not necessarily mean setup failed: a reminder may be due on another day. If the expected
reminder is missing or its time is wrong, return to **Medicine**, select the medicine, and check its reminder and
scheduling settings. See [Notifications missing or late](user-guidance-notifications-draft.md) for separate setup and
delivery checks.

### Check this medicine's calendar

You can also validate the schedule for one medicine without the other medicines shown in Overview:

1. Open **Medicine** and select **Medicine A**.
2. Select the **Calendar** link (calendar icon) on the medicine's edit screen.
3. Browse to the date you want to check and select it to see that medicine's scheduled reminders, including their times.
   Browse other future dates to check the recurring pattern.

This calendar shows the medicine's future schedule for the dates available in the calendar, as well as past events.
Use it to compare the scheduled reminders with your intended setup, especially when you have changed weekdays or cycles.

An upcoming entry in Overview or the medicine's calendar is a preview of the schedule, **not confirmation that Android has delivered a notification**.
Notification permission, Android settings, and battery restrictions can affect delivery. When the reminder becomes due,
check the actual notification separately; do not treat the preview or the creation message as a delivery test.

_Last reviewed: 2026-10-10._
