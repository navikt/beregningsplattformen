package no.nav.utbetaling.beregningsflyt

import kotlinx.serialization.Serializable

@Serializable
data class Kontekst(
    val gjelder: Int,
    val utbetalesTil: String? = null,
    val periode: Periode,
    val beregnet: String,
    val overfoeringsdato: String,
    val ledigFrikortBeloep: Int? = null,
)

@Serializable
data class Periode(
    val fom: String,
    val tom: String?
)