package com.example.pipecut.data.model

/**
 * Detailed cutting instructions and breakdown for an individual stock pipe.
 */
data class PipeCuttingPlan(
    val pipeIndex: Int,
    val stockPipeId: String,
    val stockLength: Double,
    val stockLabel: String,
    val leadTrim: Double,
    val trailTrim: Double,
    val cuts: List<PlacedCut>,
    val totalPiecesLength: Double,
    val totalExtraAllowance: Double,
    val totalKerfLoss: Double,
    val leftoverLength: Double,
    val isReusableOffcut: Boolean,
    val minReusableThreshold: Double
) {
    val totalTrimLoss: Double get() = leadTrim + trailTrim
    val totalConsumed: Double get() = totalTrimLoss + totalPiecesLength + totalExtraAllowance + totalKerfLoss
    val usableLength: Double get() = (stockLength - totalTrimLoss).coerceAtLeast(0.0)
    
    val utilizationPercentage: Double
        get() = if (stockLength > 0) (totalPiecesLength / stockLength * 100.0).coerceIn(0.0, 100.0) else 0.0

    val yieldPercentage: Double
        get() {
            val usefulPlusReusable = totalPiecesLength + if (isReusableOffcut) leftoverLength else 0.0
            return if (stockLength > 0) (usefulPlusReusable / stockLength * 100.0).coerceIn(0.0, 100.0) else 0.0
        }
}
