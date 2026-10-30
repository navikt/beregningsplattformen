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
    override val linjeGroupKey: LinjeGroupKey? = null,
    override val delperioder: List<OppdragDelperiode>,
    override val ekstraInfo: MutableSet<EkstraInfo> = mutableSetOf(),
    override val beregningsStatusflagg: MutableSet<BeregningsStatusflagg> = mutableSetOf(),
    override val avventStatus: BeregningsStatus? = null,
    override val avventOverfoeres: LocalDate? = null,
) : Segmentgrunnlag {
    override val scratchpad: MutableMap<OppdragId, BeregningsScratchpad> = mutableMapOf()
}
