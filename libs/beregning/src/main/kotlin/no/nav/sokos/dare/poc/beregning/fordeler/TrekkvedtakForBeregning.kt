package no.nav.sokos.dare.poc.beregning.fordeler

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

import no.nav.sokos.dare.poc.trekk.Trekktype
import no.nav.sokos.dare.poc.trekk.Trekkvedtak
import no.nav.sokos.dare.poc.trekk.TrekkvedtakId

@ConsistentCopyVisibility
data class TrekkvedtakForBeregning private constructor(
    val id: TrekkvedtakId,
    val trekktypeKode: String,
    val belopSaldo: BigDecimal? = null,
    val datoPriFom: LocalDate? = null,
    var trekkperiode: TrekkperiodeForBeregning,
    val trekktype: Trekktype,
    val bruker: String,
    val tidspktReg: LocalDateTime,
) {
    constructor(trekkvedtak: Trekkvedtak, trekkperiode: TrekkperiodeForBeregning) : this(
        id = trekkvedtak.id!!,
        trekktypeKode = trekkvedtak.trekktypeKode,
        belopSaldo = trekkvedtak.belopSaldo,
        datoPriFom = trekkvedtak.datoPriFom,
        trekkperiode = trekkperiode,
        trekktype = requireNotNull(trekkvedtak.trekktype) { "Mangler trekktype for trekkvedtak ${trekkvedtak.id}" },
        bruker = trekkvedtak.bruker,
        tidspktReg = trekkvedtak.tidspktReg,
    )

    /** Saldotrekk har belopSaldo satt */
    fun erSaldoTrekk(): Boolean = belopSaldo != null
}
