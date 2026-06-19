package com.example.mob_dev_portfolio.util

enum class UnitDimension {
    WEIGHT,
    VOLUME,
    NEUTRAL
}

data class CanonicalUnit(
    val symbol: String,
    val dimension: UnitDimension,
    // Multiply quantity by this factor to convert to the dimension base unit.
    // Weight base: g. Volume base: ml. NEUTRAL factor is always 1.0.
    val toBaseFactor: Double,
    // true = metric, false = imperial, null = neither.
    val isMetric: Boolean?
) {
    companion object {
        val G = CanonicalUnit("g", UnitDimension.WEIGHT, 1.0, true)
        val KG = CanonicalUnit("kg", UnitDimension.WEIGHT, 1000.0, true)
        val OZ = CanonicalUnit("oz", UnitDimension.WEIGHT, 28.3495, false)
        val LB = CanonicalUnit("lb", UnitDimension.WEIGHT, 453.592, false)

        val ML = CanonicalUnit("ml", UnitDimension.VOLUME, 1.0, true)
        val FL_OZ = CanonicalUnit("fl oz", UnitDimension.VOLUME, 29.5735, false)

        val TSP = CanonicalUnit("tsp", UnitDimension.NEUTRAL, 1.0, null)
        val TBSP = CanonicalUnit("tbsp", UnitDimension.NEUTRAL, 1.0, null)
        val CUP = CanonicalUnit("cup", UnitDimension.NEUTRAL, 1.0, null)
        val L = CanonicalUnit("l", UnitDimension.VOLUME, 1000.0, true)
        val PC = CanonicalUnit("", UnitDimension.NEUTRAL, 1.0, null)
        val UNKNOWN = CanonicalUnit("", UnitDimension.NEUTRAL, 1.0, null)

        fun fromSymbol(symbol: String): CanonicalUnit {
            return when (symbol.trim().lowercase().replace(".", "")) {
                "", "piece", "pieces", "pc", "pcs", "count", "each" -> PC
                "g", "gram", "grams", "gramme", "grammes" -> G
                "kg", "kilogram", "kilograms", "kilo", "kilos" -> KG
                "oz", "ounce", "ounces" -> OZ
                "lb", "lbs", "pound", "pounds" -> LB
                "ml", "milliliter", "milliliters", "millilitre", "millilitres" -> ML
                "fl oz", "floz", "fluid ounce", "fluid ounces" -> FL_OZ
                "tsp", "teaspoon", "teaspoons" -> TSP
                "tbsp", "tablespoon", "tablespoons", "tbs", "tblsp" -> TBSP
                "cup", "cups" -> CUP
                "l", "liter", "liters", "litre", "litres" -> L
                else -> UNKNOWN
            }
        }
    }
}
