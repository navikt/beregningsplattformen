package no.nav.sokos.dare.poc.beregning

import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.math.RoundingMode
import java.time.LocalDate

data class JustertTrekk(
    val trekk: BeregningsBeloep,
    override val periodeBeloep: BigDecimal,
    val justerteDagBeloep: Map<LocalDate, BigDecimal>,
) : BeregningsBeloep {
    override fun forDag(dato: LocalDate): BigDecimal = justerteDagBeloep[dato] ?: ZERO

    override val sum: BigDecimal = periodeBeloep + justerteDagBeloep.values.sumOf { it }
}

fun BigDecimal.intRound(): BigDecimal = this.setScale(0, RoundingMode.HALF_UP)

fun BigDecimal.round(): BigDecimal = this.setScale(2, RoundingMode.HALF_UP)
