package com.example.mob_dev_portfolio.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConverterTest {
    @Test
    fun convertsGramsToOuncesInImperial() {
        val (quantity, unit) = UnitConverter.convert(500.0, CanonicalUnit.G, useMetric = false)

        assertEquals(17.64, quantity, 0.01)
        assertEquals(CanonicalUnit.OZ, unit)
    }

    @Test
    fun convertsKilogramsToPoundsInImperial() {
        val (quantity, unit) = UnitConverter.convert(1.0, CanonicalUnit.KG, useMetric = false)

        assertEquals(2.20, quantity, 0.01)
        assertEquals(CanonicalUnit.LB, unit)
    }

    @Test
    fun convertsOuncesToGramsInMetric() {
        val (quantity, unit) = UnitConverter.convert(16.0, CanonicalUnit.OZ, useMetric = true)

        assertEquals(453.59, quantity, 0.01)
        assertEquals(CanonicalUnit.G, unit)
    }

    @Test
    fun convertsMillilitersToFluidOuncesInImperial() {
        val (quantity, unit) = UnitConverter.convert(240.0, CanonicalUnit.ML, useMetric = false)

        assertEquals(8.11, quantity, 0.01)
        assertEquals(CanonicalUnit.FL_OZ, unit)
    }

    @Test
    fun convertsLitersToFluidOuncesInImperial() {
        val (quantity, unit) = UnitConverter.convert(1.0, CanonicalUnit.L, useMetric = false)

        assertEquals(33.81, quantity, 0.01)
        assertEquals(CanonicalUnit.FL_OZ, unit)
    }

    @Test
    fun parsesLitersAsMetricVolume() {
        val unit = CanonicalUnit.fromSymbol("litres")

        assertEquals(CanonicalUnit.L, unit)
        assertEquals(UnitDimension.VOLUME, unit.dimension)
        assertEquals(true, unit.isMetric)
        assertEquals(1000.0, unit.toBaseFactor, 0.0)
    }

    @Test
    fun convertsLargeGramsToKilogramsInMetric() {
        val (quantity, unit) = UnitConverter.convert(1500.0, CanonicalUnit.G, useMetric = true)

        assertEquals(1.5, quantity, 0.01)
        assertEquals(CanonicalUnit.KG, unit)
    }

    @Test
    fun leavesTeaspoonsUnchangedInImperial() {
        val (quantity, unit) = UnitConverter.convert(2.0, CanonicalUnit.TSP, useMetric = false)

        assertEquals(2.0, quantity, 0.0)
        assertEquals(CanonicalUnit.TSP, unit)
    }

    @Test
    fun leavesCupsUnchangedInMetric() {
        val (quantity, unit) = UnitConverter.convert(1.0, CanonicalUnit.CUP, useMetric = true)

        assertEquals(1.0, quantity, 0.0)
        assertEquals(CanonicalUnit.CUP, unit)
    }

    @Test
    fun leavesUnknownUnitsUnchanged() {
        val (quantity, unit) = UnitConverter.convert(3.0, CanonicalUnit.UNKNOWN, useMetric = false)

        assertEquals(3.0, quantity, 0.0)
        assertEquals(CanonicalUnit.UNKNOWN, unit)
    }
}
