package com.futsch1.medtimer.remindertable

import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.core.ui.ReminderTableData
import com.futsch1.medtimer.core.ui.ReminderTableRowData
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.database.statusValuesWithoutDeletedAndAcknowledged
import com.futsch1.medtimer.helpers.TableHelper
import com.futsch1.medtimer.overview.EditEventSheetDialog
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderTableFragment : Fragment() {
    private val thread = HandlerThread("EditReminderFromTable").apply { start() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val medicineViewModel = ViewModelProvider(this)[MedicineViewModel::class.java]
        val composeView = ComposeView(requireContext())

        val columnHeaders = TableHelper.getTableHeadersForAnalysis(requireContext()).toImmutableList()
        val zone = ZoneId.systemDefault()

        medicineViewModel.getLiveReminderEvents(0, statusValuesWithoutDeletedAndAcknowledged)
            .observe(viewLifecycleOwner) { reminderEvents: List<ReminderEvent> ->
                val rows = reminderEvents.map { event ->
                    ReminderTableRowData(
                        eventId = event.reminderEventId,
                        takenAt = if (event.status == ReminderEvent.ReminderStatus.TAKEN)
                            Instant.ofEpochSecond(event.processedTimestamp).atZone(zone).toLocalDateTime()
                        else null,
                        takenStatus = getStatusString(event),
                        medicineName = event.medicineName,
                        dosage = event.amount,
                        remindedAt = Instant.ofEpochSecond(event.remindedTimestamp).atZone(zone).toLocalDateTime(),
                    )
                }.toImmutableList()

                val data = ReminderTableData(rows = rows, columnHeaders = columnHeaders)

                ReminderTableBridge.setTableContent(composeView, data) { eventId ->
                    Handler(thread.looper).post {
                        navigateToEditEvent(eventId, medicineViewModel)
                    }
                }
            }

        return composeView
    }

    private fun navigateToEditEvent(eventId: Int, medicineViewModel: MedicineViewModel) {
        val reminderEvent = medicineViewModel.medicineRepository.getReminderEvent(eventId)
        if (reminderEvent != null) {
            requireActivity().runOnUiThread {
                EditEventSheetDialog(requireActivity(), reminderEvent, Dispatchers.IO)
            }
        }
    }

    private fun getStatusString(event: ReminderEvent): String {
        return when (event.status) {
            ReminderEvent.ReminderStatus.RAISED -> " "
            else -> "-"
        }
    }
}