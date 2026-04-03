package com.futsch1.medtimer.overview.actions

import androidx.annotation.IdRes
import com.futsch1.medtimer.R

@Suppress("ANNOTATION_WILL_BE_APPLIED_ALSO_TO_PROPERTY_OR_FIELD")
enum class Button(@IdRes val associatedId: Int, @IdRes val anchorId: Int) {
    TAKEN(R.id.takenButton, R.id.anchorTakenButton),
    ACKNOWLEDGED(R.id.acknowledgedButton, R.id.anchorAcknowledgedButton),
    SKIPPED(R.id.skippedButton, R.id.anchorSkippedButton),
    RERAISE(R.id.reraiseButton, R.id.anchorReraiseButton),
    RESCHEDULE(R.id.rescheduleButton, R.id.anchorRescheduleButton),
    DELETE(R.id.deleteButton, R.id.anchorDeleteButton);

    companion object {
        fun fromId(@IdRes id: Int): Button {
            return entries.find { it.associatedId == id } ?: throw IllegalArgumentException("No button with id $id")
        }
    }
}