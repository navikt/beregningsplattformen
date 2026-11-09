package no.nav.sokos.dare.poc.trekk

import kotlinx.serialization.Serializable

import no.nav.sokos.dare.poc.BigDecimalJson
import no.nav.sokos.dare.poc.LocalDateJson
import no.nav.sokos.dare.poc.LocalDateTimeJson
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.structural.IdValueClass

@Serializable
data class Trekkvedtak(
    val id: TrekkvedtakId? = null,
    val eksternTrekkId: Long? = null,
    val trekktypeKode: String,
    // TODO: Kreditor sin tss-id bør heller modelleres som en TSS-id på aktoer, med en referanse til kreditor lagret for trekkvedtak
    // Det vil også gi referensiell integritet i gui når vi ska vise trekkvedtakene til en kreditor.
    val kreditorIdTss: String? = null,
    val kreditorTrekkId: String? = null,
    val kreditorRef: String? = null,
    @Deprecated("bruk erSaldoTrekk() i stedet")
    val trekkalt: TrekkAlt? = null,
    val belopSaldo: BigDecimalJson? = null,
    val kreditorKid: String? = null,
    val gyldigTom: LocalDateJson? = null,
    val ansvarligEnhet: String,
    val debitorId: Long,
    val datoPriFom: LocalDateJson? = null,
    val kilde: String? = null,
    val trekkstatus: TrekkStatus,
    var trekkperioder: List<Trekkperiode> = emptyList(),
    var fagomraader: List<TrekkFagomraade> = emptyList(),
    var trekktype: Trekktype? = null,
    val bruker: String,
    val tidspktReg: LocalDateTimeJson,
    val mottatt: LocalDateTimeJson? = null,
) {
    fun isTrekkGyldigIFagomraade(fagomraade: FagomraadeKode): Boolean = fagomraader.any { it.fagomraadeKode == fagomraade }

    fun isTrekkRelevantForDatoIntervall(datoIntervall: DatoIntervall): Boolean = trekkperioder.any { it.periode.overlaps(datoIntervall) }

    /** Saldotrekk har belopSaldo satt */
    fun erSaldoTrekk(): Boolean = belopSaldo != null
}

@Serializable
@JvmInline
value class TrekkvedtakId(
    override val id: Long,
) : IdValueClass
