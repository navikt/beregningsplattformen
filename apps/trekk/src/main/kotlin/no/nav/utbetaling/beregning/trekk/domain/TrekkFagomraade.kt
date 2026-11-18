package no.nav.utbetaling.beregning.trekk.domain

import java.time.LocalDateTime

import no.nav.utbetaling.beregning.trekk.IdValueClass

data class TrekkFagomraade(
    val id: TrekkFagomraadeId? = null,
    val trekkvedtakId: Long,
    val fagomraadeKode: String,
    val aktiv: Boolean, // true hvis trekkvedtaket er benyttet i fagomraadet
    val tidspktReg: LocalDateTime,
)

@JvmInline
value class TrekkFagomraadeId(
    override val id: Long,
) : IdValueClass
