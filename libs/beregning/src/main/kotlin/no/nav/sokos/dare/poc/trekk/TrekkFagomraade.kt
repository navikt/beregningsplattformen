package no.nav.sokos.dare.poc.trekk

import kotlinx.serialization.Serializable

import no.nav.sokos.dare.poc.LocalDateTimeJson
import no.nav.sokos.dare.poc.structural.IdValueClass

@Serializable
data class TrekkFagomraade(
    val id: TrekkFagomraadeId? = null,
    val trekkvedtakId: Long,
    val fagomraadeKode: FagomraadeKode,
    val aktiv: Boolean, // true hvis trekkvedtaket er benyttet i fagomraadet
    val tidspktReg: LocalDateTimeJson,
)

@Serializable
@JvmInline
value class TrekkFagomraadeId(
    override val id: Long,
) : IdValueClass
