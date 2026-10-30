package com.equinox.audioeq.model

enum class FilterType { PEAKING, LOW_SHELF, HIGH_SHELF, LOW_PASS, HIGH_PASS }

data class ParametricFilter(
    val type: FilterType,
    val frequencyHz: Float,
    val gainDb: Float,
    val qFactor: Float,
)

data class AutoEqProfile(
    val id: String,
    val brand: String,
    val model: String,
    val source: String,
    val target: String,
    val preampDb: Float,
    /** Center frequency (Hz) to gain (dB). May be sparse or dense (imported GraphicEQ files). */
    val graphicEqBands: Map<Int, Float>,
    val parametricFilters: List<ParametricFilter>,
) {
    val displayName: String get() = if (brand.isBlank()) model else "$brand $model"
}
