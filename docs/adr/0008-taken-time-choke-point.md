---
status: accepted
---

# Record the taken time at one choke point

One choke point records the taken time. `NotificationProcessor.setReminderEventStatus` takes an optional processed time, and `ReminderCommandBus.markReminderEvents` forwards it. The `Taken at...` action supplies a picked time, and the event row and the stock change both carry it. The amount dialog of `Taken at...` forwards the picked time, so the entered amount and the stock change carry it. Without a picked time, a dose marked taken records the current time, as before. A picked time is never after the current time, because interval and linked reminders derive their next due time from the recorded time. Every path that marks a dose taken passes through this point: the Overview, the notification buttons, the variable amount dialog and automatically taken reminders.
