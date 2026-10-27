package no.nav.sokos.dare.poc.trekk

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.structural.IdValueClass

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
    val periode by lazy { DatoIntervall(datoFom, datoTom) }
}

@JvmInline
value class TrekkperiodeId(
    override val id: Long,
) : IdValueClass
