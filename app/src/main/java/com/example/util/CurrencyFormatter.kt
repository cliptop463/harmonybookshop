package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Formats prices, signed profits, and profit margin percentages according to [AppSettings].
 */
object CurrencyFormatter {

    fun formatPrice(
        amount: Double,
        currencySymbol: String,
        decimalPlaces: Int,
    ): String {
        val decimals = decimalPlaces.coerceIn(0, 4)
        val safeSymbol = currencySymbol.trim().ifEmpty { "$" }
        val normalized = normalizeZero(amount, decimals)
        val isNegative = normalized < 0.0
        val formattedNumber = formatNumber(abs(normalized), decimals)

        val space = if (safeSymbol.length > 1) " " else ""
        val sign = if (isNegative) "-" else ""
        return "$sign$safeSymbol$space$formattedNumber"
    }

    fun formatSignedProfit(
        profit: Double,
        currencySymbol: String,
        decimalPlaces: Int,
    ): String {
        val decimals = decimalPlaces.coerceIn(0, 4)
        val normalized = normalizeZero(profit, decimals)
        val base = formatPrice(abs(normalized), currencySymbol, decimals)
        return when {
            normalized > 0.0 -> "+$base"
            normalized < 0.0 -> "-$base"
            else -> base
        }
    }

    fun formatMarginPercent(margin: Double): String {
        val normalized = if (abs(margin) < 0.05) 0.0 else margin
        val formatted = formatNumber(abs(normalized), decimals = 1)
        return when {
            normalized > 0.0 -> "+$formatted%"
            normalized < 0.0 -> "-$formatted%"
            else -> "$formatted%"
        }
    }

    /** Formats editable numeric fields cleanly without thousands separators. */
    fun formatEditableNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            BigDecimalTrim.trim(value)
        }
    }

    private fun normalizeZero(value: Double, decimals: Int): Double {
        if (!value.isFinite()) return 0.0
        val threshold = 0.5 / 10.0.pow(decimals)
        return if (abs(value) < threshold) 0.0 else value
    }

    private fun formatNumber(value: Double, decimals: Int): String {
        val pattern = buildString {
            append("#,##0")
            if (decimals > 0) {
                append('.')
                repeat(decimals) { append('0') }
            }
        }
        val formatter = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US))
        return formatter.format(value)
    }
}

private object BigDecimalTrim {
    fun trim(value: Double): String {
        val raw = String.format(Locale.US, "%.4f", value)
        return raw.trimEnd('0').trimEnd('.')
    }
}
