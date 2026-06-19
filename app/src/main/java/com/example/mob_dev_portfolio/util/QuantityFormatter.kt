package com.example.mob_dev_portfolio.util

import java.util.Locale

object QuantityFormatter {
    fun format(quantity: Double, unit: CanonicalUnit): String {
        val formattedNumber = String.format(Locale.US, "%.2f", quantity)
            .trimEnd('0')
            .trimEnd('.')

        return if (unit.symbol.isBlank()) {
            formattedNumber
        } else {
            "$formattedNumber ${unit.symbol}"
        }
    }
}
