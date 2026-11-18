package no.nav.utbetaling.beregning.trekk.dto

import kotlinx.serialization.Serializable

import no.nav.utbetaling.beregning.kotlinx.BigDecimalJson
import no.nav.utbetaling.beregning.kotlinx.LocalDateJson
import no.nav.utbetaling.beregning.kotlinx.LocalDateTimeJson

@Serializable
data class TrekkDTO(
    val trekk: Trekk? = null,
)

@Serializable
data class Trekk(
    var id: Long? = null,
    val trekkvedtakId: Long? = null,
    val kodeTrekktype: String,
    val kreditorIdTss: String? = null,
    val kreditorTrekkId: String? = null,
    val kreditorRef: String? = null,
    val kreditorKid: String? = null,
    val debitorId: String,
    val kodeTrekkAlternativ: String,
    val belopSaldo: BigDecimalJson? = null,
    val belopSaldoTrukket: BigDecimalJson? = null,
    val datoPriFom: LocalDateJson? = null,
    val datoGyldigTom: LocalDateJson? = null,
    val trekkstatus: String? = null,
    val enhet: String? = null,
    val kilde: String? = null,
    val brukerId: String,
    val tidsPktReg: LocalDateTimeJson,
    val fagomraader: Fagomraader,
    val perioder: Perioder,
)

@Serializable
data class Perioder(
    val periode: MutableList<Periode> = mutableListOf(),
)

@Serializable
data class Periode(
    val periodeFomDato: LocalDateJson,
    val periodeTomDato: LocalDateJson? = null,
    val sats: BigDecimalJson? = BigDecimalJson(0.00),
    val lopenrPeriode: Int,
    val feilRegPeriode: String,
    val brukerIdPeriode: String,
    val tidsPktRegPeriode: LocalDateTimeJson,
)

@Serializable
data class Fagomraader(
    val fagomraade: MutableList<Fagomraade> = mutableListOf(),
)

@Serializable
data class Fagomraade(
    val kodeFagomraade: String,
    val lopenrFagomraade: Int,
    val feilRegFagomraade: String,
    val tidsPktRegFagomraade: LocalDateTimeJson,
)
