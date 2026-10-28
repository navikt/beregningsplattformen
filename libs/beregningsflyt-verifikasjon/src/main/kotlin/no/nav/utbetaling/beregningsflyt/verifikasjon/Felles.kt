package no.nav.utbetaling.beregningsflyt.verifikasjon

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Periode(
    val fom: String,
    val tom: String? = null,
)

@Serializable
data class Verdi(
    val verdi: String,
    val skjult: Boolean = false,
)

@Serializable
sealed interface EkstraInfo

@Serializable
@SerialName("TekstEkstraInfo")
data class TekstEkstraInfo(
    val grunn: String,
    val template: String? = null,
    val verdier: Map<String, Verdi> = emptyMap(),
) : EkstraInfo
