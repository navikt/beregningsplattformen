package no.nav.utbetaling.beregningsflyt.verifikasjon

import kotlinx.serialization.Serializable

@Serializable
data class SkatteKontekst(
    val skattekort: Skattekort? = null,
    val allokeringer: Map<String, String> = emptyMap(),
)

@Serializable
data class Skattekort(
    val aktoerId: Int,
    val inntektsaar: Int,
    val kilde: String,
    val identifikator: Int,
    val utstedtDato: String,
    val tilleggsopplysning: List<String> = emptyList(),
    val forskuddsTrekk: List<ForskuddsTrekk> = emptyList(),
)

@Serializable
data class ForskuddsTrekk(
    val typeForskuddsTrekk: String,
    val trekkKode: String,
    val frikortBelop: Int? = null,
    val prosentsats: Double? = null,
    val trekktabell: String? = null,
    val tidspktReg: String? = null,
)
