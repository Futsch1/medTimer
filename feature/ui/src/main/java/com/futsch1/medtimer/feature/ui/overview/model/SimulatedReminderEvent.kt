package com.futsch1.medtimer.feature.ui.overview.model

import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.domain.model.ScheduledReminder
import com.futsch1.medtimer.core.domain.model.SimulatedReminder
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class SimulatedReminderEvent @AssistedInject constructor(
    preferencesDataSource: PreferencesDataSource,
    @Assisted private val simulatedReminder: SimulatedReminder
) :
    OverviewEvent(preferencesDataSource) {

    @AssistedFactory
    fun interface Factory {
        fun create(simulatedReminder: SimulatedReminder): SimulatedReminderEvent
    }

    val scheduledReminder = simulatedReminder.scheduledReminder

    override val content: OverviewEventContent = OverviewEventContent(
        reminderType = scheduledReminder.reminder.reminderType,
        time = scheduledReminder.timestamp,
        medicineName = scheduledReminder.medicine.name,
        dose = dose(scheduledReminder),
        stock = projectedStock(simulatedReminder),
        expirationDate = scheduledReminder.medicine.expirationDate
            .takeIf { scheduledReminder.reminder.reminderType == ReminderType.EXPIRATION_DATE },
        useRelativeTime = preferencesDataSource.preferences.value.useRelativeDateTime,
    )

    override val id: Int = java.util.Objects.hash(
        scheduledReminder.reminder.id,
        scheduledReminder.timestamp.epochSecond
    )

    override val timestamp: Long
        get() = scheduledReminder.timestamp.epochSecond
    override val icon: Int
        get() = scheduledReminder.medicine.iconId
    override val color: Int?
        get() = if (scheduledReminder.medicine.useColor) scheduledReminder.medicine.color else null
    override val state: OverviewState
        get() = OverviewState.PENDING
    override val reminderId: Int
        get() = scheduledReminder.reminder.id
    override val cannotSkipMedicine: Boolean
        get() = scheduledReminder.medicine.cannotBeSkipped

    private fun dose(scheduledReminder: ScheduledReminder) =
        if (scheduledReminder.reminder.isOutOfStockOrExpirationReminder) "" else scheduledReminder.reminder.amount

    /** The stock this dose is projected to consume; absent when the amount or the stock is unknown. */
    private fun projectedStock(simulatedReminder: SimulatedReminder): StockChange? {
        val reminder = simulatedReminder.scheduledReminder.reminder
        val medicine = simulatedReminder.scheduledReminder.medicine
        if (reminder.reminderType == ReminderType.EXPIRATION_DATE ||
            reminder.variableAmount ||
            !medicine.isStockManagementActive()
        ) {
            return null
        }
        return StockChange(simulatedReminder.stockBefore, simulatedReminder.stockAfter, medicine.unit)
    }
}
