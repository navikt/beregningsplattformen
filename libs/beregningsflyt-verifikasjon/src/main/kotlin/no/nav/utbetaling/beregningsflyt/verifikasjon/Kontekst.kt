package no.nav.utbetaling.beregningsflyt.verifikasjon

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Kontekst(
    val gjelder: Int,
    val utbetalesTil: String? = null,
    val periode: Periode,
    val beregnet: String,
    val overfoeringsdato: String,
    val ledigFrikortBeloep: Int? = null,
    val resten: JsonObject = JsonObject(emptyMap()),
)
