package com.example.pipecut.data.model

/**
 * Complete outcome of the pipe cutting optimization algorithm.
 */
data class OptimizationResult(
    val plans: List<PipeCuttingPlan>,
    val unfulfilledCuts: List<RequiredCut>,
    val totalStockPipesUsed: Int,
    val totalStockLengthConsumed: Double,
    val totalPiecesCut: Int,
    val totalPiecesRequested: Int,
    val totalUsefulLength: Double,
    val totalExtraAllowance: Double,
    val totalKerfLoss: Double,
    val totalTrimLoss: Double,
    val totalReusableOffcutLength: Double,
    val reusableOffcutsCount: Int,
    val totalScrapWasteLength: Double,
    val materialUtilizationPercentage: Double,
    val totalYieldPercentage: Double,
    val wastePercentage: Double,
    val unit: LengthUnit,
    val settings: CuttingSettings,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isFullySatisfied: Boolean get() = unfulfilledCuts.isEmpty()
    val hasUnfulfilled: Boolean get() = unfulfilledCuts.isNotEmpty()
}
