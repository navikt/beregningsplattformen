package no.nav.utbetaling.beregning.trekk.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

import no.nav.utbetaling.beregning.tid.Periode
import no.nav.utbetaling.beregning.trekk.IdValueClass

data class Trekkperiode(
    val id: TrekkperiodeId? = null,
    val trekkvedtakId: Long,
    val datoFom: LocalDate,
    val datoTom: LocalDate? = null,
    val sats: BigDecimal,
    val satstype: SatsType,
    var feilReg: Boolean = false,
    val bruker: String,
    val tidspktReg: LocalDateTime,
) {
    val periode by lazy { datoTom?.let { Periode.LukketPeriode(datoFom, it) } ?: Periode.ÅpenPeriode(datoFom) }
}

@JvmInline
value class TrekkperiodeId(
    override val id: Long,
) : IdValueClass
