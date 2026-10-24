# Adjust reminder schedules

These settings describe when MedTimer schedules entries; they do not recommend when a medicine should be taken. Use a
schedule provided by your health professional.

Open **Medicine**, select a medicine, open the reminder, then choose **Advanced settings**. Available options depend on
the reminder type.

## Choose days and date limits

For a time-based reminder, use **Remind on weekdays** or **Remind on days of month** to limit the dates when it is
active. An empty selection means no restriction for that option.

To limit a reminder to a date range, open **Reminder status** in Advanced settings and enable **Period start** and/or
**Period end**. A reminder outside its active range is not scheduled.

## Set a repeating active/pause cycle

Under **Cyclic reminder**, review **First cycle start**, **Active days**, and **Pause days**. MedTimer repeats the
active and pause portions from the start date. Check the resulting dates in the medicine calendar before relying on the
configuration. Changing the start date shifts the cycle.

## Understand interval reminders

A continuous interval uses an interval and a start time. **Interval start condition** determines whether the next
interval is based on the reminder being raised or the event being processed (for example, marked taken or skipped). The
selected condition changes how later reminders are calculated.

A windowed interval also has daily start and end times. It schedules within that configured window; the next occurrence
may fall in a later window. Review the generated dates in the Overview or medicine calendar. The scheduled entries are
previews, not confirmation that notifications were delivered.

## Understand weekend mode

On selected days, weekend mode postpones reminders scheduled within the interval from the start time up to (but not
including) the end time. Reminders in that interval are moved to the end time. For example, if the interval is 1:00 to
9:00, a reminder scheduled for 8:00 is postponed to 9:00, but a reminder at 0:30 is still raised at 0:30.

## Add a reminder linked to another

In Advanced settings, choose **Add reminder for following dose** to add a linked reminder. Its timing is relative to the
earlier reminder event being processed. Linked reminders can be chained, and each delay is limited to 24 hours. Review
the resulting schedule after changing the earlier reminder or its processing status.

[More tasks](more-tasks.md) · [Help contents](index.md)
