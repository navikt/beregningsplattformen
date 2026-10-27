package no.nav.sokos.dare.poc.beregning.fordeler

import java.time.LocalDate

import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatusflagg
import no.nav.sokos.dare.poc.oppdrag.OppdragId

/**
 * Et BeregningsSegment vil produsere én beregning per oppdrag. Kan inneholde flere oppdrag, separert over flere forskjellige delperioder.
 */
data class BeregningsSegment(
    val linjeGroupKey: LinjeGroupKey? = null,
    val delperioder: List<OppdragDelperiode>,
    val ekstraInfo: MutableSet<EkstraInfo> = mutableSetOf(),
    val beregningsStatusflagg: MutableSet<BeregningsStatusflagg> = mutableSetOf(),
    val avventStatus: BeregningsStatus? = null,
    val avventOverfoeres: LocalDate? = null,
) {
    val scratchpad: MutableMap<OppdragId, BeregningsScratchpad> = mutableMapOf()
}
