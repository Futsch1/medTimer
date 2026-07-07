package com.futsch1.medtimer.core.domain.model

/**
 * Maps a scanned barcode (EAN/UPC/etc.) to a medicine.
 *
 * Many-to-one: the same medicine can have several barcodes, because different
 * package sizes of the same drug carry different codes. The mapping is built
 * offline by the user's own scans — no external drug database is involved.
 */
data class Barcode(val barcode: String, val medicineId: Int)
