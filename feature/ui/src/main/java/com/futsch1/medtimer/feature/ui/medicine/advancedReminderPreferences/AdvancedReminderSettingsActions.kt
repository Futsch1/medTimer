package com.futsch1.medtimer.feature.ui.medicine.advancedReminderPreferences

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.futsch1.medtimer.core.common.di.Dispatcher
import com.futsch1.medtimer.core.common.di.MedTimerDispatchers
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.feature.ui.medicine.LinkedReminderHandling
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import com.futsch1.medtimer.core.ui.R as CoreUiR

object AdvancedReminderTestTags {
    const val DUPLICATE = "advanced_reminder_duplicate"
    const val DELETE = "advanced_reminder_delete"
}

/**
 * Duplicate/delete actions for a reminder's advanced settings screen.
 *
 * [reminder] is assigned once the screen has loaded it; both actions no-op until then, as they did
 * while this was a `MenuProvider`.
 */
class AdvancedReminderSettingsActions @AssistedInject constructor(
    @Assisted private val fragment: Fragment,
    private val linkedReminderHandlingFactory: LinkedReminderHandling.Factory,
    private val reminderRepository: ReminderRepository,
    @param:Dispatcher(MedTimerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) {

    @AssistedFactory
    fun interface Factory {
        fun create(fragment: Fragment): AdvancedReminderSettingsActions
    }

    lateinit var reminder: Reminder

    fun duplicate() {
        if (!this::reminder.isInitialized) return
        fragment.lifecycleScope.launch(ioDispatcher) {
            reminderRepository.create(reminder.copy(id = 0))
        }
        NavHostFragment.findNavController(fragment).navigateUp()
    }

    fun delete() {
        if (!this::reminder.isInitialized) return
        linkedReminderHandlingFactory.create(reminder, fragment.lifecycleScope).deleteReminder(
            fragment.requireContext(),
            { NavHostFragment.findNavController(fragment).navigateUp() },
            { }
        )
    }
}

@Composable
fun RowScope.AdvancedReminderSettingsMenu(actions: AdvancedReminderSettingsActions) {
    IconButton(onClick = actions::duplicate, modifier = Modifier.testTag(AdvancedReminderTestTags.DUPLICATE)) {
        Icon(
            painter = painterResource(CoreUiR.drawable.copy),
            contentDescription = stringResource(CoreUiR.string.duplicate),
        )
    }
    IconButton(onClick = actions::delete, modifier = Modifier.testTag(AdvancedReminderTestTags.DELETE)) {
        Icon(
            painter = painterResource(CoreUiR.drawable.trash),
            contentDescription = stringResource(CoreUiR.string.delete),
        )
    }
}
