package com.example.pipecut

import com.example.pipecut.data.model.LengthUnit
import com.example.pipecut.domain.converter.UnitConverter
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConversionTest {

    @Test
    fun testMmToCmAndBack() {
        val mm = 2500.0
        val cm = UnitConverter.convert(mm, LengthUnit.MM, LengthUnit.CM)
        assertEquals(250.0, cm, 0.0001)

        val backToMm = UnitConverter.convert(cm, LengthUnit.CM, LengthUnit.MM)
        assertEquals(2500.0, backToMm, 0.0001)
    }

    @Test
    fun testInchesToMm() {
        val inches = 10.0
        val mm = UnitConverter.convert(inches, LengthUnit.INCH, LengthUnit.MM)
        assertEquals(254.0, mm, 0.0001)
    }

    @Test
    fun testFeetToInches() {
        val feet = 20.0
        val mm = LengthUnit.FT.toMm(feet)
        val inches = LengthUnit.INCH.fromMm(mm)
        assertEquals(240.0, inches, 0.001)
    }

    @Test
    fun testRoundTripConversion() {
        val originalFt = 21.0
        val inMm = UnitConverter.convert(originalFt, LengthUnit.FT, LengthUnit.MM)
        val inInches = UnitConverter.convert(inMm, LengthUnit.MM, LengthUnit.INCH)
        val backToFt = UnitConverter.convert(inInches, LengthUnit.INCH, LengthUnit.FT)
        assertEquals(originalFt, backToFt, 0.0001)
    }

    @Test
    fun testFormatting() {
        assertEquals("1,250 mm", LengthUnit.MM.format(1250.0))
        assertEquals("48.5 in", LengthUnit.INCH.format(48.5))
    }
}
