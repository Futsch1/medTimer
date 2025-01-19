# Use cases for MedTimer

Find here some common use cases and how MedTimer should be setup to fulfill them properly.

## Birth control pills

If you are using birth control pills that require one week per cycle where no doses are required,
cyclic reminders in MedTimer can be setup to support that scheme.

Create a medicine and one reminder, there go to `Advanced settings`. In the `Cyclic reminders`
section, select the day when you would take the first pill in `First cycle start` (can be both a
future or a past date, the cycle will be calculated only based on that date and reminders would
also be active before that date). Now enter the number of days you have to take a pill (usually
in `Active days`. Finally, enter the number of days you are skipping the dose (usually 7) in
`Pause days`.

Should your cycle change, adjust the `First cycle start` to the new first pill's date.

<img src='cyclic_reminder_birth_control.png' width=200 />

## Tapering off a medicine

When tapering of a medicine, you would usually reduce the amount taken after a certain period. While
it is possible to manually change the amount, it is also possible to setup the scheme for
tapering of in advance.

For this, create one reminder per desired amount and set them active only in a certain time
period in the `Advanced settings`. If you set these periods adjacent to each other, only one
reminder with a given amount will be active at a time.

<img src='time_period_reminders.png' width=200 />

## Reminder every n weeks

If you have a reminder that should only notify every n weeks or days, you can use `Cyclic
reminders`. Go to `Advanced settings` and set `First cycle start` to one of the dates when the
reminder should notify. Set `Active days` to 1 (because you only want one reminder at that
certain day) and set `Pause days` to the number of days between two reminders. So if you want a
bi-weekly reminder, `Pause days` should be set to 13 (14 days - 1 day).

Note that weekly reminders can be setup much more easily by selecting the weekdays using the
`Remind on` button in the `Advanced settings`.

## Disable a medicine

To quickly disable a medicine completely, select the medicine from the `Medicine` screen, hit
the options menu icon on the top right and select `Deactivate all reminders`. This way, all
reminders will remain in the medicine with their configuration, but they will not create any
notifications. They can be re-activated the same way.

<img src='deactivate.png' width=200 />

## Validate reminder settings

Especially when using cyclic reminders, it can be tricky to make sure that all reminders of a
medicine are setup correctly and will trigger the desired notification behavior. To validate if
the setup was correct, the calendar view of the medicine can be used. It is opened from the
calendar icon in the `Medicine` screen next to `Notification priority`.

On this screen, you can see both past and future doses of a medicine. Days where a reminder will
be scheduled are marked with an underline. Select them to see the reminders that would notify on
that date at the bottom. Swipe to change the month.

<img src='medicine_calendar.png' width=200 />

## Different notification sounds

It is possible to set different sounds to the reminders of two classes of medicines: Default
priority and high priority medicines. The priority can be changed in the `Medicine` screen.

Each priority has separate notification settings that can be accessed via the MedTimer
settings screens using the `Notification settings ...` menu items in the `Settings` menu.

On the corresponding screen, you can configure the behavior of notifications in detail, turning
notifications on and off, assigning different sounds and controlling notification behavior. Note
that settings there have an effect on all medicines assigned to the corresponding category.

<img src='notification_settings.png' width=200 />

## Modify events

Events can be modified after they have been marked taken or skipped. The `Taken` or `Skipped`
status can be switched by selecting the corresponding buttons on the `Overview` screen.
Furthermore, details of the reminder can be changed as well by swiping the event to the right.
The event's name, the dosage and the reminded time can be edited. For additional doses, also the
date can be modified.

<img src='edit_event.png' width=200 />

## Additional dose with preset amount

On the `Overview` screen, the `Log additional dose` screen asks to select the medicine and the
dosage to create an additional event. If you have a reoccurring dose with a fixed amount, you
can add this dose to the corresponding medicine and set it to `Inactive`. Inactive reminders
will show up in the medicine selection screen when creating an additional dose with their amount
in brackets. Select these entries to skip entering the amount manually.

<img src='additional_dose.png' width=200 />

## Export medication history

With MedTimer, it is possible to export the history of past doses either for your own reference
or to show to medical staff. MedTimer supports two different formats for medication history: CSV
and PDF. The CSV export is suited to be opened in a spreadsheet editor for further processing,
filtering and sorting and provides flexibility to handle the data. The PDF export generated a
file with a table of all past events that can be used to forward it to medical staff directly.

To export the history, open the options menu from the `Overview`, `Medicine` or `Analysis`
screen. Select `Event data` and choose the export format desired.

<img src='export.png' width=200 />

## Further customization to snooze

If you need a more flexible approach to snoozing reminders like setting the snooze time individually
per reminder, you can use the Android settings for snooze notifications (`Settings > Notifications >
Allow notification snoozing`).

<img src='notification_snoozing.png' width=200 />

You can also set the snooze interval to `Custom...`, which allows you to enter the snooze interval
for every snoozed reminder individually when it is snoozed.

## More nagging, repeating reminders

Reminders can be automatically repeated after a certain delay for a specific number of times. This
feature can be enabled via the settings menu via `Repeat reminders`.

## Doses that shall be taken at a specific time after the previous dose (following doses)

If you have a medicine that requires a dose to be taken at a specific time after the previous
dose, you can create a reminder to notify at a specific time after the previous dose was marked
taken or skipped. This can be done in the `Advanced settings` of the medicine. Use the button `Add 
reminder for following dose` to add a reminder for the following dose. Set the dosage and the delay.

Following doses can overlap into the next day, but are constrained to a maximum delay of 24 hours.
It is also possible to chain following doses by entering the `Advanced settings` of a following dose
and adding another following dose from there.

However, these chained following doses need one dose at a specific time to start the chain. It is
still possible to implement a continuous interval scheme by using one dummy timed reminder that
starts the dosing scheme for the day. This reminder can then be marked taken when the last dose of
the past day's chain was taken.

So consider the following setup:

1. Dummy reminder at like 08:00, dose 0
2. Reminder following 1. 8:00 later
3. Reminder following 2. 8:00 later
4. Reminder following 3. 8:00 later

So imagine you would mark the dummy reminder as taken at 08:30, the first following reminder will
be active at 16:30, the second following reminder will be active at 00:30 and the third following
reminder will be active at 08:30 on the next day. As soon as you took this last dose, mark the dummy
reminder as taken, which will restart the chain.

## Interval reminders

In contrast to following doses, interval reminders always keep a certain interval and are not
dependent on time triggers. Use these if you need to keep an interval which is not bound to 24 hours
(e.g. every 10 hours) or if it is really important that the interval between each doses stays
exactly the same. For both use cases, the interval reminder can be configured. It can either start
when the previous dose has been reminded, consequently keeping a fixed interval between reminders
at all times. Or it can be configured to start when the previous dose was marked either taken or
skipped, keeping a fixed interval between taken times.

Additionally, it is required to set a start time of the interval. This time will mark the first
dose and is used to calculate subsequent doses. If the interval shall be changed, this interval
start time should be set to the next reminder time.

## Medicine stock tracking

MedTimer can track the stock of your medicine, reducing the amount of medicine for every dose
that has been taken. To enter the stock settings, click the parcel icon in the medicine view. In the
following screen, you can set the current amount, the out of stock reminder type and the reminder
threshold. It is also possible to indicate a refill with a single click. The out of stock reminder
can either be triggered the moment when the threshold is reached for the first time or every time a
dose was taken and the remaining amount is below the threshold. When stock tracking is active, the
remaining amount will also be shown in the medicine list and in every reminder of said medicine. If
the amount is below threshold, a warning icon `⚠` will be shown.

<img src='medicine_stock.png' width="200" />

The amount to be deducted comes from the amount indicated for the reminders. The amount can also
contain text, MedTimer will search for the first number in the amount and use this (e.g. `75 mg` or
`Take 1 pill` will work fine).
