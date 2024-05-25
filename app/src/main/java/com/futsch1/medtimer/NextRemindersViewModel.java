package com.futsch1.medtimer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class NextRemindersViewModel extends ViewModel {

    private final MutableLiveData<List<ScheduledReminder>> scheduledReminders = new MutableLiveData<>();

    public LiveData<List<ScheduledReminder>> getScheduledReminders() {
        return scheduledReminders;
    }

    public void setScheduledReminders(List<ScheduledReminder> scheduledReminders) {
        this.scheduledReminders.setValue(scheduledReminders);
    }

}
