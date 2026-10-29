package com.example.pipecut

import com.example.pipecut.data.model.CuttingSettings
import com.example.pipecut.data.model.LengthUnit
import com.example.pipecut.data.model.RequiredCut
import com.example.pipecut.data.model.StockPipe
import com.example.pipecut.domain.algorithm.PipeCutOptimizer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImpossibleCutTest {

    @Test
    fun testDetectsCutLongerThanStockPipe() {
        val stock = listOf(StockPipe(length = 2000.0, quantity = 5))
        // Cut is 2500 mm (exceeds stock length 2000)
        val cuts = listOf(RequiredCut(length = 2500.0, quantity = 1))

        val settings = CuttingSettings(
            sawKerf = 3.0,
            endTrim = 10.0,
            unit = LengthUnit.MM
        )

        val issues = PipeCutOptimizer.validateInputs(stock, cuts, settings)
        val fatal = issues.filter { it.isFatal }

        assertFalse(fatal.isEmpty())
        assertTrue(fatal.any { it.message.contains("exceeds the maximum usable stock pipe length") })
    }

    @Test
    fun testDetectsCutExceedingUsableLengthAfterTrimAndKerf() {
        val stock = listOf(StockPipe(length = 1000.0, quantity = 1))
        // Cut is 995 mm. Stock is 1000, but endTrim is 10 mm and kerf is 3 mm.
        // Max usable length = 1000 - 10 - 3 = 987 mm. 995 mm will not fit!
        val cuts = listOf(RequiredCut(length = 995.0, quantity = 1))

        val settings = CuttingSettings(
            sawKerf = 3.0,
            endTrim = 10.0,
            unit = LengthUnit.MM
        )

        val issues = PipeCutOptimizer.validateInputs(stock, cuts, settings)
        val fatal = issues.filter { it.isFatal }

        assertFalse(fatal.isEmpty())
        assertTrue(fatal.any { it.message.contains("exceeds the maximum usable stock pipe length") })
    }

    @Test
    fun testValidInputsPassValidation() {
        val stock = listOf(StockPipe(length = 6000.0, quantity = 2))
        val cuts = listOf(
            RequiredCut(length = 2000.0, quantity = 3),
            RequiredCut(length = 1500.0, quantity = 2)
        )

        val settings = CuttingSettings(
            sawKerf = 3.0,
            endTrim = 15.0,
            unit = LengthUnit.MM
        )

        val issues = PipeCutOptimizer.validateInputs(stock, cuts, settings)
        val fatal = issues.filter { it.isFatal }

        assertTrue(fatal.isEmpty())
    }
}
