package no.nav.sokos.dare.poc.beregning.fordeler

import java.math.BigDecimal

import no.nav.sokos.dare.poc.trekk.TrekkvedtakId

data class SaldoBoetter(
    val innkommendeSaldoer: Map<TrekkvedtakId, BigDecimal>,
) {
    val saldoer: MutableMap<TrekkvedtakId, BigDecimal> = innkommendeSaldoer.toMutableMap()

    fun finnRestSaldo(id: TrekkvedtakId): BigDecimal? = saldoer[id]

    fun brukSaldo(
        id: TrekkvedtakId,
        beloep: BigDecimal,
    ) {
        saldoer[id]?.minus(beloep)?.let { saldoer[id] = it }
    }
}
