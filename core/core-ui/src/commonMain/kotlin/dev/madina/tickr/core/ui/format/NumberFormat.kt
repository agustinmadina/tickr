package dev.madina.tickr.core.ui.format

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Kotlin common has no locale-aware number formatter, so amounts are rendered here rather than once
 * per platform. Fixed en-US grouping: this is a demo, not a localized product, and three
 * implementations that disagree would be worse than one that is explicit about its scope.
 */
fun Double.formatAmount(decimals: Int = 2): String {
    val negative = this < 0
    val factor = 10.0.pow(decimals)
    val scaled = (abs(this) * factor).roundToLong()
    val whole = scaled / factor.toLong()
    val fraction = scaled % factor.toLong()

    val grouped =
        whole
            .toString()
            .reversed()
            .chunked(GroupSize)
            .joinToString(",")
            .reversed()

    val sign = if (negative) "-" else ""
    return if (decimals == 0) {
        "$sign$grouped"
    } else {
        "$sign$grouped.${fraction.toString().padStart(decimals, '0')}"
    }
}

fun Double.formatUsd(decimals: Int = 2): String = "$${formatAmount(decimals)}"

fun Double.formatSignedUsd(decimals: Int = 2): String =
    if (this >= 0) "+$${formatAmount(decimals)}" else "-$${abs(this).formatAmount(decimals)}"

fun Double.formatPercent(): String =
    if (this >= 0) "+${formatAmount()}%" else "${formatAmount()}%"

/** Crypto quantities need more precision than money, but trailing zeros are noise. */
fun Double.formatQuantity(): String =
    formatAmount(QuantityDecimals).trimEnd('0').trimEnd('.')

/**
 * Cheap assets need more decimals to show any movement at all: at two decimals a coin trading near
 * a dollar looks frozen while it is in fact ticking.
 */
fun Double.formatPrice(): String =
    when {
        this >= LargePriceThreshold -> formatUsd()
        this >= SmallPriceThreshold -> formatUsd(decimals = 3)
        else -> formatUsd(decimals = 5)
    }

private const val GroupSize = 3
private const val QuantityDecimals = 8
private const val LargePriceThreshold = 100.0
private const val SmallPriceThreshold = 1.0
