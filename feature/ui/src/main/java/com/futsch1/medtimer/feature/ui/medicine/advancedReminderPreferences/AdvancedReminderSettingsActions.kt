package com.futsch1.medtimer.feature.ui.medicine.advancedReminderPreferences

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.futsch1.medtimer.core.common.di.ApplicationScope
import com.futsch1.medtimer.core.common.di.Dispatcher
import com.futsch1.medtimer.core.common.di.MedTimerDispatchers
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.feature.ui.medicine.LinkedReminderHandling
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.futsch1.medtimer.core.ui.R as CoreUiR

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
    @param:ApplicationScope private val applicationScope: CoroutineScope,
    @param:Dispatcher(MedTimerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    @param:Dispatcher(MedTimerDispatchers.Main) private val mainDispatcher: CoroutineDispatcher
) {

    @AssistedFactory
    fun interface Factory {
        fun create(fragment: Fragment): AdvancedReminderSettingsActions
    }

    lateinit var reminder: Reminder

    fun duplicate() {
        if (!this::reminder.isInitialized) return
        val context = fragment.requireContext().applicationContext
        applicationScope.launch(ioDispatcher) {
            reminderRepository.create(reminder.copy(id = 0))
            withContext(mainDispatcher) {
                Toast.makeText(context, CoreUiR.string.successfully_created_reminder, Toast.LENGTH_SHORT).show()
            }
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

object AdvancedReminderSettingsTestTags {
    const val MENU = "advanced_reminder_settings_menu"
}

@Composable
fun AdvancedReminderSettingsMenu(actions: AdvancedReminderSettingsActions) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(CoreUiR.drawable.three_dots_vertical),
                contentDescription = stringResource(CoreUiR.string.more_options),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.testTag(AdvancedReminderSettingsTestTags.MENU),
        ) {
            @Composable
            fun item(labelRes: Int, iconRes: Int, onClick: () -> Unit) {
                DropdownMenuItem(
                    text = { Text(stringResource(labelRes)) },
                    leadingIcon = { Icon(painterResource(iconRes), contentDescription = null) },
                    onClick = {
                        expanded = false
                        onClick()
                    },
                )
            }

            item(CoreUiR.string.duplicate, CoreUiR.drawable.copy, actions::duplicate)
            item(CoreUiR.string.delete, CoreUiR.drawable.trash, actions::delete)
        }
    }
}
