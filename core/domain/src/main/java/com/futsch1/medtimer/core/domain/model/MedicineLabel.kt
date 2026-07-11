package com.futsch1.medtimer.core.domain.model

/**
 * Remembers a snippet of package text (normalized: lowercase, trimmed) that the OCR scanner
 * matched to a medicine, either because it contained the medicine's own name or because the user
 * picked it manually once. Many-to-one: the same medicine can have several remembered snippets,
 * since box art/brand naming varies between package sizes or re-prints.
 *
 * [quantity] remembers the pill/dose count this package contains once the user has confirmed it
 * (or the OCR text let it be parsed with confidence), so future scans of a matching package don't
 * need to ask again.
 */
data class MedicineLabel(val text: String, val medicineId: Int, val quantity: Double? = null)
