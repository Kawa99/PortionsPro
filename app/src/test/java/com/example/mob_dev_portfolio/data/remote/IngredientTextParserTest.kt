package com.example.mob_dev_portfolio.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IngredientTextParserTest {

    @Test
    fun parseHandlesCompactMetricAndImperialAlternative() {
        val parsed = IngredientTextParser.parse("150g/5 1/2oz mature cheddar, grated")!!

        assertEquals(150.0, parsed.quantity, 0.001)
        assertEquals("g", parsed.unit)
        assertEquals("mature cheddar, grated", parsed.name)
        assertTrue(parsed.hasExplicitQuantity)
    }

    @Test
    fun parseHandlesCompactVolumeAndFluidOunceAlternative() {
        val parsed = IngredientTextParser.parse("100ml/3 1/2fl oz double cream")!!

        assertEquals(100.0, parsed.quantity, 0.001)
        assertEquals("ml", parsed.unit)
        assertEquals("double cream", parsed.name)
    }

    @Test
    fun parseHandlesUnicodeMixedFractions() {
        val parsed = IngredientTextParser.parse("1½ cups milk")!!

        assertEquals(1.5, parsed.quantity, 0.001)
        assertEquals("cups", parsed.unit)
        assertEquals("milk", parsed.name)
    }

    @Test
    fun parseKeepsCountDescriptionsWhenNoKnownUnitExists() {
        val parsed = IngredientTextParser.parse("6 slices thick-cut ham")!!

        assertEquals(6.0, parsed.quantity, 0.001)
        assertEquals("", parsed.unit)
        assertEquals("slices thick-cut ham", parsed.name)
    }

    @Test
    fun parseMarksUnquantifiedIngredients() {
        val parsed = IngredientTextParser.parse("butter, for greasing")!!

        assertEquals(0.0, parsed.quantity, 0.001)
        assertEquals("", parsed.unit)
        assertEquals("butter, for greasing", parsed.name)
        assertFalse(parsed.hasExplicitQuantity)
    }
}
