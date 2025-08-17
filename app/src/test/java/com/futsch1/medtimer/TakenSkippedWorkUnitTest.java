package com.futsch1.medtimer;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_AMOUNT;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_MEDICINE_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_EVENT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AlarmManager;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;

import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.reminders.SkippedWork;
import com.futsch1.medtimer.reminders.TakenWork;

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
public class TakenSkippedWorkUnitTest {
    private static final int REMINDER_EVENT_ID = 12;

    @Mock
    private Application mockApplication;
    private NotificationManager mockNotificationManager;
    private AlarmManager mockAlarmManager;

    @BeforeEach
    public void setUp() {

        mockApplication = mock(Application.class);

        mockNotificationManager = mock(NotificationManager.class);
        when(mockApplication.getSystemService(NotificationManager.class)).thenReturn(mockNotificationManager);

        mockAlarmManager = mock(AlarmManager.class);
        when(mockApplication.getSystemService(AlarmManager.class)).thenReturn(mockAlarmManager);
    }

    @Test
    public void testDoWorkTaken() {
        WorkerParameters workerParams = mock(WorkerParameters.class);
        Data inputData = new Data.Builder()
                .putInt(EXTRA_REMINDER_EVENT_ID, REMINDER_EVENT_ID)
                .build();
        when(workerParams.getInputData()).thenReturn(inputData);
        TakenWork takenWork = new TakenWork(mockApplication, workerParams);

        testWork(takenWork, ReminderEvent.ReminderStatus.TAKEN);
    }

    private void testWork(Worker worker, ReminderEvent.ReminderStatus status) {
        ReminderEvent reminderEvent = new ReminderEvent();
        int notificationId = 14;
        reminderEvent.notificationId = notificationId;
        int reminderId = 11;
        reminderEvent.reminderId = reminderId;
        reminderEvent.reminderEventId = REMINDER_EVENT_ID;
        reminderEvent.status = ReminderEvent.ReminderStatus.RAISED;
        reminderEvent.processedTimestamp = Instant.now().getEpochSecond();
        Reminder reminder = new Reminder(5);
        reminder.amount = "4";

        try (MockedConstruction<MedicineRepository> mockedMedicineRepositories = mockConstruction(MedicineRepository.class, (mock, context) -> {
            when(mock.getReminderEvent(REMINDER_EVENT_ID)).thenReturn(reminderEvent);
            when(mock.getReminder(reminderId)).thenReturn(reminder);
        });
             MockedStatic<WorkManagerAccess> mockedWorkManagerAccess = mockStatic(WorkManagerAccess.class)) {
            WorkManager mockWorkManager = mock(WorkManager.class);
            mockedWorkManagerAccess.when(() -> WorkManagerAccess.getWorkManager(mockApplication)).thenReturn(mockWorkManager);
            // Expected to pass
            ListenableWorker.Result result = worker.doWork();
            assertInstanceOf(ListenableWorker.Result.Success.class, result);

            // Check if reminder event was updated with the generated notification ID
            MedicineRepository mockedMedicineRepository = mockedMedicineRepositories.constructed().get(0);
            ArgumentCaptor<ReminderEvent> captor = ArgumentCaptor.forClass(ReminderEvent.class);
            verify(mockedMedicineRepository, times(1)).updateReminderEvent(captor.capture());
            assertEquals(notificationId, captor.getValue().notificationId);
            assertEquals(reminderId, captor.getValue().reminderId);
            assertEquals(REMINDER_EVENT_ID, captor.getValue().reminderEventId);
            assertEquals(status, captor.getValue().status);
            verify(mockNotificationManager, times(1)).cancel(notificationId);

            ArgumentCaptor<PendingIntent> captor1 = ArgumentCaptor.forClass(PendingIntent.class);
            verify(mockAlarmManager, times(1)).cancel(captor1.capture());

            if (status == ReminderEvent.ReminderStatus.TAKEN) {
                ArgumentCaptor<WorkRequest> captor2 = ArgumentCaptor.forClass(WorkRequest.class);
                verify(mockWorkManager, times(1)).enqueue(captor2.capture());
                assertInstanceOf(OneTimeWorkRequest.class, captor2.getValue());
                assertEquals(reminder.amount, captor2.getValue().getWorkSpec().input.getString(EXTRA_AMOUNT));
                assertEquals(reminder.medicineRelId, captor2.getValue().getWorkSpec().input.getInt(EXTRA_MEDICINE_ID, -1));
            }
        }
    }

    @Test
    public void testDoWorkSkipped() {
        WorkerParameters workerParams = mock(WorkerParameters.class);
        Data inputData = new Data.Builder()
                .putInt(EXTRA_REMINDER_EVENT_ID, REMINDER_EVENT_ID)
                .build();
        when(workerParams.getInputData()).thenReturn(inputData);
        SkippedWork skippedWork = new SkippedWork(mockApplication, workerParams);

        testWork(skippedWork, ReminderEvent.ReminderStatus.SKIPPED);
    }
}
