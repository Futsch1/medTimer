package com.futsch1.medtimer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AlarmManager;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.reminders.SnoozeProcessor;
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.robolectric.annotation.Config;

import java.time.Instant;

import tech.apter.junit.jupiter.robolectric.RobolectricExtension;

@ExtendWith(RobolectricExtension.class)
@Config(sdk = 34)
@SuppressWarnings("java:S5786") // Required for Robolectric extension
public class SnoozeWorkUnitTest {

    @Mock
    private Application mockApplication;
    private NotificationManager mockNotificationManager;
    private AlarmManager mockAlarmManager;
    private SharedPreferences mockSharedPreferences;

    @BeforeEach
    public void setUp() {

        mockApplication = mock(Application.class);
        when(mockApplication.getPackageName()).thenReturn("test");
        when(mockApplication.getApplicationContext()).thenReturn(mockApplication);

        mockNotificationManager = mock(NotificationManager.class);
        when(mockApplication.getSystemService(NotificationManager.class)).thenReturn(mockNotificationManager);

        mockSharedPreferences = mock(SharedPreferences.class);
        when(mockSharedPreferences.getBoolean(anyString(), anyBoolean())).thenReturn(false);

        mockAlarmManager = mock(AlarmManager.class);
        when(mockApplication.getSystemService(AlarmManager.class)).thenReturn(mockAlarmManager);
    }

    @Test
    public void testDoWorkSnooze() {
        ReminderEvent reminderEvent = new ReminderEvent();
        int notificationId = 14;
        reminderEvent.notificationId = notificationId;
        int reminderId = 11;
        reminderEvent.reminderId = reminderId;
        int reminderEventId = 12;
        reminderEvent.reminderEventId = reminderEventId;
        reminderEvent.status = ReminderEvent.ReminderStatus.RAISED;
        reminderEvent.processedTimestamp = Instant.now().getEpochSecond();
        Instant zero = Instant.ofEpochSecond(0);

        ReminderNotificationData data = ReminderNotificationData.Companion.fromArrays(new int[]{reminderId}, new int[]{reminderEventId}, zero, notificationId);
        Instant snooze = Instant.ofEpochSecond(15L * 60);

        try (MockedStatic<PreferenceManager> mockedPreferencesManager = mockStatic(PreferenceManager.class);
             MockedStatic<Instant> mockedInstant = mockStatic(Instant.class)) {
            mockedPreferencesManager.when(() -> PreferenceManager.getDefaultSharedPreferences(mockApplication)).thenReturn(mockSharedPreferences);
            mockedInstant.when(Instant::now).thenReturn(zero);
            mockedInstant.when(() -> Instant.ofEpochSecond(0)).thenReturn(zero);
            mockedInstant.when(() -> Instant.ofEpochSecond(15 * 60)).thenReturn(snooze);
            mockedInstant.when(() -> Instant.ofEpochSecond(15 * 60, 0)).thenReturn(snooze);
            new SnoozeProcessor(mockApplication).processSnooze(data, 15 * 60);

            // Check if reminder event was updated with the generated notification ID
            verify(mockNotificationManager, times(1)).cancel(notificationId);
            verify(mockAlarmManager, times(3)).cancel((PendingIntent) any());
            verify(mockAlarmManager, times(1)).setAndAllowWhileIdle(eq(AlarmManager.RTC_WAKEUP), eq(snooze.toEpochMilli()), any());
        }
    }
}
