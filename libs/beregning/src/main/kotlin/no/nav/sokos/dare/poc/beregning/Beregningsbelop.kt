package no.nav.sokos.dare.poc.beregning

import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.time.LocalDate

import no.nav.sokos.dare.poc.util.DagBeloepMap

// TODO: Finn et bedre navn.
//  Et Beregningsbeløp er en kilde for tall som brukes til beregning. Er er enten et beløp knyttet til en enkelt dag eller
//  et enkelt beløp som dekker en tidsperiode.

interface BeregningsBeloep {
    fun forDag(dato: LocalDate): BigDecimal

    val periodeBeloep: BigDecimal
    val sum: BigDecimal
}

data class Enkeltbeloep(
    val sats: BigDecimal,
) : BeregningsBeloep {
    override fun forDag(dato: LocalDate) = ZERO

    override val periodeBeloep = sats
    override val sum: BigDecimal = sats
}

data class BeloepPerDag(
    val dagBeloep: Map<LocalDate, BigDecimal>,
) : BeregningsBeloep {
    override fun forDag(dato: LocalDate) = dagBeloep[dato] ?: ZERO

    override val periodeBeloep: BigDecimal = ZERO
    override val sum: BigDecimal = dagBeloep.values.sumOf { it }
}

// Lager DagBeloepMap av et BeregningsBeløp. PeriodeBeløp returneres som egen komponent.
fun BeregningsBeloep.toDagBeloepMap(periode: DatoIntervall): Pair<BigDecimal, DagBeloepMap> = periodeBeloep to DagBeloepMap(periode.dager.associateWith(::forDag))
