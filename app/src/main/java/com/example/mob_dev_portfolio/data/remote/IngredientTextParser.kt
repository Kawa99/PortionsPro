package com.example.mob_dev_portfolio.data.remote

data class ParsedIngredient(
    val quantity: Double,
    val unit: String,
    val name: String,
    val hasExplicitQuantity: Boolean
)

object IngredientTextParser {

    private val unicodeFractions = mapOf(
        "½" to "1/2",
        "¼" to "1/4",
        "¾" to "3/4",
        "⅓" to "1/3",
        "⅔" to "2/3",
        "⅛" to "1/8",
        "⅜" to "3/8",
        "⅝" to "5/8",
        "⅞" to "7/8"
    )

    private val knownUnits = listOf(
        "tablespoons", "tablespoon", "teaspoons", "teaspoon",
        "fluid ounces", "fluid ounce", "fl oz", "floz",
        "kilograms", "kilogram", "grams", "gram",
        "milliliters", "millilitres", "milliliter", "millilitre",
        "liters", "litres", "liter", "litre",
        "pounds", "pound", "ounces", "ounce",
        "cups", "cup", "tbsp", "tsp", "kg", "ml", "lb", "oz", "g", "l"
    )

    private val unitPattern = knownUnits
        .sortedByDescending { it.length }
        .joinToString("|") { Regex.escape(it) }

    private val amountPattern = """\d+(?:\.\d+)?(?:\s+\d+/\d+|/\d+)?|\d+/\d+"""

    private val leadingMeasureRegex = Regex(
        """^\s*($amountPattern)\s*($unitPattern)?(?=\s|/|,|-|$)""",
        RegexOption.IGNORE_CASE
    )

    private val alternateMeasureRegex = Regex(
        """^/\s*(?:$amountPattern)\s*(?:$unitPattern)?(?=\s|,|-|$)""",
        RegexOption.IGNORE_CASE
    )

    fun parse(raw: String): ParsedIngredient? {
        if (raw.isBlank()) return null

        var remaining = normaliseFractions(raw)
            .replace(Regex("""\s+"""), " ")
            .trim()

        val measureMatch = leadingMeasureRegex.find(remaining)
        if (measureMatch == null) {
            return ParsedIngredient(
                quantity = 0.0,
                unit = "",
                name = raw.trim(),
                hasExplicitQuantity = false
            )
        }

        val quantity = parseAmount(measureMatch.groupValues[1])
        val unit = measureMatch.groupValues.getOrNull(2).orEmpty()
        remaining = remaining.removePrefix(measureMatch.value).trim()
        remaining = alternateMeasureRegex.replaceFirst(remaining, "").trim()
        remaining = remaining.trimStart(',', '.', '-', ' ').trim()

        val name = remaining.ifBlank { raw.trim() }
        return ParsedIngredient(
            quantity = quantity.coerceAtLeast(0.0),
            unit = unit,
            name = name,
            hasExplicitQuantity = true
        )
    }

    private fun normaliseFractions(value: String): String {
        var result = value.replace(Regex("""(\d)([½¼¾⅓⅔⅛⅜⅝⅞])"""), "$1 $2")
        unicodeFractions.forEach { (char, replacement) ->
            result = result.replace(char, replacement)
        }
        return result
    }

    private fun parseAmount(value: String): Double {
        val parts = value.trim().split(Regex("""\s+"""))
        if (parts.size == 2 && "/" in parts[1]) {
            return (parts[0].toDoubleOrNull() ?: 0.0) + parseFraction(parts[1])
        }

        if ("/" in value) {
            val leadingNumber = value.substringBefore("/")
            val denominator = value.substringAfter("/").toDoubleOrNull()
            val numerator = leadingNumber.toDoubleOrNull()
            if (numerator != null && denominator != null && denominator != 0.0) {
                return numerator / denominator
            }
        }

        return value.toDoubleOrNull() ?: 0.0
    }

    private fun parseFraction(value: String): Double {
        val numerator = value.substringBefore("/").toDoubleOrNull()
        val denominator = value.substringAfter("/").toDoubleOrNull()
        return if (numerator != null && denominator != null && denominator != 0.0) {
            numerator / denominator
        } else {
            0.0
        }
    }
}
