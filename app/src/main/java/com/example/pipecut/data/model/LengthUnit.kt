package com.example.pipecut.data.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Supported measurement units for pipe lengths and cutting parameters.
 * Canonical internal unit is Millimeters (MM).
 */
enum class LengthUnit(val symbol: String, val displayName: String, val mmRatio: Double) {
    MM("mm", "Millimeters (mm)", 1.0),
    CM("cm", "Centimeters (cm)", 10.0),
    INCH("in", "Inches (in)", 25.4),
    FT("ft", "Feet (ft)", 304.8);

    fun toMm(value: Double): Double = value * mmRatio

    fun fromMm(valueInMm: Double): Double = valueInMm / mmRatio

    fun format(value: Double, decimals: Int = 2): String {
        val pattern = if (decimals <= 0) "#,##0" else "#,##0." + "#".repeat(decimals)
        val symbols = DecimalFormatSymbols(Locale.US)
        val df = DecimalFormat(pattern, symbols)
        return "${df.format(value)} $symbol"
    }

    companion object {
        fun fromString(name: String?): LengthUnit {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) || it.symbol.equals(name, ignoreCase = true) } ?: MM
        }
    }
}
