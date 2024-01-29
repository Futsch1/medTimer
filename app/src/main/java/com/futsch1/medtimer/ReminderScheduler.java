package com.futsch1.medtimer;

import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class ReminderScheduler {
    private final ScheduleListener listener;
    private final TimeAccess timeAccess;
    private List<MedicineWithReminders> medicineWithReminders;
    private List<ReminderEvent> reminderEvents;

    public ReminderScheduler(ScheduleListener listener, TimeAccess timeAccess) {
        this.listener = listener;
        this.timeAccess = timeAccess;
    }

    public void updateMedicine(List<MedicineWithReminders> medicineWithReminders) {
        this.medicineWithReminders = medicineWithReminders;
        schedule();
    }

    private void schedule() {
        if (this.reminderEvents != null && this.medicineWithReminders != null && this.medicineWithReminders.size() > 0) {
            ArrayList<Reminder> sortedReminders = getSortedReminders();

            if (sortedReminders.size() > 0) {
                Reminder nextReminder = null;
                LocalDate checkDate = this.timeAccess.localDate();
                for (Reminder reminder : sortedReminders) {
                    if (!wasRaisedToday(reminder)) {
                        nextReminder = reminder;
                        break;
                    }
                }
                if (nextReminder == null) {
                    checkDate = checkDate.plusDays(1);
                    nextReminder = sortedReminders.get(0);
                }

                ZoneOffset offset = timeAccess.systemZone().getRules().getOffset(checkDate.atStartOfDay());
                OffsetDateTime localTime = OffsetDateTime.of(checkDate, LocalTime.of(nextReminder.timeInMinutes / 60, nextReminder.timeInMinutes % 60), offset);
                Instant targetDate = localTime.toInstant();

                this.listener.schedule(targetDate, getMedicine(nextReminder), nextReminder);
            }
        }
    }

    private ArrayList<Reminder> getSortedReminders() {
        ArrayList<Reminder> sortedReminders = new ArrayList<>();
        for (MedicineWithReminders medicineWithReminder : medicineWithReminders
        ) {
            sortedReminders.addAll(medicineWithReminder.reminders);
        }
        sortedReminders.sort((Reminder a, Reminder b) -> a.timeInMinutes == b.timeInMinutes ? (a.medicineRelId - b.medicineRelId) : (a.timeInMinutes - b.timeInMinutes));
        return sortedReminders;
    }

    private boolean wasRaisedToday(Reminder reminder) {
        boolean raisedToday = false;
        ListIterator<ReminderEvent> i = reminderEvents.listIterator(reminderEvents.size());
        while (i.hasPrevious()) {
            ReminderEvent reminderEvent = i.previous();
            if (!isToday(reminderEvent)) {
                break;
            }
            if (reminderEvent.reminderId == reminder.reminderId) {
                raisedToday = true;
                break;
            }
        }
        return raisedToday;
    }

    private Medicine getMedicine(Reminder reminder) {
        int medicineId = reminder.medicineRelId;
        //noinspection OptionalGetWithoutIsPresent
        return this.medicineWithReminders.stream().filter(mwr -> mwr.medicine.medicineId == medicineId).findFirst().get().medicine;
    }

    private boolean isToday(ReminderEvent reminderEvent) {
        Instant i = Instant.ofEpochSecond(reminderEvent.raisedTimestamp);
        ZoneId zoneId = timeAccess.systemZone();
        LocalDate d = i.atZone(zoneId).toLocalDate();
        return timeAccess.localDate().isEqual(d);
    }

    public void updateReminderEvents(List<ReminderEvent> reminderEvents) {
        this.reminderEvents = reminderEvents;
        schedule();
    }

    public interface ScheduleListener {
        void schedule(Instant timestamp, Medicine medicine, Reminder reminder);
    }

    public interface TimeAccess {
        ZoneId systemZone();

        LocalDate localDate();
    }
}
