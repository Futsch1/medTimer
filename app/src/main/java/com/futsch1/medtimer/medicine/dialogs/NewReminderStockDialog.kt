package com.futsch1.medtimer.medicine.dialogs

import android.app.Dialog
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineEntity
import com.futsch1.medtimer.database.ReminderRepository
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.medicine.editors.TimeEditor
import com.futsch1.medtimer.medicine.stockSettings.addDoubleValidator
import com.futsch1.medtimer.model.Reminder
import com.futsch1.medtimer.model.ReminderType
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import java.text.DecimalFormatSymbols
import java.time.LocalDate

class NewReminderStockDialog @AssistedInject constructor(
    @Assisted private val activity: FragmentActivity,
    @Assisted private val medicine: MedicineEntity,
    @Assisted private val reminder: Reminder,
    private val reminderRepository: ReminderRepository,
    private val timeEditorFactory: TimeEditor.Factory
) {
    @AssistedFactory
    interface Factory {
        fun create(activity: FragmentActivity, medicine: MedicineEntity, reminder: Reminder): NewReminderStockDialog
    }

    private val dialog: Dialog = Dialog(activity)

    init {
        dialog.setContentView(R.layout.dialog_new_reminder_stock)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        setupCreateReminder()
        setupVisibilities()
        dialog.findViewById<MaterialButton>(R.id.cancelCreateReminder).setOnClickListener {
            dialog.dismiss()
        }

        setupEditStockThreshold()
        setupEditExpirationDaysBefore()
        setupStockReminderType()

        dialog.show()
    }

    private fun setupEditExpirationDaysBefore() {
        val textInputLayout = dialog.findViewById<TextInputLayout>(R.id.editExpirationDaysBeforeLayout)
        textInputLayout.suffixText = activity.getString(R.string.days_string)
    }

    private fun setupEditStockThreshold() {
        val textInputLayout = dialog.findViewById<TextInputLayout>(R.id.editStockThresholdLayout)
        textInputLayout.suffixText = medicine.unit

        val textInputEditText = dialog.findViewById<TextInputEditText>(R.id.editStockThreshold)

        textInputEditText.setText(MedicineHelper.formatAmount(medicine.amount, ""))
        val separator = DecimalFormatSymbols.getInstance().decimalSeparator
        textInputEditText.setKeyListener(DigitsKeyListener.getInstance("0123456789$separator"))
        textInputEditText.addDoubleValidator()
        textInputEditText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    }

    private fun setupStockReminderType() {
        dialog.findViewById<RadioGroup>(R.id.stockReminderType).setOnCheckedChangeListener { _, checkedId ->
            dialog.findViewById<TextInputLayout>(R.id.editReminderTimeLayout).visibility = if (checkedId == R.id.daily) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    private fun setupVisibilities() {
        val outOfStockVisibility = if (reminder.reminderType == ReminderType.OUT_OF_STOCK) {
            View.VISIBLE
        } else {
            View.GONE
        }
        val expirationDateVisibility = if (reminder.reminderType == ReminderType.EXPIRATION_DATE) {
            View.VISIBLE
        } else {
            View.GONE
        }

        dialog.findViewById<TextInputLayout>(R.id.editStockThresholdLayout).visibility = outOfStockVisibility
        dialog.findViewById<RadioGroup>(R.id.stockReminderType).visibility = outOfStockVisibility

        dialog.findViewById<TextInputLayout>(R.id.editExpirationDaysBeforeLayout).visibility = expirationDateVisibility
        dialog.findViewById<RadioGroup>(R.id.expirationReminderType).visibility = expirationDateVisibility
        dialog.findViewById<TextInputLayout>(R.id.editReminderTimeLayout).visibility = expirationDateVisibility
    }

    private fun setupCreateReminder(
    ) {
        val timeEditor = timeEditorFactory.create(
            activity,
            dialog.findViewById(R.id.editReminderTime),
            reminder.time.toSecondOfDay() / 60,
            { _ -> },
            null
        )

        setupOnClickCreateReminder(
            timeEditor
        )
    }

    private fun setupOnClickCreateReminder(
        timeEditor: TimeEditor
    ) {
        dialog.findViewById<MaterialButton>(R.id.createReminder).setOnClickListener {
            activity.lifecycleScope.launch {
                var updatedReminder = reminder.copy(
                    time = java.time.LocalTime.ofSecondOfDay(timeEditor.getMinutes() * 60L)
                )
                var canCreate = true

                if (reminder.reminderType == ReminderType.OUT_OF_STOCK) {
                    val result = fillOutOfStockReminder(updatedReminder)
                    canCreate = result.first
                    updatedReminder = result.second
                }
                if (reminder.reminderType == ReminderType.EXPIRATION_DATE) {
                    val result = fillExpirationReminder(updatedReminder)
                    canCreate = result.first
                    updatedReminder = result.second
                }

                if (!canCreate) {
                    Toast.makeText(activity, R.string.invalid_input, Toast.LENGTH_SHORT).show()
                } else {
                    reminderRepository.create(updatedReminder)
                    Toast.makeText(
                        activity,
                        R.string.successfully_created_reminder,
                        Toast.LENGTH_LONG
                    ).show()
                    dialog.dismiss()
                }
            }
        }
    }

    private fun fillExpirationReminder(reminder: Reminder): Pair<Boolean, Reminder> {
        return try {
            val expirationReminderType = when (dialog.findViewById<RadioGroup>(R.id.expirationReminderType).checkedRadioButtonId) {
                R.id.onceExpiration -> Reminder.ExpirationReminderType.ONCE
                R.id.dailyExpiration -> Reminder.ExpirationReminderType.DAILY
                else -> Reminder.ExpirationReminderType.OFF
            }
            val daysBefore = dialog.findViewById<TextInputEditText>(R.id.editExpirationDaysBefore).text.toString().toLong()
            Pair(true, reminder.copy(
                expirationReminderType = expirationReminderType,
                periodStart = LocalDate.ofEpochDay(daysBefore)
            ))
        } catch (_: NumberFormatException) {
            Pair(false, reminder)
        }
    }

    private fun fillOutOfStockReminder(reminder: Reminder): Pair<Boolean, Reminder> {
        return try {
            val threshold = dialog.findViewById<TextInputEditText>(R.id.editStockThreshold).text.toString().toDouble()
            val outOfStockReminderType = when (dialog.findViewById<RadioGroup>(R.id.stockReminderType).checkedRadioButtonId) {
                R.id.once -> Reminder.OutOfStockReminderType.ONCE
                R.id.always -> Reminder.OutOfStockReminderType.ALWAYS
                R.id.daily -> Reminder.OutOfStockReminderType.DAILY
                else -> Reminder.OutOfStockReminderType.OFF
            }
            Pair(true, reminder.copy(outOfStockThreshold = threshold, outOfStockReminderType = outOfStockReminderType))
        } catch (_: NumberFormatException) {
            Pair(false, reminder)
        }
    }
}