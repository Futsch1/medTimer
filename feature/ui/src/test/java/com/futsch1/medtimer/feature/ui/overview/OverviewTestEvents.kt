package com.futsch1.medtimer.feature.ui.overview

import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.domain.model.UserPreferences
import com.futsch1.medtimer.feature.ui.overview.model.OverviewEvent
import com.futsch1.medtimer.feature.ui.overview.model.OverviewEventContent
import com.futsch1.medtimer.feature.ui.overview.model.OverviewState
import kotlinx.coroutines.flow.MutableStateFlow
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant

/** A minimal [OverviewEvent] for tests that only care about its time, its name and its state. */
internal class TestOverviewEvent(
    override val id: Int,
    override val timestamp: Long,
    medicineName: String,
    override val state: OverviewState = OverviewState.PENDING,
    override val reminderId: Int = id,
) : OverviewEvent(testPreferencesDataSource()) {
    override val content = OverviewEventContent(
        reminderType = ReminderType.TIME_BASED,
        time = Instant.ofEpochSecond(timestamp),
        medicineName = medicineName,
        dose = "",
    )
    override val icon = 0
    override val color: Int? = null
    override val cannotSkipMedicine = false
}

private fun testPreferencesDataSource(): PreferencesDataSource {
    val preferencesDataSource: PreferencesDataSource = mock()
    whenever(preferencesDataSource.preferences).thenReturn(MutableStateFlow(UserPreferences.default()))
    return preferencesDataSource
}
