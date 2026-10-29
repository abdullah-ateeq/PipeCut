package com.example.pipecut

import com.example.pipecut.data.model.CuttingSettings
import com.example.pipecut.data.model.LengthUnit
import com.example.pipecut.data.model.RequiredCut
import com.example.pipecut.data.model.StockPipe
import com.example.pipecut.domain.algorithm.PipeCutOptimizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KerfCalculationTest {

    @Test
    fun testKerfAndTrimDeductions() {
        val stockLength = 1000.0
        val kerf = 3.0
        val leadTrim = 10.0
        val cutLength = 400.0

        val settings = CuttingSettings(
            sawKerf = kerf,
            endTrim = leadTrim,
            trimBothEnds = false,
            extraAllowancePerPiece = 0.0,
            minReusableLength = 50.0,
            unit = LengthUnit.MM
        )

        // 2 cuts of 400:
        // Consumed = leadTrim (10) + cut1 (400) + kerf1 (3) + cut2 (400) + kerf2 (3) = 816.
        // Leftover = 1000 - 816 = 184.
        val stock = listOf(StockPipe(length = stockLength, quantity = 1))
        val cuts = listOf(RequiredCut(length = cutLength, quantity = 2))

        val result = PipeCutOptimizer.optimize(stock, cuts, settings)

        assertEquals(1, result.plans.size)
        val plan = result.plans[0]

        assertEquals(2, plan.cuts.size)
        assertEquals(leadTrim, plan.leadTrim, 0.0001)
        assertEquals(2 * kerf, plan.totalKerfLoss, 0.0001)
        assertEquals(2 * cutLength, plan.totalPiecesLength, 0.0001)
        assertEquals(184.0, plan.leftoverLength, 0.0001)

        // Verify cutting sequence positions
        val cut1 = plan.cuts[0]
        assertEquals(10.0, cut1.startPosition, 0.0001)
        assertEquals(410.0, cut1.endPosition, 0.0001)
        assertEquals(3.0, cut1.kerfAfter, 0.0001)

        val cut2 = plan.cuts[1]
        assertEquals(413.0, cut2.startPosition, 0.0001)
        assertEquals(813.0, cut2.endPosition, 0.0001)
        assertEquals(3.0, cut2.kerfAfter, 0.0001)
    }

    @Test
    fun testTrimBothEnds() {
        val settings = CuttingSettings(
            sawKerf = 2.0,
            endTrim = 10.0,
            trimBothEnds = true,
            unit = LengthUnit.MM
        )

        val stock = listOf(StockPipe(length = 1000.0, quantity = 1))
        val cuts = listOf(RequiredCut(length = 500.0, quantity = 1))

        val result = PipeCutOptimizer.optimize(stock, cuts, settings)
        val plan = result.plans[0]

        assertEquals(10.0, plan.leadTrim, 0.0001)
        assertEquals(10.0, plan.trailTrim, 0.0001)
        assertEquals(20.0, plan.totalTrimLoss, 0.0001)
        // Consumed: 10 + 500 + 2 + 10 = 522. Leftover = 1000 - 522 = 478
        assertEquals(478.0, plan.leftoverLength, 0.0001)
    }

    @Test
    fun testExtraPieceAllowance() {
        val settings = CuttingSettings(
            sawKerf = 3.0,
            endTrim = 0.0,
            extraAllowancePerPiece = 5.0, // 5 mm extra per piece
            unit = LengthUnit.MM
        )

        val stock = listOf(StockPipe(length = 1000.0, quantity = 1))
        val cuts = listOf(RequiredCut(length = 200.0, quantity = 2))

        val result = PipeCutOptimizer.optimize(stock, cuts, settings)
        val plan = result.plans[0]

        // Each piece is effectively 205 + 3 = 208
        // Total pieces = 400 useful + 10 allowance + 6 kerf = 416 consumed
        assertEquals(400.0, plan.totalPiecesLength, 0.0001)
        assertEquals(10.0, plan.totalExtraAllowance, 0.0001)
        assertEquals(584.0, plan.leftoverLength, 0.0001)
    }
}
