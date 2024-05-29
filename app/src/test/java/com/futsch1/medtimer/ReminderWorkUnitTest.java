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

    private final int REMINDER_ID = 11;
    private final int REMINDER_EVENT_ID = 12;
    private final int MEDICINE_ID = 1;
    private final int NOTIFICATION_CHANNEL_ID = 13;
    private final int NOTIFICATION_ID = 14;
    private ReminderWork reminderWork;
    @Mock
    private Application mockApplication;
    private SharedPreferences mockSharedPreferences;
    private NotificationManager mockNotificationManager;

    @Before
    public void setUp() {
        WorkerParameters workerParams = mock(WorkerParameters.class);

        Data inputData = new Data.Builder().putInt(EXTRA_REMINDER_ID, REMINDER_ID).putInt(EXTRA_REMINDER_EVENT_ID, REMINDER_EVENT_ID).build();
        when(workerParams.getInputData()).thenReturn(inputData);

        mockApplication = mock(Application.class);

        reminderWork = new ReminderWork(mockApplication, workerParams);

        mockSharedPreferences = mock(SharedPreferences.class);
        when(mockSharedPreferences.getBoolean(anyString(), anyBoolean())).thenReturn(true);
        when(mockSharedPreferences.getString(anyString(), anyString())).thenReturn("1");
        when(mockSharedPreferences.getInt(eq("notificationChannelId"), anyInt())).thenReturn(NOTIFICATION_CHANNEL_ID);
        when(mockSharedPreferences.getInt(eq("notificationId"), anyInt())).thenReturn(NOTIFICATION_ID);
        SharedPreferences.Editor mockEditor = mock(SharedPreferences.Editor.class);
        when(mockSharedPreferences.edit()).thenReturn(mockEditor);
        when(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor);

        when(mockApplication.getSharedPreferences(anyString(), anyInt())).thenReturn(mockSharedPreferences);
        when(mockApplication.getString(R.string.notification_title)).thenReturn("NotificationTitle");
        when(mockApplication.getString(R.string.notification_taken)).thenReturn("NotificationTaken");
        when(mockApplication.getString(eq(R.string.notification_content), any(Object[].class))).thenReturn("NotificationContent");
        mockNotificationManager = mock(NotificationManager.class);
        when(mockApplication.getSystemService(NotificationManager.class)).thenReturn(mockNotificationManager);
    }

    @Test
    public void testDoWorkNullHandling() {
        // Reminder is null
        try (MockedConstruction<MedicineRepository> ignored = mockConstruction(MedicineRepository.class, (mock, context) -> when(mock.getReminder(11)).thenReturn(null));
             MockedStatic<WorkManagerAccess> mockedWorkManagerAccess = mockStatic(WorkManagerAccess.class)) {
            WorkManager mockWorkManager = mock(WorkManager.class);
            mockedWorkManagerAccess.when(() -> WorkManagerAccess.getWorkManager(mockApplication)).thenReturn(mockWorkManager);
            ListenableWorker.Result result = reminderWork.doWork();
            assertTrue(result instanceof ListenableWorker.Result.Failure);
        }
        // Medicine is null
        try (MockedConstruction<MedicineRepository> ignored = mockConstruction(MedicineRepository.class, (mock, context) -> {
            when(mock.getReminder(REMINDER_ID)).thenReturn(new Reminder(MEDICINE_ID));
            when(mock.getMedicine(MEDICINE_ID)).thenReturn(null);
        });
             MockedStatic<WorkManagerAccess> mockedWorkManagerAccess = mockStatic(WorkManagerAccess.class)) {
            WorkManager mockWorkManager = mock(WorkManager.class);
            mockedWorkManagerAccess.when(() -> WorkManagerAccess.getWorkManager(mockApplication)).thenReturn(mockWorkManager);
            ListenableWorker.Result result = reminderWork.doWork();
            assertTrue(result instanceof ListenableWorker.Result.Failure);
        }
        // Reminder event is null
        try (MockedConstruction<MedicineRepository> ignored = mockConstruction(MedicineRepository.class, (mock, context) -> {
            when(mock.getReminder(REMINDER_ID)).thenReturn(new Reminder(MEDICINE_ID));
            when(mock.getMedicine(MEDICINE_ID)).thenReturn(new Medicine("Test"));
            when(mock.getReminderEvent(REMINDER_EVENT_ID)).thenReturn(null);
        });
             MockedStatic<WorkManagerAccess> mockedWorkManagerAccess = mockStatic(WorkManagerAccess.class)) {
            WorkManager mockWorkManager = mock(WorkManager.class);
            mockedWorkManagerAccess.when(() -> WorkManagerAccess.getWorkManager(mockApplication)).thenReturn(mockWorkManager);
            ListenableWorker.Result result = reminderWork.doWork();
            assertTrue(result instanceof ListenableWorker.Result.Failure);
        }
    }

    @Test
    public void testDoWorkNotifications() {
        try (MockedConstruction<MedicineRepository> mockedMedicineRepositories = mockConstruction(MedicineRepository.class, (mock, context) -> {
            when(mock.getReminder(REMINDER_ID)).thenReturn(new Reminder(MEDICINE_ID));
            when(mock.getMedicine(MEDICINE_ID)).thenReturn(new Medicine("TestMedicine"));
            ReminderEvent reminderEvent = new ReminderEvent();
            reminderEvent.reminderId = REMINDER_ID;
            reminderEvent.reminderEventId = REMINDER_EVENT_ID;
            when(mock.getReminderEvent(REMINDER_EVENT_ID)).thenReturn(reminderEvent);
        });
             MockedConstruction<NotificationCompat.Builder> ignored2 = mockConstruction(NotificationCompat.Builder.class, (mock, context) -> {
                 // Implicitly verify arguments because invalid arguments will break the call chain of the builder
                 assertEquals(String.format("com.futsch1.medTimer.NOTIFICATION%d", NOTIFICATION_CHANNEL_ID), context.arguments().get(1));
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
            mockedWorkManagerAccess.when(() -> WorkManagerAccess.getWorkManager(mockApplication)).thenReturn(mockWorkManager);
            mockedPreferencesManager.when(() -> PreferenceManager.getDefaultSharedPreferences(mockApplication)).thenReturn(mockSharedPreferences);

            // Expected to pass
            ListenableWorker.Result result = reminderWork.doWork();
            assertTrue(result instanceof ListenableWorker.Result.Success);

            // Check if reminder event was updated with the generated notification ID
            MedicineRepository mockedMedicineRepository = mockedMedicineRepositories.constructed().get(0);
            ArgumentCaptor<ReminderEvent> captor = ArgumentCaptor.forClass(ReminderEvent.class);
            verify(mockedMedicineRepository, times(1)).updateReminderEvent(captor.capture());
            assertEquals(NOTIFICATION_ID, captor.getValue().notificationId);
            assertEquals(REMINDER_ID, captor.getValue().reminderId);
            assertEquals(REMINDER_EVENT_ID, captor.getValue().reminderEventId);
            verify(mockNotificationManager, times(1)).notify(eq(NOTIFICATION_ID), any());
        }
    }
}