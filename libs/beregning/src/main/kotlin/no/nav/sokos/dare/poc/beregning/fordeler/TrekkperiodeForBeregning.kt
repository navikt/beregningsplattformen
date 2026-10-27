package no.nav.sokos.dare.poc.beregning.fordeler

import java.math.BigDecimal

import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.trekk.SatsType
import no.nav.sokos.dare.poc.trekk.Trekkperiode
import no.nav.sokos.dare.poc.trekk.TrekkperiodeId

data class TrekkperiodeForBeregning(
    val id: TrekkperiodeId,
    val trekkvedtakId: Long,
    val periode: DatoIntervall,
    val sats: BigDecimal,
    val satstype: SatsType,
    val boetter: List<String>,
) {
    fun satsBoetteIds(): List<SatsBoetteId> = boetter.map { boette -> SatsBoetteId(id, boette) }
}

fun makeTrekkPeriodeForBeregning(
    tp: Trekkperiode,
    boetter: List<String> = emptyList(),
): TrekkperiodeForBeregning =
    TrekkperiodeForBeregning(
        id = tp.id!!,
        trekkvedtakId = tp.trekkvedtakId,
        periode = tp.periode,
        sats = tp.sats,
        satstype = tp.satstype,
        boetter = boetter,
    )
