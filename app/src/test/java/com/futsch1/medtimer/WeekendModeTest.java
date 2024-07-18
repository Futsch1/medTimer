package com.futsch1.medtimer;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.SharedPreferences;
import android.util.ArraySet;

import com.futsch1.medtimer.reminders.WeekendMode;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class WeekendModeTest {
    @Test
    public void test_adjustInstant_ReturnsSameInstant_WeekendModeDisabled() {
        SharedPreferences preferences = Mockito.mock(SharedPreferences.class);
        WeekendMode weekendMode = new WeekendMode(preferences);
        Instant instant = Instant.now();

        Mockito.when(preferences.getBoolean(eq(WeekendModePreferencesFragment.WEEKEND_MODE), anyBoolean())).thenReturn(false);

        Instant adjustedInstant = weekendMode.adjustInstant(instant);

        assertEquals(instant, adjustedInstant);
    }

    @Test
    public void test_adjustInstant_ReturnsSameInstant_WeekendDaysEmpty() {
        SharedPreferences preferences = Mockito.mock(SharedPreferences.class);
        WeekendMode weekendMode = new WeekendMode(preferences);
        Instant instant = Instant.now();

        Mockito.when(preferences.getBoolean(WeekendModePreferencesFragment.WEEKEND_MODE, false)).thenReturn(true);
        Mockito.when(preferences.getStringSet(WeekendModePreferencesFragment.WEEKEND_DAYS, new ArraySet<>())).thenReturn(new HashSet<>());

        Instant adjustedInstant = weekendMode.adjustInstant(instant);

        assertEquals(instant, adjustedInstant);
    }

    @Test
    public void test_adjustInstant_ReturnsSameInstant_CurrentDayIsNotWeekend() {
        SharedPreferences preferences = Mockito.mock(SharedPreferences.class);
        Instant instant = Mockito.mock(Instant.class);
        WeekendMode weekendMode = new WeekendMode(preferences);
        ZonedDateTime testZonedDateTime = ZonedDateTime.of(2024, 4, 6, 8, 0, 0, 0, ZoneId.of("Z"));

        Mockito.when(preferences.getBoolean(eq(WeekendModePreferencesFragment.WEEKEND_MODE), anyBoolean())).thenReturn(true);
        Mockito.when(preferences.getInt(eq(WeekendModePreferencesFragment.WEEKEND_TIME), anyInt())).thenReturn(10 * 60);
        Mockito.when(instant.atZone(any())).thenReturn(testZonedDateTime);
        Set<String> weekendDays = new HashSet<>();
        weekendDays.add(String.valueOf(DayOfWeek.TUESDAY.getValue()));
        weekendDays.add(String.valueOf(DayOfWeek.WEDNESDAY.getValue()));
        Mockito.when(preferences.getStringSet(WeekendModePreferencesFragment.WEEKEND_DAYS, new ArraySet<>())).thenReturn(weekendDays);

        Instant adjustedInstant = weekendMode.adjustInstant(instant);

        assertEquals(instant, adjustedInstant);
    }

    @Test
    public void test_adjustInstant_ReturnsAdjustedInstant_WeekendModeEnabled_CurrentDayIsWeekend() {
        SharedPreferences preferences = Mockito.mock(SharedPreferences.class);
        Instant instant = Mockito.mock(Instant.class);
        WeekendMode weekendMode = new WeekendMode(preferences);
        ZonedDateTime testZonedDateTime = ZonedDateTime.of(2024, 4, 6, 8, 0, 0, 0, ZoneId.of("Z"));

        Mockito.when(preferences.getBoolean(eq(WeekendModePreferencesFragment.WEEKEND_MODE), anyBoolean())).thenReturn(true);
        Mockito.when(preferences.getInt(eq(WeekendModePreferencesFragment.WEEKEND_TIME), anyInt())).thenReturn(10 * 60);
        Mockito.when(instant.atZone(any())).thenReturn(testZonedDateTime);
        Set<String> weekendDays = new HashSet<>();
        weekendDays.add(String.valueOf(DayOfWeek.SATURDAY.getValue()));
        weekendDays.add(String.valueOf(DayOfWeek.SUNDAY.getValue()));
        Mockito.when(preferences.getStringSet(WeekendModePreferencesFragment.WEEKEND_DAYS, new ArraySet<>())).thenReturn(weekendDays);

        weekendMode.adjustInstant(instant);

        verify(instant, times(1)).plusSeconds(2 * 60 * 60);
    }

    @Test
    public void test_adjustInstant_ReturnsAdjustedInstant_WeekendModeEnabled_CurrentDayIsWeekendReminderIsLater() {
        SharedPreferences preferences = Mockito.mock(SharedPreferences.class);
        Instant instant = Mockito.mock(Instant.class);
        WeekendMode weekendMode = new WeekendMode(preferences);
        ZonedDateTime testZonedDateTime = ZonedDateTime.of(2024, 4, 6, 11, 0, 0, 0, ZoneId.of("Z"));

        Mockito.when(preferences.getBoolean(eq(WeekendModePreferencesFragment.WEEKEND_MODE), anyBoolean())).thenReturn(true);
        Mockito.when(preferences.getInt(eq(WeekendModePreferencesFragment.WEEKEND_TIME), anyInt())).thenReturn(10 * 60);
        Mockito.when(instant.atZone(any())).thenReturn(testZonedDateTime);
        Set<String> weekendDays = new HashSet<>();
        weekendDays.add(String.valueOf(DayOfWeek.SATURDAY.getValue()));
        weekendDays.add(String.valueOf(DayOfWeek.SUNDAY.getValue()));
        Mockito.when(preferences.getStringSet(WeekendModePreferencesFragment.WEEKEND_DAYS, new ArraySet<>())).thenReturn(weekendDays);

        weekendMode.adjustInstant(instant);

        verify(instant, times(0)).plusSeconds(anyLong());
    }
}

