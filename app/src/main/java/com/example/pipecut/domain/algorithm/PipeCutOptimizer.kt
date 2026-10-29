package com.example.pipecut.domain.algorithm

import com.example.pipecut.data.model.CuttingSettings
import com.example.pipecut.data.model.LengthUnit
import com.example.pipecut.data.model.OptimizationResult
import com.example.pipecut.data.model.PipeCuttingPlan
import com.example.pipecut.data.model.PlacedCut
import com.example.pipecut.data.model.RequiredCut
import com.example.pipecut.data.model.StockPipe
import java.util.UUID
import kotlin.math.max
import kotlin.random.Random

/**
 * High-performance 1D Cutting Stock Optimizer.
 * Converts all calculations to Millimeters internally, evaluates multiple heuristic strategies
 * (Best-Fit Decreasing, First-Fit Decreasing, Best Stock Selection, 2-Opt Swaps),
 * and selects the cutting plan with maximum material utilization and minimum scrap.
 */
object PipeCutOptimizer {

    data class ValidationIssue(
        val message: String,
        val isFatal: Boolean,
        val problematicCut: RequiredCut? = null
    )

    /**
     * Validates inputs before running optimization.
     * Detects impossible cuts, negative/zero lengths, empty lists, etc.
     */
    fun validateInputs(
        stockPipes: List<StockPipe>,
        requiredCuts: List<RequiredCut>,
        settings: CuttingSettings
    ): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()

        if (stockPipes.isEmpty()) {
            issues.add(ValidationIssue("No stock pipes provided. Add at least one stock pipe.", isFatal = true))
            return issues
        }

        if (requiredCuts.isEmpty()) {
            issues.add(ValidationIssue("No required cuts provided. Add at least one cut piece.", isFatal = true))
            return issues
        }

        val leadTrimMm = settings.unit.toMm(settings.endTrim)
        val trailTrimMm = if (settings.trimBothEnds) leadTrimMm else 0.0
        val totalTrimMm = leadTrimMm + trailTrimMm
        val extraAllowanceMm = settings.unit.toMm(settings.extraAllowancePerPiece)
        val kerfMm = settings.unit.toMm(settings.sawKerf)

        // Find maximum usable length of any available stock pipe
        val maxStockUsableMm = stockPipes.maxOfOrNull {
            val stockLenMm = settings.unit.toMm(it.length)
            max(0.0, stockLenMm - totalTrimMm - kerfMm)
        } ?: 0.0

        for (cut in requiredCuts) {
            val cutLenMm = settings.unit.toMm(cut.length)
            val effectiveCutMm = cutLenMm + extraAllowanceMm

            if (cutLenMm <= 0.0) {
                issues.add(ValidationIssue("Cut '${cut.label.ifBlank { "Piece" }}' has an invalid length (<= 0).", isFatal = true, problematicCut = cut))
            }

            if (cut.quantity <= 0) {
                issues.add(ValidationIssue("Cut '${cut.label.ifBlank { "Piece" }}' has an invalid quantity (<= 0).", isFatal = true, problematicCut = cut))
            }

            if (effectiveCutMm > maxStockUsableMm) {
                val cutFormatted = settings.unit.format(cut.length)
                val maxUsableFormatted = settings.unit.format(settings.unit.fromMm(maxStockUsableMm))
                issues.add(
                    ValidationIssue(
                        message = "Piece '${cut.label.ifBlank { cutFormatted }}' requires $cutFormatted (plus allowance) which exceeds the maximum usable stock pipe length of $maxUsableFormatted.",
                        isFatal = true,
                        problematicCut = cut
                    )
                )
            }
        }

        // Check if total stock length could satisfy total cut demand
        val totalStockLengthMm = stockPipes.sumOf { settings.unit.toMm(it.length) * it.quantity }
        val totalCutLengthMm = requiredCuts.sumOf { (settings.unit.toMm(it.length) + extraAllowanceMm) * it.quantity }
        if (totalStockLengthMm < totalCutLengthMm) {
            issues.add(
                ValidationIssue(
                    message = "Total required cut length (${settings.unit.format(settings.unit.fromMm(totalCutLengthMm))}) exceeds total available stock length (${settings.unit.format(settings.unit.fromMm(totalStockLengthMm))}). Some pieces will not be fulfilled.",
                    isFatal = false
                )
            )
        }

