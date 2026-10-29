package com.example.pipecut.data.model

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * Settings for cutting parameters.
 * Numerical values are stored in the current [unit].
 */
data class CuttingSettings(
    val sawKerf: Double = 3.0,                  // 3.0 mm (or 0.125 in)
    val endTrim: Double = 10.0,                 // 10.0 mm (lead trim to clean pipe end)
    val trimBothEnds: Boolean = false,          // Whether to trim both ends of each stock pipe
    val extraAllowancePerPiece: Double = 0.0,   // Facing or safety margin added to each cut
    val minReusableLength: Double = 300.0,      // Minimum offcut length considered reusable stock
    val unit: LengthUnit = LengthUnit.MM,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM
) {
    fun totalTrimPerPipe(): Double {
        return if (trimBothEnds) endTrim * 2.0 else endTrim
    }

    /**
     * Converts numerical settings to a target unit so values match when the user switches units.
     */
    fun convertTo(targetUnit: LengthUnit): CuttingSettings {
        if (targetUnit == unit) return this
        val toTarget = { v: Double ->
            val inMm = unit.toMm(v)
            targetUnit.fromMm(inMm)
        }
        return copy(
            sawKerf = toTarget(sawKerf),
            endTrim = toTarget(endTrim),
            extraAllowancePerPiece = toTarget(extraAllowancePerPiece),
            minReusableLength = toTarget(minReusableLength),
            unit = targetUnit
        )
    }
}
