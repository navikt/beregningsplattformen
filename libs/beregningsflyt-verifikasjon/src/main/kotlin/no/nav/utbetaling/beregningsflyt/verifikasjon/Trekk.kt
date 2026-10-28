package no.nav.utbetaling.beregningsflyt.verifikasjon

import kotlinx.serialization.Serializable

@Serializable
data class Trekkvedtak(
    val id: Int,
    val debitorId: Int,
    val bruker: String,
    val trekkalt: String,
    val trekktypeKode: String,
    val trekkstatus: String,
    val ansvarligEnhet: String,
    val tidspktReg: String,
    val belopSaldo: Int? = null,
    val belopSaldoTrukket: Int? = null,
    val datoPriFom: String? = null,
    val eksternTrekkId: String? = null,
    val gyldigTom: String? = null,
    val kilde: String? = null,
    val kreditorIdTss: String? = null,
    val kreditorKid: String? = null,
    val kreditorRef: String? = null,
    val kreditorTrekkId: String? = null,
    val fagomraader: List<TrekkFagomraade> = emptyList(),
    val trekkperioder: List<Trekkperiode> = emptyList(),
    val trekktype: Trekktype? = null,
)

@Serializable
data class TrekkFagomraade(
    val id: Int? = null,
    val trekkvedtakId: Int,
    val fagomraadeKode: String,
    val aktiv: Boolean,
    val tidspktReg: String,
)

@Serializable
data class Trekkperiode(
    val id: Int,
    val trekkvedtakId: Int,
    val bruker: String,
    val datoFom: String,
    val datoTom: String? = null,
    val periode: Periode? = null,
    val sats: Int,
    val satstype: String,
    val feilReg: Boolean,
    val tidspktReg: String,
)

@Serializable
data class Trekktype(
    val kodeTrekktype: String,
    val kodeTrekkategori: String,
    val kodeOppgjoerstype: String,
    val kodeKlasseTrekk: String,
    val kodeKlasseOppgj: String,
    val trekkberegningstype: String,
    val beskrivelse: String,
    val prioritet: String,
    val belopsgrense: Int,
    val reduserSkattegr: Boolean,
    val bruker: String,
    val tidspktEndret: String,
)