        return issues
    }

    // Internal representation during optimization (all lengths in canonical Millimeters)
    private data class InternalPiece(
        val originalCut: RequiredCut,
        val lengthMm: Double,
        val extraAllowanceMm: Double,
        val effectiveLengthMm: Double,
        val uniqueId: String = UUID.randomUUID().toString()
    )

    private data class InternalStockDefinition(
        val originalStock: StockPipe,
        val lengthMm: Double,
        val availableQuantity: Int
    )

    private data class InternalBin(
        val stockDefinition: InternalStockDefinition,
        val capacityMm: Double, // Usable length: stockLength - leadTrim - trailTrim
        val leadTrimMm: Double,
        val trailTrimMm: Double,
        val kerfMm: Double,
        val pieces: MutableList<InternalPiece> = mutableListOf()
    ) {
        val totalPiecesLengthMm: Double get() = pieces.sumOf { it.lengthMm }
        val totalExtraAllowanceMm: Double get() = pieces.sumOf { it.extraAllowanceMm }
        val totalKerfLossMm: Double get() = pieces.size * kerfMm

        val totalUsedMm: Double
            get() = leadTrimMm + trailTrimMm + pieces.sumOf { it.effectiveLengthMm } + totalKerfLossMm

        val remainingSpaceMm: Double
            get() = (stockDefinition.lengthMm - totalUsedMm).coerceAtLeast(0.0)

        fun canFit(piece: InternalPiece): Boolean {
            // Adding this piece consumes piece.effectiveLengthMm + kerfMm
            val additionalNeeded = piece.effectiveLengthMm + kerfMm
            return remainingSpaceMm >= additionalNeeded - 1e-6
        }

        fun addPiece(piece: InternalPiece) {
            pieces.add(piece)
        }

        fun copyBin(): InternalBin {
            val copy = InternalBin(
                stockDefinition = stockDefinition,
                capacityMm = capacityMm,
                leadTrimMm = leadTrimMm,
                trailTrimMm = trailTrimMm,
                kerfMm = kerfMm,
                pieces = pieces.toMutableList()
            )
            return copy
        }
    }

    /**
     * Executes the cutting-stock optimization.
     */
    fun optimize(
        stockPipes: List<StockPipe>,
        requiredCuts: List<RequiredCut>,
        settings: CuttingSettings
    ): OptimizationResult {
        val unit = settings.unit
        val leadTrimMm = unit.toMm(settings.endTrim)
        val trailTrimMm = if (settings.trimBothEnds) leadTrimMm else 0.0
        val kerfMm = unit.toMm(settings.sawKerf)
        val extraAllowanceMm = unit.toMm(settings.extraAllowancePerPiece)
        val minReusableMm = unit.toMm(settings.minReusableLength)

        // Expand required cut pieces
        val allPieces = mutableListOf<InternalPiece>()
        for (cut in requiredCuts) {
            val pieceLenMm = unit.toMm(cut.length)
            val effMm = pieceLenMm + extraAllowanceMm
            repeat(cut.quantity) {
                allPieces.add(
                    InternalPiece(
                        originalCut = cut,
                        lengthMm = pieceLenMm,
                        extraAllowanceMm = extraAllowanceMm,
                        effectiveLengthMm = effMm
                    )
                )
            }
        }

        // Expand available stock definitions
        val stockDefinitions = stockPipes.map { stock ->
            InternalStockDefinition(
                originalStock = stock,
                lengthMm = unit.toMm(stock.length),
                availableQuantity = stock.quantity
            )
        }

        // Candidate solutions
        var bestSolutionBins = emptyList<InternalBin>()
        var bestUnplacedPieces = allPieces.toList()
        var bestScore = Double.NEGATIVE_INFINITY

        // Candidate heuristic passes:
        // 1. BFD with Longest Stock First
        // 2. BFD with Shortest Stock First
        // 3. BFD with Best Fit Stock
        // 4. FFD with Longest Stock First
        // 5. Multi-pass randomized greedy with local improvement
        val strategies = listOf(
            HeuristicStrategy.BEST_FIT_LONGEST_STOCK,
            HeuristicStrategy.BEST_FIT_SHORTEST_STOCK,
            HeuristicStrategy.BEST_FIT_TIGHTEST_STOCK,
            HeuristicStrategy.FIRST_FIT_LONGEST_STOCK
        )

        for (strat in strategies) {
            val (bins, unplaced) = runHeuristic(
                pieces = allPieces,
                stockDefs = stockDefinitions,
                leadTrimMm = leadTrimMm,
                trailTrimMm = trailTrimMm,
                kerfMm = kerfMm,
                strategy = strat,
                minReusableMm = minReusableMm
            )
            val improvedBins = optimizeSwaps(bins, kerfMm, minReusableMm)
            val score = evaluateSolution(improvedBins, unplaced, minReusableMm)
            if (score > bestScore) {
                bestScore = score
                bestSolutionBins = improvedBins
                bestUnplacedPieces = unplaced
            }
        }

        // Randomized multi-pass iterations for extra quality
        val random = Random(42)
        repeat(60) {
            val randomizedPieces = allPieces.shuffled(random).sortedByDescending { it.effectiveLengthMm * (0.95 + random.nextDouble() * 0.1) }
            val (bins, unplaced) = runHeuristicWithPieceOrder(
                pieces = randomizedPieces,
                stockDefs = stockDefinitions,
                leadTrimMm = leadTrimMm,
                trailTrimMm = trailTrimMm,
                kerfMm = kerfMm,
                strategy = HeuristicStrategy.BEST_FIT_TIGHTEST_STOCK
            )
            val improvedBins = optimizeSwaps(bins, kerfMm, minReusableMm)
            val score = evaluateSolution(improvedBins, unplaced, minReusableMm)
            if (score > bestScore) {
                bestScore = score
                bestSolutionBins = improvedBins
                bestUnplacedPieces = unplaced
            }
        }

        // Convert the best internal bins back to user-facing domain models (in project's LengthUnit)
        return buildOptimizationResult(
            bins = bestSolutionBins,
            unplaced = bestUnplacedPieces,
            stockPipes = stockPipes,
            requiredCuts = requiredCuts,
            settings = settings,
            minReusableMm = minReusableMm
        )
    }

    private enum class HeuristicStrategy {
        BEST_FIT_LONGEST_STOCK,
        BEST_FIT_SHORTEST_STOCK,
        BEST_FIT_TIGHTEST_STOCK,
        FIRST_FIT_LONGEST_STOCK
    }

    private fun runHeuristic(
        pieces: List<InternalPiece>,
        stockDefs: List<InternalStockDefinition>,
        leadTrimMm: Double,
        trailTrimMm: Double,
        kerfMm: Double,
        strategy: HeuristicStrategy,
        minReusableMm: Double
    ): Pair<List<InternalBin>, List<InternalPiece>> {
        // Sort pieces descending by effective length
        val sortedPieces = pieces.sortedByDescending { it.effectiveLengthMm }
        return runHeuristicWithPieceOrder(sortedPieces, stockDefs, leadTrimMm, trailTrimMm, kerfMm, strategy)
    }

    private fun runHeuristicWithPieceOrder(
        pieces: List<InternalPiece>,
        stockDefs: List<InternalStockDefinition>,
        leadTrimMm: Double,
        trailTrimMm: Double,
        kerfMm: Double,
        strategy: HeuristicStrategy
    ): Pair<List<InternalBin>, List<InternalPiece>> {
        val openBins = mutableListOf<InternalBin>()
        val unplaced = mutableListOf<InternalPiece>()

        // Track remaining quantity of each stock definition
        val remainingStockCount = stockDefs.associate { it.originalStock.id to it.availableQuantity }.toMutableMap()

        for (piece in pieces) {
            var placed = false

            // Try existing open bins
            when (strategy) {
                HeuristicStrategy.BEST_FIT_LONGEST_STOCK,
                HeuristicStrategy.BEST_FIT_SHORTEST_STOCK,
                HeuristicStrategy.BEST_FIT_TIGHTEST_STOCK -> {
                    // Best Fit: open bin with smallest remaining space that still fits the piece
                    val bestBin = openBins
                        .filter { it.canFit(piece) }
                        .minByOrNull { it.remainingSpaceMm - (piece.effectiveLengthMm + kerfMm) }

                    if (bestBin != null) {
                        bestBin.addPiece(piece)
                        placed = true
                    }
                }
                HeuristicStrategy.FIRST_FIT_LONGEST_STOCK -> {
                    val firstBin = openBins.firstOrNull { it.canFit(piece) }
                    if (firstBin != null) {
                        firstBin.addPiece(piece)
                        placed = true
                    }
                }
            }

            if (!placed) {
                // Must open a new stock pipe
                val candidateStock = selectStockToOpen(
                    stockDefs = stockDefs,
                    remainingCounts = remainingStockCount,
                    neededPieceMm = piece.effectiveLengthMm + kerfMm,
                    leadTrimMm = leadTrimMm,
                    trailTrimMm = trailTrimMm,
                    strategy = strategy
                )

                if (candidateStock != null) {
                    val currentCount = remainingStockCount[candidateStock.originalStock.id] ?: 0
                    remainingStockCount[candidateStock.originalStock.id] = currentCount - 1

                    val totalTrim = leadTrimMm + trailTrimMm
                    val capacity = max(0.0, candidateStock.lengthMm - totalTrim)
                    val newBin = InternalBin(
                        stockDefinition = candidateStock,
                        capacityMm = capacity,
                        leadTrimMm = leadTrimMm,
                        trailTrimMm = trailTrimMm,
                        kerfMm = kerfMm
                    )
                    newBin.addPiece(piece)
                    openBins.add(newBin)
                    placed = true
                }
            }

            if (!placed) {
                unplaced.add(piece)
            }
        }

        return Pair(openBins, unplaced)
    }

    private fun selectStockToOpen(
        stockDefs: List<InternalStockDefinition>,
        remainingCounts: Map<String, Int>,
        neededPieceMm: Double,
        leadTrimMm: Double,
        trailTrimMm: Double,
        strategy: HeuristicStrategy
    ): InternalStockDefinition? {
        val totalTrim = leadTrimMm + trailTrimMm
        val availableDefs = stockDefs.filter { (remainingCounts[it.originalStock.id] ?: 0) > 0 }
            .filter { (it.lengthMm - totalTrim) >= neededPieceMm - 1e-6 }

        if (availableDefs.isEmpty()) return null

        return when (strategy) {
            HeuristicStrategy.BEST_FIT_LONGEST_STOCK,
            HeuristicStrategy.FIRST_FIT_LONGEST_STOCK -> {
                availableDefs.maxByOrNull { it.lengthMm }
            }
            HeuristicStrategy.BEST_FIT_SHORTEST_STOCK -> {
                availableDefs.minByOrNull { it.lengthMm }
            }
            HeuristicStrategy.BEST_FIT_TIGHTEST_STOCK -> {
                // Smallest stock that fits this piece
                availableDefs.minByOrNull { it.lengthMm }
            }
        }
    }

    /**
     * Local Search / 2-Opt piece swapping:
     * Attempts to swap pieces between pairs of bins to consolidate remaining space into reusable offcuts
     * or eliminate unusable scrap drops.
     */
    private fun optimizeSwaps(bins: List<InternalBin>, kerfMm: Double, minReusableMm: Double): List<InternalBin> {
        if (bins.size <= 1) return bins
        val binCopies = bins.map { it.copyBin() }.toMutableList()

        var improved = true
        var passes = 0
        while (improved && passes < 3) {
            improved = false
            passes++

            for (i in 0 until binCopies.size) {
                for (j in i + 1 until binCopies.size) {
                    val binA = binCopies[i]
                    val binB = binCopies[j]

                    // Try swapping piece a from binA with piece b from binB
                    for (aIdx in binA.pieces.indices) {
                        for (bIdx in binB.pieces.indices) {
                            val pieceA = binA.pieces[aIdx]
                            val pieceB = binB.pieces[bIdx]

                            val diff = pieceA.effectiveLengthMm - pieceB.effectiveLengthMm
                            if (kotlin.math.abs(diff) < 1e-4) continue

                            // If pieceA goes to B and pieceB goes to A:
                            // binA new remaining = remainingA + diff
                            // binB new remaining = remainingB - diff
                            val newRemA = binA.remainingSpaceMm + diff
                            val newRemB = binB.remainingSpaceMm - diff

                            if (newRemA >= -1e-6 && newRemB >= -1e-6) {
                                // Both can legally accommodate the swap
                                val oldScrapA = if (binA.remainingSpaceMm < minReusableMm) binA.remainingSpaceMm else 0.0
                                val oldScrapB = if (binB.remainingSpaceMm < minReusableMm) binB.remainingSpaceMm else 0.0
                                val newScrapA = if (newRemA < minReusableMm) newRemA else 0.0
                                val newScrapB = if (newRemB < minReusableMm) newRemB else 0.0

                                if ((newScrapA + newScrapB) < (oldScrapA + oldScrapB) - 1.0) {
                                    // Beneficial swap: reduces scrap
                                    binA.pieces.removeAt(aIdx)
                                    binA.pieces.add(aIdx, pieceB)
                                    binB.pieces.removeAt(bIdx)
                                    binB.pieces.add(bIdx, pieceA)
                                    improved = true
                                    break
                                }
                            }
                        }
                        if (improved) break
                    }
                    if (improved) break
                }
            }
        }

        return binCopies
    }

    /**
     * Evaluation scoring function.
     * Higher score is better.
     * Heavily penalizes unplaced pieces.
     * Rewards reusable offcuts and penalizes scrap waste.
     */
    private fun evaluateSolution(
        bins: List<InternalBin>,
        unplaced: List<InternalPiece>,
        minReusableMm: Double
    ): Double {
        if (unplaced.isNotEmpty()) {
            // Severe penalty for each unplaced piece
            return -1_000_000.0 * unplaced.size - unplaced.sumOf { it.lengthMm }
        }

        var score = 100_000.0 // Base score for 100% fulfillment

        // Penalty for number of pipes used (favor fewer pipes)
        score -= bins.size * 1_000.0

        for (bin in bins) {
            val rem = bin.remainingSpaceMm
            if (rem >= minReusableMm) {
                // Reusable offcut: small reward for larger reusable piece
                score += rem * 0.5
            } else {
                // Scrap waste: penalty proportional to wasted length
                score -= rem * 2.0
            }
        }

        return score
    }

    /**
     * Maps internal bins into complete [OptimizationResult] in the requested [LengthUnit].
     */
    private fun buildOptimizationResult(
        bins: List<InternalBin>,
        unplaced: List<InternalPiece>,
        stockPipes: List<StockPipe>,
        requiredCuts: List<RequiredCut>,
        settings: CuttingSettings,
        minReusableMm: Double
    ): OptimizationResult {
        val unit = settings.unit

        val plans = bins.mapIndexed { index, bin ->
            var currentPosMm = bin.leadTrimMm
            val placedCuts = mutableListOf<PlacedCut>()

            for ((seqIdx, piece) in bin.pieces.withIndex()) {
                val startMm = currentPosMm
                val endMm = startMm + piece.effectiveLengthMm
                val kerfAfterMm = bin.kerfMm
                placedCuts.add(
                    PlacedCut(
                        pieceId = piece.originalCut.id,
                        label = piece.originalCut.label.ifBlank { "Piece ${seqIdx + 1}" },
                        length = unit.fromMm(piece.lengthMm),
                        extraAllowance = unit.fromMm(piece.extraAllowanceMm),
                        effectiveLength = unit.fromMm(piece.effectiveLengthMm),
                        startPosition = unit.fromMm(startMm),
                        endPosition = unit.fromMm(endMm),
                        kerfAfter = unit.fromMm(kerfAfterMm),
                        sequenceNumber = seqIdx + 1,
                        colorHex = piece.originalCut.colorHex
                    )
                )
                currentPosMm = endMm + kerfAfterMm
            }

            val leftoverMm = bin.remainingSpaceMm
            val isReusable = leftoverMm >= minReusableMm - 1e-4

            PipeCuttingPlan(
                pipeIndex = index + 1,
                stockPipeId = bin.stockDefinition.originalStock.id,
                stockLength = unit.fromMm(bin.stockDefinition.lengthMm),
                stockLabel = bin.stockDefinition.originalStock.label.ifBlank { "Stock Pipe ${index + 1}" },
                leadTrim = unit.fromMm(bin.leadTrimMm),
                trailTrim = unit.fromMm(bin.trailTrimMm),
                cuts = placedCuts,
                totalPiecesLength = unit.fromMm(bin.totalPiecesLengthMm),
                totalExtraAllowance = unit.fromMm(bin.totalExtraAllowanceMm),
                totalKerfLoss = unit.fromMm(bin.totalKerfLossMm),
                leftoverLength = unit.fromMm(leftoverMm),
                isReusableOffcut = isReusable,
                minReusableThreshold = unit.fromMm(minReusableMm)
            )
        }

        // Aggregate metrics
        val totalStockLengthConsumed = plans.sumOf { it.stockLength }
        val totalPiecesCut = plans.sumOf { it.cuts.size }
        val totalPiecesRequested = requiredCuts.sumOf { it.quantity }
        val totalUsefulLength = plans.sumOf { it.totalPiecesLength }
        val totalExtraAllowance = plans.sumOf { it.totalExtraAllowance }
        val totalKerfLoss = plans.sumOf { it.totalKerfLoss }
        val totalTrimLoss = plans.sumOf { it.totalTrimLoss }

        val reusablePlans = plans.filter { it.isReusableOffcut }
        val totalReusableOffcutLength = reusablePlans.sumOf { it.leftoverLength }
        val reusableOffcutsCount = reusablePlans.size

        val totalScrapWasteLength = plans.filter { !it.isReusableOffcut }.sumOf { it.leftoverLength } + totalKerfLoss + totalTrimLoss

        val materialUtilizationPercentage = if (totalStockLengthConsumed > 0) {
            (totalUsefulLength / totalStockLengthConsumed * 100.0).coerceIn(0.0, 100.0)
        } else 0.0

        val totalYieldPercentage = if (totalStockLengthConsumed > 0) {
            ((totalUsefulLength + totalReusableOffcutLength) / totalStockLengthConsumed * 100.0).coerceIn(0.0, 100.0)
        } else 0.0

        val wastePercentage = if (totalStockLengthConsumed > 0) {
            (totalScrapWasteLength / totalStockLengthConsumed * 100.0).coerceIn(0.0, 100.0)
        } else 0.0

        // Group unplaced pieces back into RequiredCut summary
        val unplacedMap = mutableMapOf<String, Int>()
        for (up in unplaced) {
            unplacedMap[up.originalCut.id] = (unplacedMap[up.originalCut.id] ?: 0) + 1
        }
        val unfulfilledCuts = requiredCuts.mapNotNull { orig ->
            val count = unplacedMap[orig.id] ?: 0
            if (count > 0) orig.copy(quantity = count) else null
        }

        return OptimizationResult(
            plans = plans,
            unfulfilledCuts = unfulfilledCuts,
            totalStockPipesUsed = plans.size,
            totalStockLengthConsumed = totalStockLengthConsumed,
            totalPiecesCut = totalPiecesCut,
            totalPiecesRequested = totalPiecesRequested,
            totalUsefulLength = totalUsefulLength,
            totalExtraAllowance = totalExtraAllowance,
            totalKerfLoss = totalKerfLoss,
            totalTrimLoss = totalTrimLoss,
            totalReusableOffcutLength = totalReusableOffcutLength,
            reusableOffcutsCount = reusableOffcutsCount,
            totalScrapWasteLength = totalScrapWasteLength,
            materialUtilizationPercentage = materialUtilizationPercentage,
            totalYieldPercentage = totalYieldPercentage,
            wastePercentage = wastePercentage,
            unit = unit,
            settings = settings
        )
    }
}
