package com.example.mob_dev_portfolio.util

object UnitConverter {
    fun convert(
        quantity: Double,
        from: CanonicalUnit,
        useMetric: Boolean
    ): Pair<Double, CanonicalUnit> {
        if (from.dimension == UnitDimension.NEUTRAL || from.isMetric == null) {
            return quantity to from
        }

        val inBase = quantity * from.toBaseFactor
        val target = when {
            !useMetric && from.dimension == UnitDimension.WEIGHT -> {
                if (inBase >= CanonicalUnit.KG.toBaseFactor) CanonicalUnit.LB else CanonicalUnit.OZ
            }
            !useMetric && from.dimension == UnitDimension.VOLUME -> CanonicalUnit.FL_OZ
            useMetric && from.dimension == UnitDimension.WEIGHT -> {
                if (inBase >= CanonicalUnit.KG.toBaseFactor) CanonicalUnit.KG else CanonicalUnit.G
            }
            useMetric && from.dimension == UnitDimension.VOLUME -> CanonicalUnit.ML
            else -> from
        }

        return (inBase / target.toBaseFactor) to target
    }
}
