package com.futsch1.medtimer;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_REPEAT_TIME_SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.reminders.RepeatReminderWork;
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.robolectric.annotation.Config;

import java.time.Instant;

import tech.apter.junit.jupiter.robolectric.RobolectricExtension;

@ExtendWith(RobolectricExtension.class)
@Config(sdk = 34)
@SuppressWarnings("java:S5786") // Required for Robolectric extension
public class RepeatReminderWorkUnitTest {

    @Mock
    private Application mockApplication;
    private AlarmManager mockAlarmManager;
    private SharedPreferences mockSharedPreferences;

    @BeforeEach
    public void setUp() {

        mockApplication = mock(Application.class);
        when(mockApplication.getPackageName()).thenReturn("test");

        mockSharedPreferences = mock(SharedPreferences.class);
        when(mockSharedPreferences.getBoolean(anyString(), anyBoolean())).thenReturn(false);

        mockAlarmManager = mock(AlarmManager.class);
        when(mockApplication.getSystemService(AlarmManager.class)).thenReturn(mockAlarmManager);
    }

    @Test
    public void testDoWorkRepeatReminder() {
        ReminderEvent reminderEvent = new ReminderEvent();
        reminderEvent.notificationId = 14;
        int reminderId = 11;
        reminderEvent.reminderId = reminderId;
        int reminderEventId = 12;
        reminderEvent.reminderEventId = reminderEventId;
        reminderEvent.status = ReminderEvent.ReminderStatus.RAISED;
        reminderEvent.processedTimestamp = Instant.now().getEpochSecond();

        int remainingRepeats = 4;
        ReminderNotificationData data = ReminderNotificationData.Companion.fromArrays(new int[]{reminderId}, new int[]{reminderEventId}, Instant.now());
        WorkerParameters workerParams = mock(WorkerParameters.class);
        Data.Builder builder = new Data.Builder();
        data.toBuilder(builder);
        builder.putInt(EXTRA_REPEAT_TIME_SECONDS, 15);
        when(workerParams.getInputData()).thenReturn(builder.build());
        Instant zero = Instant.ofEpochSecond(0);
        Instant repeat = Instant.ofEpochSecond(15);

        try (MockedStatic<PreferenceManager> mockedPreferencesManager = mockStatic(PreferenceManager.class);
             MockedStatic<Instant> mockedInstant = mockStatic(Instant.class);
             MockedConstruction<MedicineRepository> mockedMedicineRepositories = mockConstruction(MedicineRepository.class, (mock, context) -> when(mock.getReminderEvent(reminderEventId)).thenReturn(reminderEvent)
             )) {
            mockedPreferencesManager.when(() -> PreferenceManager.getDefaultSharedPreferences(mockApplication)).thenReturn(mockSharedPreferences);
            mockedInstant.when(Instant::now).thenReturn(zero);
            when(zero.plusSeconds(15)).thenReturn(repeat);
            RepeatReminderWork repeatReminderWork = new RepeatReminderWork(mockApplication, workerParams);

            // Expected to pass
            ListenableWorker.Result result = repeatReminderWork.doWork();
            assertInstanceOf(ListenableWorker.Result.Success.class, result);

            verify(mockAlarmManager, times(2)).cancel((PendingIntent) any());
            verify(mockAlarmManager, times(1)).setAndAllowWhileIdle(eq(AlarmManager.RTC_WAKEUP), eq(repeat.toEpochMilli()), any());

            // Check if reminder event was updated with the one lower remaining repeats
            MedicineRepository mockedMedicineRepository = mockedMedicineRepositories.constructed().get(0);
            ArgumentCaptor<ReminderEvent> captor = ArgumentCaptor.forClass(ReminderEvent.class);
            verify(mockedMedicineRepository, times(1)).updateReminderEvent(captor.capture());
            assertEquals(remainingRepeats - 1, captor.getValue().remainingRepeats);
        }
    }
}
