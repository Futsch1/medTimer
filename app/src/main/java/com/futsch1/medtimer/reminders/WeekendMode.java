package com.futsch1.medtimer.reminders;

import android.content.SharedPreferences;
import android.util.ArraySet;

import com.futsch1.medtimer.PreferencesNames;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

public class WeekendMode {

    private final SharedPreferences preferences;

    public WeekendMode(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public Instant adjustInstant(Instant instant) {
        if (isWeekendModeEnabled()) {
            int weekendTime = preferences.getInt(PreferencesNames.WEEKEND_TIME, 540);
            Set<String> weekendDays = preferences.getStringSet(PreferencesNames.WEEKEND_DAYS, new ArraySet<>());
            ZonedDateTime localDateTime = instant.atZone(ZoneId.systemDefault());
            DayOfWeek dayOfWeek = localDateTime.getDayOfWeek();
            int minutes = localDateTime.getMinute() + localDateTime.getHour() * 60;
            int deltaMinutes = weekendTime - minutes;
            if (weekendDays.contains(String.valueOf(dayOfWeek.getValue())) && deltaMinutes > 0) {
                instant = instant.plusSeconds(deltaMinutes * 60L);
            }
        }
        return instant;
    }

    private boolean isWeekendModeEnabled() {
        return preferences.getBoolean(PreferencesNames.WEEKEND_MODE, false);
    }
}
