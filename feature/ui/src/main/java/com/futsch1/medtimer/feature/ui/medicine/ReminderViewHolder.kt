package com.futsch1.medtimer.feature.ui.medicine

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.futsch1.medtimer.core.common.di.Dispatcher
import com.futsch1.medtimer.core.common.di.MedTimerDispatchers
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderTime
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.ui.ReminderSummaryFormatter
import com.futsch1.medtimer.core.ui.getHelp
import com.futsch1.medtimer.core.ui.getIcon
import com.futsch1.medtimer.core.ui.getTitle
import com.futsch1.medtimer.feature.ui.R
import com.futsch1.medtimer.feature.ui.helpers.AmountTextWatcher
import com.futsch1.medtimer.feature.ui.medicine.editors.TimeEditor
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReminderViewHolder @AssistedInject constructor(
    @Assisted parent: ViewGroup,
    @Assisted private val fragmentActivity: FragmentActivity,
    private val linkedReminderHandlingFactory: LinkedReminderHandling.Factory,
    private val timeEditorFactory: TimeEditor.Factory,
    private val reminderSummaryFormatter: ReminderSummaryFormatter,
    @param:Dispatcher(MedTimerDispatchers.IO) private val dispatcher: CoroutineDispatcher,
    @param:Dispatcher(MedTimerDispatchers.Main) private val mainDispatcher: CoroutineDispatcher
) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.card_reminder, parent, false)) {

    @AssistedFactory
    interface Factory {
        fun create(parent: ViewGroup, fragmentActivity: FragmentActivity): ReminderViewHolder
    }

    private val editTime: TextInputEditText = itemView.findViewById(R.id.editReminderTime)
    private val editAmount: TextInputEditText = itemView.findViewById(R.id.editAmount)
    private val advancedSettings: MaterialButton = itemView.findViewById(R.id.openAdvancedSettings)
    private val advancedSettingsSummary: TextView = itemView.findViewById(R.id.advancedSettingsSummary)
    private val editTimeLayout: TextInputLayout = itemView.findViewById(R.id.editReminderTimeLayout)
    private val reminderTypeIcon: ImageView = itemView.findViewById(R.id.reminderTypeIcon)

    private lateinit var reminder: Reminder
    private var timeEditor: TimeEditor? = null

    @SuppressLint("SetTextI18n")
    fun bind(reminder: Reminder, medicine: Medicine) {
        this.reminder = reminder

        setupTimeEditor()

        advancedSettings.setOnClickListener { _: View? -> onClickAdvancedSettings(reminder) }

        editAmount.setText(reminder.amount)

        if (reminder.isOutOfStockOrExpirationReminder) {
            editAmount.visibility = View.GONE
        }

        fragmentActivity.lifecycleScope.launch(dispatcher) {
            val summary = reminderSummaryFormatter.formatReminderSummary(reminder)
            withContext(mainDispatcher) { advancedSettingsSummary.text = summary }
        }

        if (medicine.isStockManagementActive()) {
            editAmount.addTextChangedListener(
                AmountTextWatcher(
                    editAmount
                )
            )
        } else {
            (itemView.findViewById<View?>(R.id.editAmountLayout) as TextInputLayout).isErrorEnabled = false
        }

        setupTypeIcon()

        setupLongPress()
    }

    private fun setupLongPress() {
        itemView.setOnCreateContextMenuListener { menu, _, _ ->
            menu.add(com.futsch1.medtimer.core.ui.R.string.delete).setOnMenuItemClickListener {
                linkedReminderHandlingFactory.create(reminder, fragmentActivity.lifecycleScope).deleteReminder(
                    fragmentActivity,
                    { }, { }
                )
                true
            }
        }
    }

    private fun setupTimeEditor() {
        if (reminder.usesTimeInMinutes) {
            @StringRes val textId = if (reminder.reminderType != ReminderType.LINKED) com.futsch1.medtimer.core.ui.R.string.time else com.futsch1.medtimer.core.ui.R.string.delay
            editTimeLayout.setHint(textId)
            timeEditor = timeEditorFactory.create(fragmentActivity, editTime, reminder.time.minutes, { _ ->

            }, if (reminder.reminderType == ReminderType.LINKED) com.futsch1.medtimer.core.ui.R.string.linked_reminder_delay else null)
        } else {
            editTimeLayout.visibility = View.GONE
        }
    }

    private fun onClickAdvancedSettings(reminder: Reminder) {
        val navController = findNavController(itemView)
        val action =
            if (reminder.isOutOfStockOrExpirationReminder) {
                EditMedicineFragmentDirections.actionEditMedicineFragmentToAdvancedReminderPreferencesStockExpirationFragment(
                    reminder.id
                )
            } else {
                EditMedicineFragmentDirections.actionEditMedicineFragmentToAdvancedReminderPreferencesRootFragment(
                    reminder.id
                )
            }
        try {
            navController.navigate(action)
        } catch (_: IllegalArgumentException) {
            // Intentionally empty (monkey test can cause this to fail)
        }
    }

    private fun setupTypeIcon() {
        val builder = MaterialAlertDialogBuilder(itemView.context)
            .setTitle(reminder.reminderType.getTitle())
            .setMessage(reminder.reminderType.getHelp()).setIcon(reminder.reminderType.getIcon()).setPositiveButton(com.futsch1.medtimer.core.ui.R.string.ok, null)
        reminderTypeIcon.setImageResource(reminder.reminderType.getIcon())
        reminderTypeIcon.setOnClickListener { _: View? -> builder.create().show() }
    }

    fun getUpdatedReminder(): Reminder {
        val minutes = timeEditor?.getMinutes() ?: -1
        val newTime = if (minutes >= 0 && reminder.usesTimeInMinutes) ReminderTime(minutes) else reminder.time

        return reminder.copy(time = newTime, amount = editAmount.text.toString().trim())
    }
}
