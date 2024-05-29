package com.futsch1.medtimer;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_EVENT_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.app.NotificationManager;
import android.content.SharedPreferences;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;

import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.reminders.ReminderWork;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ReminderWorkUnitTest {

    private ReminderWork reminderWork;

    @Mock
    private Application application;
    private SharedPreferences mockSharedPreferences;

    @Before
    public void setUp() {
        WorkerParameters workerParams = mock(WorkerParameters.class);

        Data inputData = new Data.Builder().putInt(EXTRA_REMINDER_ID, 11).putInt(EXTRA_REMINDER_EVENT_ID, 12).build();
        when(workerParams.getInputData()).thenReturn(inputData);

        application = mock(Application.class);

        reminderWork = new ReminderWork(application, workerParams);

        mockSharedPreferences = mock(SharedPreferences.class);
        when(mockSharedPreferences.getBoolean(anyString(), anyBoolean())).thenReturn(true);
        when(mockSharedPreferences.getString(anyString(), anyString())).thenReturn("1");
        when(mockSharedPreferences.getInt(eq("notificationChannelId"), anyInt())).thenReturn(13);
        when(mockSharedPreferences.getInt(eq("notificationId"), anyInt())).thenReturn(14);
        SharedPreferences.Editor mockEditor = mock(SharedPreferences.Editor.class);
        when(mockSharedPreferences.edit()).thenReturn(mockEditor);
        when(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor);

        when(application.getSharedPreferences(anyString(), anyInt())).thenReturn(mockSharedPreferences);
        when(application.getString(R.string.notification_title)).thenReturn("NotificationTitle");
        when(application.getString(R.string.notification_taken)).thenReturn("NotificationTaken");
        when(application.getString(eq(R.string.notification_content), any(Object[].class))).thenReturn("NotificationContent");
        when(application.getSystemService(NotificationManager.class)).thenReturn(mock(NotificationManager.class));
    }

    @Test
    public void testDoWorkNullHandling() {
        // Reminder is null
        try (MockedConstruction<MedicineRepository> ignored = mockConstruction(MedicineRepository.class, (mock, context) -> when(mock.getReminder(11)).thenReturn(null));
             MockedStatic<WorkManagerAccess> mockedWorkManagerAccess = mockStatic(WorkManagerAccess.class)) {
            WorkManager mockWorkManager = mock(WorkManager.class);
            mockedWorkManagerAccess.when(() -> WorkManagerAccess.getWorkManager(application)).thenReturn(mockWorkManager);
            ListenableWorker.Result result = reminderWork.doWork();
            assertTrue(result instanceof ListenableWorker.Result.Failure);
        }
        // Medicine is null
        try (MockedConstruction<MedicineRepository> ignored = mockConstruction(MedicineRepository.class, (mock, context) -> {
            when(mock.getReminder(11)).thenReturn(new Reminder(1));
            when(mock.getMedicine(1)).thenReturn(null);
        });
             MockedStatic<WorkManagerAccess> mockedWorkManagerAccess = mockStatic(WorkManagerAccess.class)) {
            WorkManager mockWorkManager = mock(WorkManager.class);
            mockedWorkManagerAccess.when(() -> WorkManagerAccess.getWorkManager(application)).thenReturn(mockWorkManager);
            ListenableWorker.Result result = reminderWork.doWork();
            assertTrue(result instanceof ListenableWorker.Result.Failure);
        }
        // Reminder event is null
        try (MockedConstruction<MedicineRepository> ignored = mockConstruction(MedicineRepository.class, (mock, context) -> {
            when(mock.getReminder(11)).thenReturn(new Reminder(1));
            when(mock.getMedicine(1)).thenReturn(new Medicine("Test"));
            when(mock.getReminderEvent(12)).thenReturn(null);
        });
             MockedStatic<WorkManagerAccess> mockedWorkManagerAccess = mockStatic(WorkManagerAccess.class)) {
            WorkManager mockWorkManager = mock(WorkManager.class);
            mockedWorkManagerAccess.when(() -> WorkManagerAccess.getWorkManager(application)).thenReturn(mockWorkManager);
            ListenableWorker.Result result = reminderWork.doWork();
            assertTrue(result instanceof ListenableWorker.Result.Failure);
        }
    }

    @Test
    public void testDoWorkNotifications() {
        try (MockedConstruction<MedicineRepository> mockedMedicineRepositories = mockConstruction(MedicineRepository.class, (mock, context) -> {
            when(mock.getReminder(11)).thenReturn(new Reminder(1));
            when(mock.getMedicine(1)).thenReturn(new Medicine("TestMedicine"));
            ReminderEvent reminderEvent = new ReminderEvent();
            reminderEvent.reminderId = 11;
            reminderEvent.reminderEventId = 12;
            when(mock.getReminderEvent(12)).thenReturn(reminderEvent);
        });
             MockedConstruction<NotificationCompat.Builder> ignored2 = mockConstruction(NotificationCompat.Builder.class, (mock, context) -> {
                 // Implicitly verify arguments because invalid arguments will break the call chain of the builder
                 assertEquals("com.futsch1.medTimer.NOTIFICATION13", context.arguments().get(1));
                 when(mock.setSmallIcon(R.drawable.capsule)).thenReturn(mock);
                 when(mock.setContentTitle("NotificationTitle")).thenReturn(mock);
                 when(mock.setContentText("NotificationContent")).thenReturn(mock); // Should not be necessary?
                 when(mock.setPriority(NotificationCompat.PRIORITY_DEFAULT)).thenReturn(mock);
                 when(mock.setContentIntent(any())).thenReturn(mock);
                 when(mock.addAction(eq(R.drawable.check2_circle), eq("NotificationTaken"), any())).thenReturn(mock());

             });
             MockedStatic<WorkManagerAccess> mockedWorkManagerAccess = mockStatic(WorkManagerAccess.class);
             MockedStatic<PreferenceManager> mockedPreferencesManager = mockStatic(PreferenceManager.class)) {
            WorkManager mockWorkManager = mock(WorkManager.class);
            mockedWorkManagerAccess.when(() -> WorkManagerAccess.getWorkManager(application)).thenReturn(mockWorkManager);
            mockedPreferencesManager.when(() -> PreferenceManager.getDefaultSharedPreferences(application)).thenReturn(mockSharedPreferences);

            ListenableWorker.Result result = reminderWork.doWork();
            assertTrue(result instanceof ListenableWorker.Result.Success);

            // Check if reminder event was updated with the generated notification ID
            MedicineRepository mockedMedicineRepository = mockedMedicineRepositories.constructed().get(0);
            ArgumentCaptor<ReminderEvent> captor = ArgumentCaptor.forClass(ReminderEvent.class);
            verify(mockedMedicineRepository, times(1)).updateReminderEvent(captor.capture());
            assertEquals(14, captor.getValue().notificationId);
            assertEquals(11, captor.getValue().reminderId);
            assertEquals(12, captor.getValue().reminderEventId);
        }
    }
}