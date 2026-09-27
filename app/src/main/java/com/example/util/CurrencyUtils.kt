package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyUtils {
    private val formatSymbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    private val numberFormat = DecimalFormat("#,###", formatSymbols)

    fun formatRupiah(amount: Long, withPrefix: Boolean = true): String {
        val formatted = numberFormat.format(amount)
        return if (withPrefix) "Rp $formatted" else formatted
    }

    fun parseAmount(text: String): Long {
        val clean = text.replace(Regex("[^0-9]"), "")
        return clean.toLongOrNull() ?: 0L
    }
}
