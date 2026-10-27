package no.nav.sokos.dare.poc.beregning.fordeler

import no.nav.sokos.dare.poc.beregning.DatoIntervall
import no.nav.sokos.dare.poc.beregning.Delberegningslinje
import no.nav.sokos.dare.poc.beregning.EkstraInfo
import no.nav.sokos.dare.poc.beregning.beregningsmotor.KapasitetStyring

data class BeregningsScratchpad(
    val beregninger: MutableMap<DatoIntervall, MutableList<Delberegningslinje>> = mutableMapOf(),
    val ekstraInfo: MutableSet<EkstraInfo> = mutableSetOf(),
    val erEtterbetaling: MutableMap<DatoIntervall, Boolean> = mutableMapOf(),
) {
    val kapasitetStyring: KapasitetStyring = KapasitetStyring()

    val restKapasitet get() = kapasitetStyring.current()
}
