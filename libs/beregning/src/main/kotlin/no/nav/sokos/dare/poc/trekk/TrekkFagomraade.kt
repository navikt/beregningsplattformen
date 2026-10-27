package no.nav.sokos.dare.poc.trekk

import java.time.LocalDateTime

import no.nav.sokos.dare.poc.structural.IdValueClass

data class TrekkFagomraade(
    val id: TrekkFagomraadeId? = null,
    val trekkvedtakId: Long,
    val fagomraadeKode: FagomraadeKode,
    val aktiv: Boolean, // true hvis trekkvedtaket er benyttet i fagomraadet
    val tidspktReg: LocalDateTime,
)

@JvmInline
value class TrekkFagomraadeId(
    override val id: Long,
) : IdValueClass
