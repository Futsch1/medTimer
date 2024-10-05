package com.futsch1.medtimer;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_DATE;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_EVENT_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
import android.text.format.DateFormat;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.robolectric.annotation.Config;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import tech.apter.junit.jupiter.robolectric.RobolectricExtension;

@ExtendWith(RobolectricExtension.class)
@Config(sdk = 34)
@SuppressWarnings("java:S5786") // Required for Robolectric extension
public class ReminderWorkUnitTest {

    private final int reminderId = 11;
    private final int reminderEventId = 12;
    private final int medicineId = 1;
    private final int notificationId = 14;
    private ReminderWork reminderWork;
    @Mock
    private Application mockApplication;
    private SharedPreferences mockSharedPreferences;
    private NotificationManager mockNotificationManager;
    private WorkerParameters workerParams;

    @BeforeEach
    public void setUp() {
        workerParams = mock(WorkerParameters.class);

        Data inputData = new Data.Builder().putInt(EXTRA_REMINDER_ID, reminderId).putInt(EXTRA_REMINDER_EVENT_ID, reminderEventId).
                putLong(EXTRA_REMINDER_DATE, 1).build();
        when(workerParams.getInputData()).thenReturn(inputData);

        mockApplication = mock(Application.class);

        reminderWork = new ReminderWork(mockApplication, workerParams);

        mockSharedPreferences = mock(SharedPreferences.class);
        when(mockSharedPreferences.getBoolean(anyString(), anyBoolean())).thenReturn(true);
        when(mockSharedPreferences.getString(anyString(), anyString())).thenReturn("1");
        final int notificationChannelId = 13;
        when(mockSharedPreferences.getInt(eq("notificationChannelId"), anyInt())).thenReturn(notificationChannelId);
        when(mockSharedPreferences.getInt(eq("notificationId"), anyInt())).thenReturn(notificationId);
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
            assertInstanceOf(ListenableWorker.Result.Failure.class, result);
        }
        // Medicine is null
        try (MockedConstruction<MedicineRepository> ignored = mockConstruction(MedicineRepository.class, (mock, context) -> {
            when(mock.getReminder(reminderId)).thenReturn(new Reminder(medicineId));
            when(mock.getMedicine(medicineId)).thenReturn(null);
        });
             MockedStatic<WorkManagerAccess> mockedWorkManagerAccess = mockStatic(WorkManagerAccess.class)) {
            WorkManager mockWorkManager = mock(WorkManager.class);
            mockedWorkManagerAccess.when(() -> WorkManagerAccess.getWorkManager(mockApplication)).thenReturn(mockWorkManager);
            ListenableWorker.Result result = reminderWork.doWork();
            assertInstanceOf(ListenableWorker.Result.Failure.class, result);
        }
        // Reminder event is null
        try (MockedConstruction<MedicineRepository> ignored = mockConstruction(MedicineRepository.class, (mock, context) -> {
            when(mock.getReminder(reminderId)).thenReturn(new Reminder(medicineId));
            when(mock.getMedicine(medicineId)).thenReturn(new Medicine("Test"));
            when(mock.getReminderEvent(reminderEventId)).thenReturn(null);
        });
             MockedStatic<WorkManagerAccess> mockedWorkManagerAccess = mockStatic(WorkManagerAccess.class)) {
            WorkManager mockWorkManager = mock(WorkManager.class);
            mockedWorkManagerAccess.when(() -> WorkManagerAccess.getWorkManager(mockApplication)).thenReturn(mockWorkManager);
            ListenableWorker.Result result = reminderWork.doWork();
            assertInstanceOf(ListenableWorker.Result.Failure.class, result);
        }
    }

    @Test
    public void testDoWorkNotifications() {
        try (MockedConstruction<MedicineRepository> mockedMedicineRepositories = mockConstruction(MedicineRepository.class, (mock, context) -> {
            when(mock.getReminder(reminderId)).thenReturn(new Reminder(medicineId));
            when(mock.getMedicine(medicineId)).thenReturn(new Medicine("TestMedicine"));
            ReminderEvent reminderEvent = new ReminderEvent();
            reminderEvent.reminderId = reminderId;
            reminderEvent.reminderEventId = reminderEventId;
            when(mock.getReminderEvent(reminderEventId)).thenReturn(reminderEvent);
        });
             MockedConstruction<NotificationCompat.Builder> ignored2 = mockConstruction(NotificationCompat.Builder.class, (mock, context) -> {
                 // Implicitly verify arguments because invalid arguments will break the call chain of the builder
                 assertEquals(String.format("%d", 3), context.arguments().get(1));
                 when(mock.setSmallIcon(R.drawable.capsule)).thenReturn(mock);
                 when(mock.setContentTitle("NotificationTitle")).thenReturn(mock);
                 when(mock.setContentText("NotificationContent")).thenReturn(mock); // Should not be necessary?
                 when(mock.setPriority(NotificationCompat.PRIORITY_DEFAULT)).thenReturn(mock);
                 when(mock.setContentIntent(any())).thenReturn(mock);
                 when(mock.addAction(eq(R.drawable.check2_circle), eq("NotificationTaken"), any())).thenReturn(mock());

             });
             MockedStatic<WorkManagerAccess> mockedWorkManagerAccess = mockStatic(WorkManagerAccess.class);
             MockedStatic<PreferenceManager> mockedPreferencesManager = mockStatic(PreferenceManager.class);
             MockedStatic<DateFormat> dateAccessMockedStatic = mockStatic(DateFormat.class)) {
            WorkManager mockWorkManager = mock(WorkManager.class);
            mockedWorkManagerAccess.when(() -> WorkManagerAccess.getWorkManager(mockApplication)).thenReturn(mockWorkManager);
            mockedPreferencesManager.when(() -> PreferenceManager.getDefaultSharedPreferences(mockApplication)).thenReturn(mockSharedPreferences);
            dateAccessMockedStatic.when(() -> DateFormat.getTimeFormat(any())).thenReturn(java.text.DateFormat.getTimeInstance());

            // Expected to pass
            ListenableWorker.Result result = reminderWork.doWork();
            assertInstanceOf(ListenableWorker.Result.Success.class, result);

            // Check if reminder event was updated with the generated notification ID
            MedicineRepository mockedMedicineRepository = mockedMedicineRepositories.constructed().get(0);
            ArgumentCaptor<ReminderEvent> captor = ArgumentCaptor.forClass(ReminderEvent.class);
            verify(mockedMedicineRepository, times(1)).updateReminderEvent(captor.capture());
            assertEquals(notificationId, captor.getValue().notificationId);
            assertEquals(reminderId, captor.getValue().reminderId);
            assertEquals(reminderEventId, captor.getValue().reminderEventId);
            verify(mockNotificationManager, times(1)).notify(eq(notificationId), any());
        }
    }

    @Test
    public void testDoWorkNewReminder() {
        try (MockedConstruction<MedicineRepository> mockedMedicineRepositories = mockConstruction(MedicineRepository.class, (mock, context) -> {
            Reminder reminder = new Reminder(medicineId);
            reminder.reminderId = reminderId;
            when(mock.getReminder(reminderId)).thenReturn(reminder);
            when(mock.getMedicine(medicineId)).thenReturn(new Medicine("TestMedicine"));
            when(mock.insertReminderEvent(any())).thenReturn((long) reminderEventId);
        });
             MockedConstruction<NotificationCompat.Builder> ignored2 = mockConstruction(NotificationCompat.Builder.class, (mock, context) -> {
                 // Implicitly verify arguments because invalid arguments will break the call chain of the builder
                 assertEquals(String.format("%d", 3), context.arguments().get(1));
                 when(mock.setSmallIcon(R.drawable.capsule)).thenReturn(mock);
                 when(mock.setContentTitle("NotificationTitle")).thenReturn(mock);
                 when(mock.setContentText("NotificationContent")).thenReturn(mock); // Should not be necessary?
                 when(mock.setPriority(NotificationCompat.PRIORITY_DEFAULT)).thenReturn(mock);
                 when(mock.setContentIntent(any())).thenReturn(mock);
                 when(mock.addAction(eq(R.drawable.check2_circle), eq("NotificationTaken"), any())).thenReturn(mock());

             });
             MockedStatic<WorkManagerAccess> mockedWorkManagerAccess = mockStatic(WorkManagerAccess.class);
             MockedStatic<PreferenceManager> mockedPreferencesManager = mockStatic(PreferenceManager.class);
             MockedStatic<DateFormat> dateAccessMockedStatic = mockStatic(DateFormat.class)) {
            WorkManager mockWorkManager = mock(WorkManager.class);
            mockedWorkManagerAccess.when(() -> WorkManagerAccess.getWorkManager(mockApplication)).thenReturn(mockWorkManager);
            mockedPreferencesManager.when(() -> PreferenceManager.getDefaultSharedPreferences(mockApplication)).thenReturn(mockSharedPreferences);
            dateAccessMockedStatic.when(() -> DateFormat.getTimeFormat(any())).thenReturn(java.text.DateFormat.getTimeInstance());

            Data inputData = new Data.Builder().putInt(EXTRA_REMINDER_ID, reminderId).putInt(EXTRA_REMINDER_EVENT_ID, 0).
                    putLong(EXTRA_REMINDER_DATE, 1).build();
            when(workerParams.getInputData()).thenReturn(inputData);

            // Expected to pass
            ListenableWorker.Result result = reminderWork.doWork();
            assertInstanceOf(ListenableWorker.Result.Success.class, result);

            // Check if reminder event was updated with the generated notification ID
            MedicineRepository mockedMedicineRepository = mockedMedicineRepositories.constructed().get(0);
            ArgumentCaptor<ReminderEvent> captor = ArgumentCaptor.forClass(ReminderEvent.class);
            verify(mockedMedicineRepository, times(1)).updateReminderEvent(captor.capture());
            assertEquals(notificationId, captor.getValue().notificationId);
            assertEquals(reminderId, captor.getValue().reminderId);
            assertEquals(reminderEventId, captor.getValue().reminderEventId);
            assertEquals(LocalDateTime.of(LocalDate.ofEpochDay(1), LocalTime.of(Reminder.DEFAULT_TIME / 60, Reminder.DEFAULT_TIME % 60))
                    .toEpochSecond(ZoneId.systemDefault().getRules().getOffset(Instant.now())), captor.getValue().remindedTimestamp);
            verify(mockNotificationManager, times(1)).notify(eq(notificationId), any());
            verify(mockedMedicineRepository, times(1)).updateReminderEvent(any());
        }
    }

}