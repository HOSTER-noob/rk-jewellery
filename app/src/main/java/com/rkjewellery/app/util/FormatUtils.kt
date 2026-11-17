package com.rkjewellery.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {

    /** 123450 paise -> "₹1,234.50" */
    fun money(paise: Long, withSymbol: Boolean = true): String {
        val negative = paise < 0
        val abs = kotlin.math.abs(paise)
        val rupees = abs / 100
        val p = abs % 100
        val grouped = formatIndianNumber(rupees)
        val text = if (p > 0) "$grouped.${"%02d".format(p)}" else grouped
        return (if (negative) "-" else "") + (if (withSymbol) "₹" else "") + text
    }

    /** Indian digit grouping: 1234567 -> 12,34,567 */
    fun formatIndianNumber(n: Long): String {
        if (n < 1000) return n.toString()
        val s = n.toString()
        val last3 = s.substring(s.length - 3)
        var rest = s.substring(0, s.length - 3)
        val sb = StringBuilder()
        while (rest.length > 2) {
            sb.insert(0, "," + rest.substring(rest.length - 2))
            rest = rest.substring(0, rest.length - 2)
        }
        sb.insert(0, rest)
        return sb.toString() + "," + last3
    }

    /** Milligrams -> human weight. 12345 mg -> "12.345 g"; 900 mg -> "900 mg" */
    fun weight(mg: Long, compact: Boolean = true): String {
        if (compact && mg < 1000) return "$mg mg"
        val grams = mg / 1000.0
        return String.format(Locale.US, "%.3f g", grams)
    }

    fun grams(mg: Long): Double = mg / 1000.0

    fun gramsText(mg: Long): String = String.format(Locale.US, "%.3f", mg / 1000.0)

    /** 12.0 -> "12", 12.5 -> "12.5" */
    fun trimZero(d: Double): String =
        if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()

    fun trimZeroStatic(d: Double): String = trimZero(d)

    fun date(millis: Long): String =
        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(millis))

    fun dateShort(millis: Long): String =
        SimpleDateFormat("dd MMM", Locale.ENGLISH).format(Date(millis))

    fun dateTime(millis: Long): String =
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date(millis))

    fun dayName(millis: Long): String =
        SimpleDateFormat("EEEE", Locale.ENGLISH).format(Date(millis))

    fun greeting(): String {
        val h = SimpleDateFormat("HH", Locale.US).format(Date()).toIntOrNull() ?: 12
        return when {
            h < 12 -> "Good Morning"
            h < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }
}

object DocNumberGenerator {
    fun next(prefix: String, existingCount: Int): String {
        val year = SimpleDateFormat("yyyy", Locale.US).format(Date())
        val seq = existingCount + 1
        return "$prefix-$year-${"%04d".format(seq)}"
    }
}
