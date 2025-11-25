package com.futsch1.medtimer;

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
import android.app.Notification;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.text.format.DateFormat;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;

import com.futsch1.medtimer.database.FullMedicine;
import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.helpers.MedicineIcons;
import com.futsch1.medtimer.reminders.NotificationSoundManager;
import com.futsch1.medtimer.reminders.ReminderWork;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;

import tech.apter.junit.jupiter.robolectric.RobolectricExtension;

@ExtendWith(RobolectricExtension.class)
@Config(sdk = 34)
@SuppressWarnings("java:S5786") // Required for Robolectric extension
public class ReminderWorkUnitTest {

    public static final String NOTIFICATION_TITLE = "NotificationTitle";
    public static final String NOTIFICATION_TAKEN = "NotificationTaken";
    private static final int REMINDER_ID = 11;
    private static final int REMINDER_EVENT_ID = 12;
    private static final int MEDICINE_ID = 1;
    private static final int NOTIFICATION_ID = 14;
    private ReminderWork reminderWork;
    @Mock
    private Application mockApplication;
    private SharedPreferences mockSharedPreferences;
    private NotificationManager mockNotificationManager;
    private WorkerParameters workerParams;

    @BeforeEach
    public void setUp() {
        workerParams = mock(WorkerParameters.class);

        ReminderNotificationData data = ReminderNotificationData.Companion.fromArrays(null, new int[]{REMINDER_ID}, new int[]{REMINDER_EVENT_ID}, Instant.now());
        Data.Builder inputData = new Data.Builder();
        data.toBuilder(inputData);
        when(workerParams.getInputData()).thenReturn(inputData.build());

        mockApplication = mock(Application.class);

        reminderWork = new ReminderWork(mockApplication, workerParams);

        mockSharedPreferences = mock(SharedPreferences.class);
        when(mockSharedPreferences.getBoolean(anyString(), anyBoolean())).thenReturn(true);
        when(mockSharedPreferences.getString(anyString(), anyString())).thenReturn("1");
        final int notificationChannelId = 13;
        when(mockSharedPreferences.getInt(eq("notificationChannelId"), anyInt())).thenReturn(notificationChannelId);
        when(mockSharedPreferences.getInt(eq("notificationId"), anyInt())).thenReturn(NOTIFICATION_ID);
        SharedPreferences.Editor mockEditor = mock(SharedPreferences.Editor.class);
        when(mockSharedPreferences.edit()).thenReturn(mockEditor);
        when(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor);

        when(mockApplication.getSharedPreferences(anyString(), anyInt())).thenReturn(mockSharedPreferences);
        when(mockApplication.getString(R.string.notification_title)).thenReturn(NOTIFICATION_TITLE);
        when(mockApplication.getString(R.string.taken)).thenReturn(NOTIFICATION_TAKEN);
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
            when(mock.getReminder(REMINDER_ID)).thenReturn(new Reminder(MEDICINE_ID));
            when(mock.getOnlyMedicine(MEDICINE_ID)).thenReturn(null);
        });
             MockedStatic<WorkManagerAccess> mockedWorkManagerAccess = mockStatic(WorkManagerAccess.class)) {
            WorkManager mockWorkManager = mock(WorkManager.class);
            mockedWorkManagerAccess.when(() -> WorkManagerAccess.getWorkManager(mockApplication)).thenReturn(mockWorkManager);
            ListenableWorker.Result result = reminderWork.doWork();
            assertInstanceOf(ListenableWorker.Result.Failure.class, result);
        }
        // Reminder event is null
        try (MockedConstruction<MedicineRepository> ignored = mockConstruction(MedicineRepository.class, (mock, context) -> {
            when(mock.getReminder(REMINDER_ID)).thenReturn(new Reminder(MEDICINE_ID));
            when(mock.getOnlyMedicine(MEDICINE_ID)).thenReturn(new Medicine("Test"));
            when(mock.getReminderEvent(REMINDER_EVENT_ID)).thenReturn(null);
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
            when(mock.getReminder(REMINDER_ID)).thenReturn(new Reminder(MEDICINE_ID));
            FullMedicine medicine = new FullMedicine();
            medicine.medicine = new Medicine("TestMedicine");
            medicine.tags = new ArrayList<>();
            when(mock.getMedicine(MEDICINE_ID)).thenReturn(medicine);
            ReminderEvent reminderEvent = new ReminderEvent();
            reminderEvent.reminderId = REMINDER_ID;
            reminderEvent.reminderEventId = REMINDER_EVENT_ID;
            when(mock.getReminderEvent(REMINDER_EVENT_ID)).thenReturn(reminderEvent);
        });
             MockedConstruction<NotificationCompat.Builder> ignored2 = mockConstruction(NotificationCompat.Builder.class, (mock, context) -> {
                 // Implicitly verify arguments because invalid arguments will break the call chain of the builder
                 assertEquals(String.format("%d", 3), context.arguments().get(1));
                 when(mock.setSmallIcon(R.drawable.capsule)).thenReturn(mock);
                 when(mock.setStyle(any())).thenReturn(mock);
                 when(mock.setContentTitle(NOTIFICATION_TITLE)).thenReturn(mock);
                 when(mock.setContentText("NotificationContent")).thenReturn(mock); // Should not be necessary?
                 when(mock.setPriority(NotificationCompat.PRIORITY_DEFAULT)).thenReturn(mock);
                 when(mock.setContentIntent(any())).thenReturn(mock);
                 when(mock.setCategory(Notification.CATEGORY_REMINDER)).thenReturn(mock);
                 when(mock.setLargeIcon((Bitmap) null)).thenReturn(mock);
                 when(mock.addAction(eq(R.drawable.check2_circle), eq(NOTIFICATION_TAKEN), any())).thenReturn(mock());
                 when(mock.build()).thenReturn(new Notification());
             });
             MockedConstruction<MedicineIcons> ignored3 = mockConstruction(MedicineIcons.class, (mock, context) -> when(mock.getIconBitmap(0)).thenReturn(null));
             MockedStatic<WorkManagerAccess> mockedWorkManagerAccess = mockStatic(WorkManagerAccess.class);
             MockedStatic<PreferenceManager> mockedPreferencesManager = mockStatic(PreferenceManager.class);
             MockedStatic<DateFormat> dateAccessMockedStatic = mockStatic(DateFormat.class);
             MockedConstruction<NotificationSoundManager> ignored4 = mockConstruction(NotificationSoundManager.class)) {
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
            assertEquals(NOTIFICATION_ID, captor.getValue().notificationId);
            assertEquals(REMINDER_ID, captor.getValue().reminderId);
            assertEquals(REMINDER_EVENT_ID, captor.getValue().reminderEventId);
            verify(mockNotificationManager, times(1)).notify(eq(NOTIFICATION_ID), any());
        }
    }

    @Test
    public void testDoWorkNewReminder() {
        try (MockedConstruction<MedicineRepository> mockedMedicineRepositories = mockConstruction(MedicineRepository.class, (mock, context) -> {
            Reminder reminder = new Reminder(MEDICINE_ID);
            reminder.reminderId = REMINDER_ID;
            when(mock.getReminder(REMINDER_ID)).thenReturn(reminder);
            FullMedicine medicine = new FullMedicine();
            medicine.medicine = new Medicine("TestMedicine");
            medicine.medicine.iconId = 16;
            medicine.tags = new ArrayList<>();
            when(mock.getMedicine(MEDICINE_ID)).thenReturn(medicine);
            when(mock.insertReminderEvent(any())).thenReturn((long) REMINDER_EVENT_ID);
        });
             MockedConstruction<NotificationCompat.Builder> ignored2 = mockConstruction(NotificationCompat.Builder.class, (mock, context) -> {
                 // Implicitly verify arguments because invalid arguments will break the call chain of the builder
                 assertEquals(String.format("%d", 3), context.arguments().get(1));
                 when(mock.setSmallIcon(R.drawable.capsule)).thenReturn(mock);
                 when(mock.setStyle(any())).thenReturn(mock);
                 when(mock.setContentTitle(NOTIFICATION_TITLE)).thenReturn(mock);
                 when(mock.setContentText("NotificationContent")).thenReturn(mock); // Should not be necessary?
                 when(mock.setPriority(NotificationCompat.PRIORITY_DEFAULT)).thenReturn(mock);
                 when(mock.setContentIntent(any())).thenReturn(mock);
                 when(mock.setLargeIcon((Bitmap) null)).thenReturn(mock);
                 when(mock.setCategory(Notification.CATEGORY_REMINDER)).thenReturn(mock);
                 when(mock.addAction(eq(R.drawable.check2_circle), eq(NOTIFICATION_TAKEN), any())).thenReturn(mock());
                 when(mock.build()).thenReturn(new Notification());
             });
             MockedConstruction<MedicineIcons> ignored3 = mockConstruction(MedicineIcons.class, (mock, context) -> when(mock.getIconBitmap(16)).thenReturn(null));
             MockedStatic<WorkManagerAccess> mockedWorkManagerAccess = mockStatic(WorkManagerAccess.class);
             MockedStatic<PreferenceManager> mockedPreferencesManager = mockStatic(PreferenceManager.class);
             MockedStatic<DateFormat> dateAccessMockedStatic = mockStatic(DateFormat.class);
             MockedConstruction<NotificationSoundManager> ignored4 = mockConstruction(NotificationSoundManager.class)) {
            WorkManager mockWorkManager = mock(WorkManager.class);
            mockedWorkManagerAccess.when(() -> WorkManagerAccess.getWorkManager(mockApplication)).thenReturn(mockWorkManager);
            mockedPreferencesManager.when(() -> PreferenceManager.getDefaultSharedPreferences(mockApplication)).thenReturn(mockSharedPreferences);
            dateAccessMockedStatic.when(() -> DateFormat.getTimeFormat(any())).thenReturn(java.text.DateFormat.getTimeInstance());

            ReminderNotificationData data = ReminderNotificationData.Companion.fromArrays(null, new int[]{REMINDER_ID}, new int[]{REMINDER_EVENT_ID}, Instant.ofEpochSecond(Reminder.DEFAULT_TIME * 60 + 24 * 60 * 60));
            Data.Builder inputData = new Data.Builder();
            data.toBuilder(inputData);
            when(workerParams.getInputData()).thenReturn(inputData.build());

            // Expected to pass
            ListenableWorker.Result result = reminderWork.doWork();
            assertInstanceOf(ListenableWorker.Result.Success.class, result);

            // Check if reminder event was updated with the generated notification ID
            MedicineRepository mockedMedicineRepository = mockedMedicineRepositories.constructed().get(0);
            ArgumentCaptor<ReminderEvent> captor = ArgumentCaptor.forClass(ReminderEvent.class);
            verify(mockedMedicineRepository, times(1)).updateReminderEvent(captor.capture());
            assertEquals(NOTIFICATION_ID, captor.getValue().notificationId);
            assertEquals(REMINDER_ID, captor.getValue().reminderId);
            assertEquals(REMINDER_EVENT_ID, captor.getValue().reminderEventId);
            LocalDateTime reminderEventTime = LocalDateTime.of(LocalDate.ofEpochDay(1), LocalTime.of(Reminder.DEFAULT_TIME / 60, Reminder.DEFAULT_TIME % 60));
            assertEquals(reminderEventTime
                    .toEpochSecond(ZoneId.systemDefault().getRules().getOffset(reminderEventTime)), captor.getValue().remindedTimestamp);
            verify(mockNotificationManager, times(1)).notify(eq(NOTIFICATION_ID), any());
            verify(mockedMedicineRepository, times(1)).updateReminderEvent(any());
        }
    }

}