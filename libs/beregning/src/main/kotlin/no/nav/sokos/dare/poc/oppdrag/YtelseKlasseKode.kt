package no.nav.sokos.dare.poc.oppdrag

import kotlinx.serialization.Serializable

import no.nav.sokos.dare.poc.LocalDateTimeJson
import no.nav.sokos.dare.poc.trekk.FagomraadeKode

@JvmInline
@Serializable
value class YtelseKlasseKodeRef(
    val kode: String,
)

@Serializable
data class YtelseKlasseKode(
    val kode: YtelseKlasseKodeRef,
    val beskrivelse: String,
    val grunnlagSkatt: Boolean,
    val grunnlagTrekk: Boolean,
    val fagomraader: List<FagomraadeKode>,
    val tidspktOpprettet: LocalDateTimeJson? = null,
) {
    // Kode alene identifiserer klassekoden. Andre felt er beskrivende og skal ikke inngå i likhetssjekk.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is YtelseKlasseKode) return false
        return kode == other.kode
    }

    override fun hashCode(): Int = kode.hashCode()
}
