package no.nav.sokos.dare.poc.beregning

import java.math.BigDecimal
import java.time.LocalDate
import java.util.TreeMap

// Kapasiteten på en spesifik linje i en utført Beregning.
data class Kapasitet(
    val id: KapasitetId? = null,
    val dagbeloep: TreeMap<LocalDate, BigDecimal>,
    val periodeBeloep: BigDecimal,
) {
    override fun toString() =
        buildString {
            dagbeloep.forEach { d -> append("${d.key}: ${d.value.toPlainString()}, ") }
            append(" + $periodeBeloep = $sum")
        }

    val sum: BigDecimal = dagbeloep.values.sumOf { it } + periodeBeloep
}

@JvmInline
value class KapasitetId(
    val id: Long,
)
