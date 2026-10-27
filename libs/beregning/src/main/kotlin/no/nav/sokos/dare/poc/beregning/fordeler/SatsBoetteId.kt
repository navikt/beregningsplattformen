package no.nav.sokos.dare.poc.beregning.fordeler

import no.nav.sokos.dare.poc.trekk.TrekkperiodeId

data class SatsBoetteId(
    val id: TrekkperiodeId,
    val boette: String,
)
