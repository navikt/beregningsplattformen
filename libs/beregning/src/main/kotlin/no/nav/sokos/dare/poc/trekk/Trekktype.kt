package no.nav.sokos.dare.poc.trekk

import kotlinx.serialization.Serializable

import no.nav.sokos.dare.poc.LocalDateTimeJson

@Serializable
data class Trekktype(
    val kodeTrekktype: String,
    val beskrivelse: String,
    val prioritet: String,
    val kodeKlasseTrekk: String,
    val kodeTrekkategori: String,
    val reduserSkattegr: Boolean,
    val kodeOppgjoerstype: String,
    val kodeKlasseOppgj: String,
    val belopsgrense: Long,
    val bruker: String,
    val tidspktEndret: LocalDateTimeJson,
    val trekkberegningstype: Trekkberegningstype,
)
