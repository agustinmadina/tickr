package dev.madina.tickr.ui.format

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Kotlin common has no locale-aware number formatter, so amounts are rendered here rather than
 * per platform. Fixed en-US grouping: this is a demo, not a localized product.
 */
internal fun Double.formatAmount(decimals: Int = 2): String {
    val negative = this < 0
    val factor = 10.0.pow(decimals)
    val scaled = (abs(this) * factor).roundToLong()
    val whole = scaled / factor.toLong()
    val fraction = scaled % factor.toLong()

    val grouped = whole.toString()
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()

    val sign = if (negative) "-" else ""
    return if (decimals == 0) {
        "$sign$grouped"
    } else {
        "$sign$grouped.${fraction.toString().padStart(decimals, '0')}"
    }
}

internal fun Double.formatUsd(decimals: Int = 2): String = "$${formatAmount(decimals)}"

internal fun Double.formatSigned(decimals: Int = 2): String =
    if (this >= 0) "+${formatAmount(decimals)}" else formatAmount(decimals)

internal fun Double.formatPercent(): String = "${formatSigned(2)}%"

/** Crypto quantities need more precision than money, but trailing zeros are noise. */
internal fun Double.formatQuantity(): String = formatAmount(8).trimEnd('0').trimEnd('.')
