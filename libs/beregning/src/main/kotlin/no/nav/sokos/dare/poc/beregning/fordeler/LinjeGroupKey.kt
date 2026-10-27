package no.nav.sokos.dare.poc.beregning.fordeler

import no.nav.sokos.dare.poc.aktoer.AktoerId

data class LinjeGroupKey(
    val utbetalesTilId: AktoerId?,
    val kid: String?,
)
