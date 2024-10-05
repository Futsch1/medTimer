package com.futsch1.medtimer;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_NOTIFICATION_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_EVENT_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_SNOOZE_TIME;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
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
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.reminders.SnoozeWork;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

        WorkerParameters workerParams = mock(WorkerParameters.class);
        Data inputData = new Data.Builder()
                .putInt(EXTRA_REMINDER_ID, reminderId)
                .putInt(EXTRA_REMINDER_EVENT_ID, reminderEventId)
                .putInt(EXTRA_SNOOZE_TIME, 15)
                .putInt(EXTRA_NOTIFICATION_ID, notificationId)
                .build();
        when(workerParams.getInputData()).thenReturn(inputData);
        Instant zero = Instant.ofEpochSecond(0);
        Instant snooze = Instant.ofEpochSecond(15 * 60);

        try (MockedStatic<PreferenceManager> mockedPreferencesManager = mockStatic(PreferenceManager.class);
             MockedStatic<Instant> mockedInstant = mockStatic(Instant.class)) {
            mockedPreferencesManager.when(() -> PreferenceManager.getDefaultSharedPreferences(mockApplication)).thenReturn(mockSharedPreferences);
            mockedInstant.when(Instant::now).thenReturn(zero);
            mockedInstant.when(() -> Instant.ofEpochSecond(15 * 60)).thenReturn(snooze);
            mockedInstant.when(() -> Instant.ofEpochSecond(15 * 60, 0)).thenReturn(snooze);
            SnoozeWork snoozeWork = new SnoozeWork(mockApplication, workerParams);

            // Expected to pass
            ListenableWorker.Result result = snoozeWork.doWork();
            assertInstanceOf(ListenableWorker.Result.Success.class, result);

            // Check if reminder event was updated with the generated notification ID
            verify(mockNotificationManager, times(1)).cancel(notificationId);
            ArgumentCaptor<PendingIntent> captor1 = ArgumentCaptor.forClass(PendingIntent.class);
            verify(mockAlarmManager, times(1)).cancel(captor1.capture());
            verify(mockAlarmManager, times(1)).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snooze.toEpochMilli(), captor1.getValue());
        }
    }
}
