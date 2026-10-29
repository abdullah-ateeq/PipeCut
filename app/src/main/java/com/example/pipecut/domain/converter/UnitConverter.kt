package com.example.pipecut.domain.converter

import com.example.pipecut.data.model.LengthUnit
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object UnitConverter {

    /**
     * Converts a length [value] from [fromUnit] to [toUnit].
     */
    fun convert(value: Double, fromUnit: LengthUnit, toUnit: LengthUnit): Double {
        if (fromUnit == toUnit) return value
        val mm = fromUnit.toMm(value)
        return toUnit.fromMm(mm)
    }

    /**
     * Converts a length [value] from [fromUnit] to Millimeters.
     */
    fun toMm(value: Double, fromUnit: LengthUnit): Double {
        return fromUnit.toMm(value)
    }

    /**
     * Converts a length [valueInMm] from Millimeters to [targetUnit].
     */
    fun fromMm(valueInMm: Double, targetUnit: LengthUnit): Double {
        return targetUnit.fromMm(valueInMm)
    }

    /**
     * Formats a double value with a specific number of decimal places, removing unnecessary trailing zeros.
     */
    fun formatDouble(value: Double, maxDecimals: Int = 2): String {
        val pattern = if (maxDecimals <= 0) "#,##0" else "#,##0." + "#".repeat(maxDecimals)
        val symbols = DecimalFormatSymbols(Locale.US)
        val df = DecimalFormat(pattern, symbols)
        return df.format(value)
    }

    /**
     * Formats a value with its unit symbol (e.g. "1,200 mm" or "48.5 in").
     */
    fun formatWithUnit(value: Double, unit: LengthUnit, maxDecimals: Int = 2): String {
        return "${formatDouble(value, maxDecimals)} ${unit.symbol}"
    }

    /**
     * Rounds a double to standard epsilon precision to avoid floating point inaccuracies.
     */
    fun roundToDecimals(value: Double, decimals: Int = 4): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10.0 }
        return kotlin.math.round(value * multiplier) / multiplier
    }
}
