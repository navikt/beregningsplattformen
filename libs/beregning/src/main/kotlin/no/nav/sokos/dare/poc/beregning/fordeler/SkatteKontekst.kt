package no.nav.sokos.dare.poc.beregning.fordeler

import no.nav.sokos.dare.poc.skattekort.domain.ForskuddstrekkKode
import no.nav.sokos.dare.poc.skattekort.domain.Skattekort
import no.nav.sokos.dare.poc.trekk.Faggruppe

data class SkatteKontekst(
    val skattekort: Skattekort?,
    val allokeringer: Map<ForskuddstrekkKode, Faggruppe>,
)
