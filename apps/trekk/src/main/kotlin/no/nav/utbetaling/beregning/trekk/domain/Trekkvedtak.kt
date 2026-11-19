package no.nav.utbetaling.beregning.trekk.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

import kotlinx.serialization.Serializable

import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.trekk.utils.IdValueClass

data class Trekkvedtak(
    val id: TrekkvedtakId? = null,
    val eksternTrekkId: Long? = null,
    val trekktypeKode: String,
    val kreditorIdTss: String? = null,
    val kreditorTrekkId: String? = null,
    val kreditorRef: String? = null,
    val belopSaldo: BigDecimal? = null,
    val kreditorKid: String? = null,
    val gyldigTom: LocalDate? = null,
    val ansvarligEnhet: String,
    val debitorId: Long,
    val datoPriFom: LocalDate? = null,
    val kilde: String? = null,
    val trekkstatus: Trekkstatus,
    var trekkperioder: List<Trekkperiode> = emptyList(),
    var fagomraader: List<TrekkFagomraade> = emptyList(),
    var trekktype: Trekktype? = null,
    val bruker: String,
    val tidspktReg: LocalDateTime,
    val mottatt: LocalDateTime? = null,
) {
    fun isTrekkRelevantForDatoIntervall(overlappendePeriode: Periode): Boolean = trekkperioder.any { it.periode.overlapperMed(overlappendePeriode) }

    fun erSaldoTrekk(): Boolean = belopSaldo != null
}

@Serializable
@JvmInline
value class TrekkvedtakId(
    override val id: Long,
) : IdValueClass
