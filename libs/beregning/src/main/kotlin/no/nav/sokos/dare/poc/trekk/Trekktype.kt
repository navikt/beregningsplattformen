package no.nav.sokos.dare.poc.trekk

import java.time.LocalDateTime

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
    val tidspktEndret: LocalDateTime,
    val trekkberegningstype: Trekkberegningstype,
)
