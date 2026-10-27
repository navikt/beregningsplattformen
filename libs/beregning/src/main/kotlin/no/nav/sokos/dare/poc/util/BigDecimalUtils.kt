package no.nav.sokos.dare.poc.util

import java.math.BigDecimal
import java.math.RoundingMode

object BigDecimalUtils {
    fun divideWithRemainder(
        dividend: BigDecimal,
        divisor: Long,
    ): Pair<BigDecimal, BigDecimal> {
        val divisorBD = BigDecimal.valueOf(divisor)
        val quotient = dividend.divideToIntegralValue(divisorBD)
        return quotient to dividend.subtract(quotient * divisorBD)
    }

    /**
     * Tapsfri proratering: fordeler [amount] jevnt over [totalParts] deler,
     * og summerer de delene som er angitt i [selectedIndices]. Garanterer at
     * summen av alle ikke-overlappende delintervaller er lik det opprinnelige beløpet.
     *
     * @param selectedIndices hvilke indekser (0-basert) av totalen som skal inkluderes i summen
     */
    fun prorater(
        amount: BigDecimal,
        totalParts: Int,
        selectedIndices: List<Int>,
    ): BigDecimal {
        if (selectedIndices.size == totalParts) return amount
        val (quotient, remainder) = divideWithRemainder(amount, totalParts.toLong())
        val remainderInt = remainder.toInt()
        return selectedIndices.sumOf { index ->
            if (index < remainderInt) quotient + BigDecimal.ONE else quotient
        }
    }

    /**
     * Returns A list of reduced bigDecimals with the amount subtracted, largest number reduced first.
     * [5,2,6] - 3 => [4,2,4]
     * In case an even distribution is impossible, there will be one lowered from the first numbers, then the fraction
     * separating the rest from the higher numbers.
     * [3,3.5,5] - 3 => [2.5,3,3]
     **/
    fun reverseWaterFill(
        list: List<BigDecimal>,
        amount: BigDecimal,
    ): List<BigDecimal> {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return list // early exit

        val sum = list.sumOf { it }
        if (sum < amount) throw IllegalArgumentException("Impossible to satisfy. List sum $sum is smaller than $amount")
        val values = list.toTypedArray()
        // A list of indexes sorted in the order by the value of the indexed number (descending) with the index itself (ascending) as a tie-breaker.
        // This is so that we can iterate over the biggest values first without changing the original order.
        val indexOrder = values.indices.sortedWith(compareByDescending<Int> { values[it] }.thenBy { it })
        var lastValue = values[indexOrder[0]]
        var position = 0
        var sweep = BigDecimal.ZERO

        while (sweep < amount) {
            position++
            val currentValue = if (position < values.size) values[indexOrder[position]] else BigDecimal.ZERO
            val delta = lastValue - currentValue
            lastValue = currentValue
            sweep += delta * BigDecimal(position)
        }

        var (overshotWhole, overshotRemainder) = divideWithRemainder(sweep - amount, position.toLong())

        val adjusted = lastValue + overshotWhole
        val flooredRemainder = overshotRemainder.setScale(0, RoundingMode.DOWN)
        val remainderFraction = overshotRemainder - flooredRemainder
        val remainderThreshold = position - flooredRemainder.toInt() - 1

        (0 until position).forEach { i ->
            if (i < remainderThreshold) {
                values[indexOrder[i]] = adjusted
            } else if (i == remainderThreshold) {
                values[indexOrder[i]] = adjusted + remainderFraction
            } else {
                values[indexOrder[i]] = adjusted + BigDecimal.ONE
            }
        }
        return values.toList()
    }
}
