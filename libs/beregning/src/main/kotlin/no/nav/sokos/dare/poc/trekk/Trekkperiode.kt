package no.nav.sokos.dare.poc.trekk

import kotlinx.serialization.Serializable

import no.nav.sokos.dare.poc.BigDecimalJson
import no.nav.sokos.dare.poc.LocalDateJson
import no.nav.sokos.dare.poc.LocalDateTimeJson
import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.structural.IdValueClass

@Serializable
data class Trekkperiode(
    val id: TrekkperiodeId? = null,
    val trekkvedtakId: Long,
    val datoFom: LocalDateJson,
    val datoTom: LocalDateJson? = null,
    val sats: BigDecimalJson,
    val satstype: SatsType,
    var feilReg: Boolean = false,
    val bruker: String,
    val tidspktReg: LocalDateTimeJson,
) {
    val periode by lazy { DatoIntervall(datoFom, datoTom) }
}

@Serializable
@JvmInline
value class TrekkperiodeId(
    override val id: Long,
) : IdValueClass
