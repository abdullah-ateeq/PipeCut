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

class WasteCalculationTest {

    @Test
    fun testReusableOffcutThreshold() {
        val settings = CuttingSettings(
            sawKerf = 0.0,
            endTrim = 0.0,
            minReusableLength = 300.0, // Drops >= 300 mm are reusable
            unit = LengthUnit.MM
        )

        // Case A: Pipe of 1000 mm, cut 600 mm -> leftover 400 mm (>= 300 mm => Reusable)
        val stock = listOf(StockPipe(length = 1000.0, quantity = 1))
        val cutsA = listOf(RequiredCut(length = 600.0, quantity = 1))

        val resultA = PipeCutOptimizer.optimize(stock, cutsA, settings)
        assertEquals(1, resultA.plans.size)
        assertTrue(resultA.plans[0].isReusableOffcut)
        assertEquals(400.0, resultA.totalReusableOffcutLength, 0.0001)
        assertEquals(0.0, resultA.totalScrapWasteLength, 0.0001)

        // Case B: Pipe of 1000 mm, cut 800 mm -> leftover 200 mm (< 300 mm => Scrap)
        val cutsB = listOf(RequiredCut(length = 800.0, quantity = 1))
        val resultB = PipeCutOptimizer.optimize(stock, cutsB, settings)
        assertFalse(resultB.plans[0].isReusableOffcut)
        assertEquals(0.0, resultB.totalReusableOffcutLength, 0.0001)
        assertEquals(200.0, resultB.totalScrapWasteLength, 0.0001)
    }

    @Test
    fun testMaterialUtilizationPercentage() {
        val settings = CuttingSettings(
            sawKerf = 0.0,
            endTrim = 0.0,
            unit = LengthUnit.MM
        )

        // 1 pipe of 1000 mm, cut 1 piece of 800 mm
        val stock = listOf(StockPipe(length = 1000.0, quantity = 1))
        val cuts = listOf(RequiredCut(length = 800.0, quantity = 1))

        val result = PipeCutOptimizer.optimize(stock, cuts, settings)

        // Utilization: 800 / 1000 = 80.0%
        assertEquals(80.0, result.materialUtilizationPercentage, 0.01)
        assertEquals(80.0, result.plans[0].utilizationPercentage, 0.01)
    }
}
