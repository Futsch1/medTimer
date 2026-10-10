package com.futsch1.medtimer.feature.ui.medicine

import com.futsch1.medtimer.core.domain.model.Medicine

/** A new medicine copied from [medicine], renamed by [copyName] so the copy can be told apart from the original. */
internal fun duplicateOf(medicine: Medicine, copyName: (String) -> String): Medicine =
    medicine.copy(id = 0, name = copyName(medicine.name))
