package com.futsch1.medtimer.feature.ui.overview.actions

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.futsch1.medtimer.core.ui.R

@Suppress("ANNOTATION_WILL_BE_APPLIED_ALSO_TO_PROPERTY_OR_FIELD")
enum class Button(@DrawableRes val iconRes: Int, @StringRes val labelRes: Int) {
    TAKEN(R.drawable.check2_circle, R.string.taken),

    /** Taken, with a picked time instead of the current one. Directly after [TAKEN], so that it sits next to it in every list. */
    TAKEN_AT(R.drawable.calendar_check, R.string.taken_at),
    ACKNOWLEDGED(R.drawable.check2_circle, R.string.acknowledged),
    SKIPPED(R.drawable.x_circle, R.string.skipped),
    RERAISE(R.drawable.bell, R.string.re_raise_event),
    RESCHEDULE(R.drawable.bell, R.string.reschedule_reminder),
    DELETE(R.drawable.trash, R.string.delete);

    /** The action that a long press on this one runs instead, when the menu also offers it. */
    val longPressAlternative: Button?
        get() = if (this == TAKEN) TAKEN_AT else null
}
