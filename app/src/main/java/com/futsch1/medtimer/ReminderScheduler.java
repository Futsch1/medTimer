package com.futsch1.medtimer;

import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class ReminderScheduler {
    private final ScheduleListener listener;
    private final GetInstant getInstant;
    private List<MedicineWithReminders> medicineWithReminders;
    private List<ReminderEvent> reminderEvents;

    public ReminderScheduler(ScheduleListener listener, GetInstant getInstant) {
        this.listener = listener;
        this.getInstant = getInstant;
    }

    public void updateMedicine(List<MedicineWithReminders> medicineWithReminders) {
        this.medicineWithReminders = medicineWithReminders;
        schedule();
    }

    private void schedule() {
        if (this.reminderEvents != null && this.medicineWithReminders != null && this.medicineWithReminders.size() > 0) {
            ArrayList<Reminder> sortedReminders = getSortedReminders();

            if (sortedReminders.size() > 0) {

                // Now start at the last reminderEvent
                Start start = getStart(sortedReminders);
                int reminderIndex = start.reminderIndex;
                LocalDate checkDate = start.startDate;
                Instant now = this.getInstant.now();
                boolean inThePast = checkDate.isBefore(toLocalDate(now));

                while (true) {
                    for (; reminderIndex < sortedReminders.size(); reminderIndex++) {
                        Reminder nextReminder = sortedReminders.get(reminderIndex);
                        this.listener.schedule(toInstant(checkDate).plusSeconds(nextReminder.timeInMinutes * 60L), getMedicine(nextReminder), nextReminder);
                        if (!inThePast) {
                            return;
                        }
                    }

                    reminderIndex = 0;
                    checkDate = checkDate.plusDays(1);
                    inThePast = checkDate.isBefore(toLocalDate(now));
                }
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

    private Start getStart(List<Reminder> reminderList) {
        if (this.reminderEvents.size() > 0) {
            ReminderEvent lastEvent = this.reminderEvents.get(this.reminderEvents.size() - 1);
            int lastReminderId = lastEvent.reminderId;

            Reminder reminder = reminderList.stream().filter(r -> r.reminderId == lastReminderId).findFirst().orElse(null);

            return new Start(reminderList.indexOf(reminder), LocalDate.from(Instant.ofEpochSecond(lastEvent.raisedTimestamp)));
        } else {
            return new Start(0, toLocalDate(this.getInstant.now()));
        }
    }

    private LocalDate toLocalDate(Instant i) {
        return i.atZone(ZoneOffset.UTC).toLocalDate();
    }

    private Instant toInstant(LocalDate d) {
        return d.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private Medicine getMedicine(Reminder reminder) {
        int medicineId = reminder.medicineRelId;
        //noinspection OptionalGetWithoutIsPresent
        return this.medicineWithReminders.stream().filter(mwr -> mwr.medicine.medicineId == medicineId).findFirst().get().medicine;
    }

    public void updateReminderEvents(List<ReminderEvent> reminderEvents) {
        this.reminderEvents = reminderEvents;
        schedule();
    }

    public interface ScheduleListener {
        void schedule(Instant timestamp, Medicine medicine, Reminder reminder);
    }

    public interface GetInstant {
        Instant now();
    }

    private static class Start {
        public final int reminderIndex;
        public final LocalDate startDate;

        public Start(int reminderIndex, LocalDate startDate) {

            this.reminderIndex = reminderIndex;
            this.startDate = startDate;
        }
    }
}
