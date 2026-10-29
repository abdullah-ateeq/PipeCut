package com.example.pipecut

import com.example.pipecut.data.model.CuttingSettings
import com.example.pipecut.data.model.LengthUnit
import com.example.pipecut.data.model.RequiredCut
import com.example.pipecut.data.model.StockPipe
import com.example.pipecut.domain.algorithm.PipeCutOptimizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StockLimitTest {

    @Test
    fun testStockPipeLimitStrictlyRespected() {
        // Only 1 stock pipe available of length 1000
        val stock = listOf(StockPipe(length = 1000.0, quantity = 1))
        // We request 3 pieces of 400 (400*3 + kerf > 1000, only 2 can fit)
        val cuts = listOf(RequiredCut(length = 400.0, quantity = 3))

        val settings = CuttingSettings(
            sawKerf = 3.0,
            endTrim = 10.0,
            unit = LengthUnit.MM
        )

        val result = PipeCutOptimizer.optimize(stock, cuts, settings)

        // Only 1 stock pipe should be used
        assertEquals(1, result.totalStockPipesUsed)
        assertEquals(1, result.plans.size)

        // Only 2 pieces should be cut
        assertEquals(2, result.totalPiecesCut)

        // Exactly 1 piece should be reported as unfulfilled
        assertTrue(result.hasUnfulfilled)
        assertEquals(1, result.unfulfilledCuts.size)
        assertEquals(1, result.unfulfilledCuts[0].quantity)
        assertEquals(400.0, result.unfulfilledCuts[0].length, 0.0001)
    }

    @Test
    fun testMultipleStockTypesPackEfficiently() {
        // Stock 1: 3000 mm (qty 1)
        // Stock 2: 6000 mm (qty 1)
        val stock = listOf(
            StockPipe(length = 3000.0, quantity = 1, label = "3m Pipe"),
            StockPipe(length = 6000.0, quantity = 1, label = "6m Pipe")
        )

        // Cuts: 2 pieces of 2500 mm (one fits in 3m, one fits in 6m, or both fit in 6m)
        val cuts = listOf(RequiredCut(length = 2500.0, quantity = 2))

        val settings = CuttingSettings(
            sawKerf = 3.0,
            endTrim = 10.0,
            unit = LengthUnit.MM
        )

        val result = PipeCutOptimizer.optimize(stock, cuts, settings)

        // All 2 pieces should be cut successfully
        assertTrue(result.isFullySatisfied)
        assertEquals(2, result.totalPiecesCut)
    }
}
