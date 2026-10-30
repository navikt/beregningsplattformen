package no.nav.utbetaling.beregningsflyt.kontekst

import java.time.LocalDate

import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatus
import no.nav.sokos.dare.poc.beregning.dataklasser.BeregningsStatusflagg
import no.nav.sokos.dare.poc.beregning.fordeler.BeregningsScratchpad
import no.nav.sokos.dare.poc.beregning.fordeler.LinjeGroupKey
import no.nav.sokos.dare.poc.beregning.fordeler.Segmentgrunnlag
import no.nav.sokos.dare.poc.oppdrag.OppdragId

class BeregningsSegment(
    override val linjeGroupKey: LinjeGroupKey? = null,
    override val delperioder: List<OppdragDelperiode>,
    ekstraInfo: MutableSet<EkstraInfo> = mutableSetOf(),
    beregningsStatusflagg: MutableSet<BeregningsStatusflagg> = mutableSetOf(),
    override val avventStatus: BeregningsStatus? = null,
    override val avventOverfoeres: LocalDate? = null,
) : Segmentgrunnlag {
    override val ekstraInfo: MutableSet<EkstraInfo> = ekstraInfo.toMutableSet()
    override val beregningsStatusflagg: MutableSet<BeregningsStatusflagg> = beregningsStatusflagg.toMutableSet()
    override val scratchpad: MutableMap<OppdragId, BeregningsScratchpad> = mutableMapOf()

    init {
        require(delperioder.map { it.oppdragsnokkel }.distinct().size <= 1) {
            "Expected one logical assignment key per segment: ${delperioder.map { it.oppdragsnokkel to it.periode }}"
        }
        require(delperioder.map { it.oppdragId }.distinct().size <= 1) {
            "Conflicting numeric identities for ${delperioder.map { it.oppdragsnokkel to it.periode }}"
        }
    }
}
