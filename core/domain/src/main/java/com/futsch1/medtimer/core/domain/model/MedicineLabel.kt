package com.futsch1.medtimer.core.domain.model

/**
 * Remembers a snippet of package text (normalized: lowercase, trimmed) that the OCR scanner
 * matched to a medicine, either because it contained the medicine's own name or because the user
 * picked it manually once. Many-to-one: the same medicine can have several remembered snippets,
 * since box art/brand naming varies between package sizes or re-prints.
 */
data class MedicineLabel(val text: String, val medicineId: Int)
