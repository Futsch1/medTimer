package com.futsch1.medtimer.processortests

import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.feature.reminders.ShowReminderNotificationProcessor
import com.futsch1.medtimer.feature.reminders.StockHandlingProcessor
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StockHandlingProcessorTest {

    private val medicineRepository: MedicineRepository = mock()
    private val showReminderNotificationProcessor: ShowReminderNotificationProcessor = mock()
    private val processor = StockHandlingProcessor(medicineRepository) { showReminderNotificationProcessor }

    private fun medicineBelowThreshold(active: Boolean) = Medicine.default().copy(
        id = MEDICINE_ID,
        name = "Medicine A",
        amount = 2.0,
        reminders = listOf(
            Reminder.default().copy(
                id = 4,
                medicineRelId = MEDICINE_ID,
                active = active,
                outOfStockReminderType = Reminder.OutOfStockReminderType.ALWAYS,
                outOfStockThreshold = 5.0,
            )
        ),
    )

    @Test
    fun `should raise the out of stock reminder when the stock drops below its threshold`() = runBlocking {
        whenever(medicineRepository.decreaseStock(MEDICINE_ID, 1.0)).thenReturn(medicineBelowThreshold(active = true))

        processor.processStock(1.0, MEDICINE_ID, Instant.EPOCH)

        verify(showReminderNotificationProcessor).showReminder(any())
    }

    @Test
    fun `should not raise a deactivated out of stock reminder`() = runBlocking {
        whenever(medicineRepository.decreaseStock(MEDICINE_ID, 1.0)).thenReturn(medicineBelowThreshold(active = false))

        processor.processStock(1.0, MEDICINE_ID, Instant.EPOCH)

        verify(showReminderNotificationProcessor, never()).showReminder(any())
    }

    private companion object {
        const val MEDICINE_ID = 3
    }
}
