package com.futsch1.medtimer.feature.ui.medicine.scan

/**
 * Best-effort extraction of a pill/dose count from OCR'd package text (already normalized:
 * lowercase, single-spaced). Covers the packaging phrasings most commonly seen on Italian and
 * English medicine boxes. Returns null when nothing matches confidently enough, so the caller can
 * fall back to asking the user (and remembering the answer for next time).
 */
object QuantityParser {
    private val UNIT_SUFFIXED = Regex(
        "\\b(\\d{1,4})\\s?(?:cpr|cps|cp|compress[ae]|capsul[ae]|pastigli[ae]|confetti|bustin[ae]|fiale|supposte|gocce|pillole|tavolette|tablet(?:s)?|capsule(?:s)?|pz|pezzi)\\b"
    )
    private val COUNT_PREFIXED = Regex("\\b(?:da|x|n[.\\s]?)\\s?(\\d{1,4})\\b")

    fun parse(normalizedText: String): Double? {
        val match = UNIT_SUFFIXED.find(normalizedText) ?: COUNT_PREFIXED.find(normalizedText)
        return match?.groupValues?.get(1)?.toDoubleOrNull()?.takeIf { it > 0 }
    }
}
