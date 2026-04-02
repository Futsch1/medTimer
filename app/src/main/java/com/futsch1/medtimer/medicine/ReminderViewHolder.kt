package com.futsch1.medtimer.medicine

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
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.database.ReminderEntity.ReminderType
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.helpers.AmountTextWatcher
import com.futsch1.medtimer.helpers.ReminderSummaryFormatter
import com.futsch1.medtimer.medicine.editors.TimeEditor
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

    private lateinit var reminder: ReminderEntity
    private var timeEditor: TimeEditor? = null

    @SuppressLint("SetTextI18n")
    fun bind(reminder: ReminderEntity, fullMedicine: FullMedicineEntity) {
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

        if (fullMedicine.isStockManagementActive) {
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
            menu.add(R.string.delete).setOnMenuItemClickListener {
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
            @StringRes val textId = if (reminder.reminderType != ReminderType.LINKED) R.string.time else R.string.delay
            editTimeLayout.setHint(textId)
            timeEditor = timeEditorFactory.create(fragmentActivity, editTime, reminder.timeInMinutes, { minutes: Int? ->
                reminder.timeInMinutes = minutes!!
            }, if (reminder.reminderType == ReminderType.LINKED) R.string.linked_reminder_delay else null)
        } else {
            editTimeLayout.visibility = View.GONE
        }
    }

    private fun onClickAdvancedSettings(reminder: ReminderEntity) {
        val navController = findNavController(itemView)
        val action =
            if (reminder.isOutOfStockOrExpirationReminder) {
                EditMedicineFragmentDirections.actionEditMedicineFragmentToAdvancedReminderPreferencesStockExpirationFragment(
                    reminder.reminderId
                )
            } else {
                EditMedicineFragmentDirections.actionEditMedicineFragmentToAdvancedReminderPreferencesRootFragment(
                    reminder.reminderId
                )
            }
        try {
            navController.navigate(action)
        } catch (_: IllegalArgumentException) {
            // Intentionally empty (monkey test can cause this to fail)
        }
    }

    private fun setupTypeIcon() {
        var titleText: Int
        var helpText: Int
        var iconId: Int
        when (reminder.reminderType) {
            ReminderType.TIME_BASED -> {
                iconId = R.drawable.calendar_event
                titleText = R.string.time_based_reminder
                helpText = R.string.time_based_reminder_help
            }

            ReminderType.LINKED -> {
                iconId = R.drawable.link
                titleText = R.string.linked_reminder
                helpText = R.string.linked_reminder_help
            }

            ReminderType.CONTINUOUS_INTERVAL -> {
                iconId = R.drawable.repeat
                titleText = R.string.continuous_interval_reminder
                helpText = R.string.continuous_interval_reminder_help
            }

            ReminderType.WINDOWED_INTERVAL -> {
                iconId = R.drawable.interval
                titleText = R.string.windowed_interval_reminder
                helpText = R.string.windowed_interval_reminder_help
            }

            ReminderType.OUT_OF_STOCK -> {
                iconId = R.drawable.box_seam
                titleText = R.string.out_of_stock_reminder
                helpText = R.string.out_of_stock_reminder_help
            }

            ReminderType.EXPIRATION_DATE -> {
                iconId = R.drawable.ban
                titleText = R.string.expiration_date
                helpText = R.string.expiration_date_reminder_help
            }

            ReminderType.REFILL -> {
                // Never created here, so keep empty
                iconId = 0
                titleText = 0
                helpText = 0
                assert(false)
            }
        }
        val builder = MaterialAlertDialogBuilder(itemView.context)
            .setTitle(titleText)
            .setMessage(helpText).setIcon(iconId).setPositiveButton(R.string.ok, null)
        reminderTypeIcon.setImageResource(iconId)
        reminderTypeIcon.setOnClickListener { _: View? -> builder.create().show() }
    }

    fun getUpdatedReminder(): ReminderEntity {
        reminder.amount = editAmount.getText().toString().trim()
        if (timeEditor != null) {
            val minutes = timeEditor!!.getMinutes()
            if (minutes >= 0) {
                reminder.timeInMinutes = minutes
            }
        }
        return reminder
    }
}
