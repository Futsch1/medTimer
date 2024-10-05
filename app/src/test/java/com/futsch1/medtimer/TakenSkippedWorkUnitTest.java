package com.futsch1.medtimer;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_EVENT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AlarmManager;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;

import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.reminders.SkippedWork;
import com.futsch1.medtimer.reminders.TakenWork;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.robolectric.annotation.Config;

import java.time.Instant;

import tech.apter.junit.jupiter.robolectric.RobolectricExtension;

@ExtendWith(RobolectricExtension.class)
@Config(sdk = 34)
@SuppressWarnings("java:S5786") // Required for Robolectric extension
public class TakenSkippedWorkUnitTest {
    private final int reminderEventId = 12;

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
                .putInt(EXTRA_REMINDER_EVENT_ID, reminderEventId)
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
        reminderEvent.reminderEventId = reminderEventId;
        reminderEvent.status = ReminderEvent.ReminderStatus.RAISED;
        reminderEvent.processedTimestamp = Instant.now().getEpochSecond();

        try (MockedConstruction<MedicineRepository> mockedMedicineRepositories = mockConstruction(MedicineRepository.class, (mock, context) -> when(mock.getReminderEvent(reminderEventId)).thenReturn(reminderEvent))) {
            // Expected to pass
            ListenableWorker.Result result = worker.doWork();
            assertInstanceOf(ListenableWorker.Result.Success.class, result);

            // Check if reminder event was updated with the generated notification ID
            MedicineRepository mockedMedicineRepository = mockedMedicineRepositories.constructed().get(0);
            ArgumentCaptor<ReminderEvent> captor = ArgumentCaptor.forClass(ReminderEvent.class);
            verify(mockedMedicineRepository, times(1)).updateReminderEvent(captor.capture());
            assertEquals(notificationId, captor.getValue().notificationId);
            assertEquals(reminderId, captor.getValue().reminderId);
            assertEquals(reminderEventId, captor.getValue().reminderEventId);
            assertEquals(status, captor.getValue().status);
            verify(mockNotificationManager, times(1)).cancel(notificationId);
            ArgumentCaptor<PendingIntent> captor1 = ArgumentCaptor.forClass(PendingIntent.class);
            verify(mockAlarmManager, times(1)).cancel(captor1.capture());
        }
    }

    @Test
    public void testDoWorkSkipped() {
        WorkerParameters workerParams = mock(WorkerParameters.class);
        Data inputData = new Data.Builder()
                .putInt(EXTRA_REMINDER_EVENT_ID, reminderEventId)
                .build();
        when(workerParams.getInputData()).thenReturn(inputData);
        SkippedWork skippedWork = new SkippedWork(mockApplication, workerParams);

        testWork(skippedWork, ReminderEvent.ReminderStatus.SKIPPED);
    }
}
