package com.example.mob_dev_portfolio.util

import org.junit.Assert.assertEquals
import org.junit.Test

class QuantityFormatterTest {
    @Test
    fun formatsWholeNumberWithMetricUnit() {
        assertEquals("500 g", QuantityFormatter.format(500.0, CanonicalUnit.G))
    }

    @Test
    fun roundsLongDecimalToTwoPlaces() {
        assertEquals("17.64 oz", QuantityFormatter.format(17.6369, CanonicalUnit.OZ))
    }

    @Test
    fun formatsNeutralUnit() {
        assertEquals("2 tsp", QuantityFormatter.format(2.0, CanonicalUnit.TSP))
    }

    @Test
    fun trimsTrailingZero() {
        assertEquals("1.5 kg", QuantityFormatter.format(1.5, CanonicalUnit.KG))
    }

    @Test
    fun omitsBlankPieceSymbol() {
        assertEquals("3", QuantityFormatter.format(3.0, CanonicalUnit.PC))
    }

    @Test
    fun formatsRepeatingDecimalToTwoPlaces() {
        assertEquals("0.33 g", QuantityFormatter.format(0.333, CanonicalUnit.G))
    }
}
